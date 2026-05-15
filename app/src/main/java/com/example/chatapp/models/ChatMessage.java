package com.example.chatapp.models;

import java.util.Date;

public class ChatMessage {
    public String senderId, receiverId, message, dateTime;
    public Date dateObject;
    public String conversionId, conversionName, conversionImage;
    public boolean isFlagged;
    public String flaggedReason;
}
