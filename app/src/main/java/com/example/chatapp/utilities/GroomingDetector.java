package com.example.chatapp.utilities;

import java.util.HashMap;
import java.util.Map;

public class GroomingDetector {

    public enum RiskLevel {
        SAFE, MEDIUM, HIGH
    }

    public static class DetectionResult {
        public RiskLevel riskLevel;
        public int score;
        public String reason;

        public DetectionResult(RiskLevel riskLevel, int score, String reason) {
            this.riskLevel = riskLevel;
            this.score = score;
            this.reason = reason;
        }
    }

    private static final Map<String, Integer> GROOMING_KEYWORDS = new HashMap<>();

    static {
        // High risk keywords/phrases
        GROOMING_KEYWORDS.put("don't tell", 30);
        GROOMING_KEYWORDS.put("our secret", 30);
        GROOMING_KEYWORDS.put("private photo", 40);
        GROOMING_KEYWORDS.put("nude", 50);
        GROOMING_KEYWORDS.put("sexy", 25);
        GROOMING_KEYWORDS.put("meet alone", 40);
        GROOMING_KEYWORDS.put("parents won't know", 35);
        
        // Medium risk keywords/phrases
        GROOMING_KEYWORDS.put("webcam", 20);
        GROOMING_KEYWORDS.put("lonely", 15);
        GROOMING_KEYWORDS.put("gift", 10);
        GROOMING_KEYWORDS.put("money", 10);
        GROOMING_KEYWORDS.put("older", 5);
        GROOMING_KEYWORDS.put("sweetie", 10);
        GROOMING_KEYWORDS.put("darling", 10);
    }

    public static DetectionResult analyze(String message) {
        if (message == null || message.trim().isEmpty()) {
            return new DetectionResult(RiskLevel.SAFE, 0, "Empty message");
        }

        String lowerMessage = message.toLowerCase();
        int score = 0;
        StringBuilder detectedPatterns = new StringBuilder();

        for (Map.Entry<String, Integer> entry : GROOMING_KEYWORDS.entrySet()) {
            if (lowerMessage.contains(entry.getKey())) {
                score += entry.getValue();
                if (detectedPatterns.length() > 0) detectedPatterns.append(", ");
                detectedPatterns.append(entry.getKey());
            }
        }

        // Decision Tree Logic
        RiskLevel level;
        String reason;
        if (score >= 50) {
            level = RiskLevel.HIGH;
            reason = "High risk patterns detected: " + detectedPatterns.toString();
        } else if (score >= 20) {
            level = RiskLevel.MEDIUM;
            reason = "Suspicious patterns detected: " + detectedPatterns.toString();
        } else {
            level = RiskLevel.SAFE;
            reason = "No significant risk patterns detected";
        }

        return new DetectionResult(level, score, reason);
    }
}
