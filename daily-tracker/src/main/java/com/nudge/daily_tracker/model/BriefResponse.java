package com.nudge.daily_tracker.model;

public class BriefResponse {
    private String message;

    public BriefResponse(String message) {
        this.message = message;
    }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}