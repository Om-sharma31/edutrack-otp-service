package com.edutrack.otp.model;

import lombok.Data;

// ── Request: send OTP to an email ────────────────
@Data
class SendOtpRequest {
    private String email;
    private String userName;   // shown in the email greeting
}

// ── Request: verify OTP entered by user ──────────
@Data
class VerifyOtpRequest {
    private String email;
    private String otp;
}

// ── Generic API response ──────────────────────────
@Data
class ApiResponse {
    private boolean success;
    private String message;

    public ApiResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
}
