package com.example.chatapp.models;

public class SafetyHubMessage {
    public String message;
    public boolean isUser;
    public String dateTime;

    public SafetyHubMessage(String message, boolean isUser, String dateTime) {
        this.message = message;
        this.isUser = isUser;
        this.dateTime = dateTime;
    }
}
