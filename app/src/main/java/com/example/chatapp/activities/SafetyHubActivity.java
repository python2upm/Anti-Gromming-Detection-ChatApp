package com.example.chatapp.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.example.chatapp.databinding.ActivitySafetyHubBinding;
import com.example.chatapp.utilities.Constants;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class SafetyHubActivity extends BaseActivity {

    private ActivitySafetyHubBinding binding;
    private final Executor executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySafetyHubBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setListeners();
    }

    private void setListeners() {
        binding.imageBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        binding.imageSend.setOnClickListener(v -> {
            if (!binding.inputPrompt.getText().toString().trim().isEmpty()) {
                callGeminiAPI(binding.inputPrompt.getText().toString());
            }
        });
    }

    private void callGeminiAPI(String prompt) {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.imageSend.setEnabled(false);
        executor.execute(() -> {
            try {
                // Gemini API Endpoint (Generative Language API)
                URL url = new URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent?key=" + Constants.GEMINI_API_KEY);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                // Construct Request JSON
                JSONObject contents = new JSONObject();
                JSONArray parts = new JSONArray();
                JSONObject textPart = new JSONObject();
                textPart.put("text", "You are a safety assistant for a chat app. Help the user with anti-grooming advice, policy analysis, or report writing. User: " + prompt);
                parts.put(textPart);
                contents.put("parts", parts);
                
                JSONArray contentsArray = new JSONArray();
                contentsArray.put(contents);
                
                JSONObject requestBody = new JSONObject();
                requestBody.put("contents", contentsArray);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = requestBody.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                    
                    JSONObject responseJson = new JSONObject(response.toString());
                    String resultText = responseJson.getJSONArray("candidates")
                            .getJSONObject(0)
                            .getJSONObject("content")
                            .getJSONArray("parts")
                            .getJSONObject(0)
                            .getString("text");

                    runOnUiThread(() -> {
                        binding.textResponse.setText(resultText);
                        binding.progressBar.setVisibility(View.GONE);
                        binding.imageSend.setEnabled(true);
                        binding.inputPrompt.setText("");
                    });
                } else {
                    throw new Exception("API call failed with code: " + responseCode);
                }
            } catch (Exception e) {
                runOnUiThread(() -> {
                    showToast("Error: " + e.getMessage());
                    binding.progressBar.setVisibility(View.GONE);
                    binding.imageSend.setEnabled(true);
                });
            }
        });
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }
}
