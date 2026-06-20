package com.edutrack.otp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling   // needed for the OTP expiry cleanup task
public class OtpServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OtpServiceApplication.class, args);
    }
}
