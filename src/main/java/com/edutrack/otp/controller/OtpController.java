package com.edutrack.otp.controller;

import com.edutrack.otp.service.OtpService;
import jakarta.mail.MessagingException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/otp")
public class OtpController {

    private final OtpService otpService;

    public OtpController(OtpService otpService) {
        this.otpService = otpService;
    }

    // ── POST /api/otp/send ────────────────────────────────────────────────────
    // Body: { "email": "user@example.com", "userName": "Aarav Sharma" }
    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> sendOtp(@RequestBody Map<String, String> body) {
        String email    = body.getOrDefault("email", "").trim();
        String userName = body.getOrDefault("userName", "User").trim();

        if (email.isEmpty() || !email.contains("@")) {
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", "Invalid email address."));
        }

        try {
            otpService.sendOtp(email, userName);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "OTP sent to " + email + ". Valid for 10 minutes."
            ));
        } catch (MessagingException e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("success", false,
                    "message", "Failed to send email. Check SMTP configuration. Error: " + e.getMessage()));
        }
    }

    // ── POST /api/otp/verify ─────────────────────────────────────────────────
    // Body: { "email": "user@example.com", "otp": "482619" }
    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyOtp(@RequestBody Map<String, String> body) {
        String email = body.getOrDefault("email", "").trim();
        String otp   = body.getOrDefault("otp", "").trim();

        if (email.isEmpty() || otp.isEmpty()) {
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", "Email and OTP are required."));
        }

        OtpService.VerifyResult result = otpService.verifyOtp(email, otp);

        if (result.success()) {
            return ResponseEntity.ok(Map.of("success", true, "message", result.message()));
        } else {
            return ResponseEntity.ok(Map.of("success", false, "message", result.message()));
        }
    }

    // ── GET /api/otp/health ───────────────────────────────────────────────────
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "EduTrack OTP Service",
            "version", "1.0.0"
        ));
    }
}
