package com.edutrack.otp.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class OtpRecord {
    private String email;
    private String otp;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private boolean used;
    private int attemptCount;   // brute-force protection
}
