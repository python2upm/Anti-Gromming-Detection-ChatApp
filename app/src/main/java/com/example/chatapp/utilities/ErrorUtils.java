package com.example.chatapp.utilities;

public class ErrorUtils {

    public static String getCleanErrorMessage(int statusCode) {
        switch (statusCode) {
            case 400:
                return "Error 400: Bad Request";
            case 401:
                return "Error 401: Unauthorized access";
            case 403:
                return "Error 403: Action forbidden";
            case 404:
                return "Error 404: Service not found";
            case 408:
                return "Error 408: Connection timed out";
            case 429:
                return "Error 429: Too many requests. Please slow down.";
            case 500:
                return "Error 500: Internal server error";
            case 503:
                return "Error 503: Service temporarily unavailable";
            default:
                if (statusCode >= 400 && statusCode < 500) {
                    return "Error " + statusCode + ": Request failed";
                } else if (statusCode >= 500 && statusCode < 600) {
                    return "Error " + statusCode + ": Server is busy";
                } else {
                    return "Error " + statusCode + ": Unexpected error";
                }
        }
    }

    public static String getStatusName(int statusCode) {
        switch (statusCode) {
            case 400: return "Bad Request";
            case 401: return "Unauthorized";
            case 403: return "Forbidden";
            case 404: return "Not Found";
            case 405: return "Method Not Allowed";
            case 408: return "Request Timeout";
            case 429: return "Too Many Requests";
            case 500: return "Internal Server Error";
            case 502: return "Bad Gateway";
            case 503: return "Service Unavailable";
            case 504: return "Gateway Timeout";
            default: return "Unknown Error";
        }
    }
}
