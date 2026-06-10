package com.example.chatapp.utilities;

import androidx.annotation.NonNull;

public class HttpException extends Exception {
    private final int statusCode;
    private final String statusName;

    public HttpException(int statusCode, String statusName, String message) {
        super(message);
        this.statusCode = statusCode;
        this.statusName = statusName;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getStatusName() {
        return statusName;
    }

    @NonNull
    @Override
    public String toString() {
        return "Error " + statusCode + ": " + statusName;
    }
}
