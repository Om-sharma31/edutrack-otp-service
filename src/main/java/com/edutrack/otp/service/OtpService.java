package com.edutrack.otp.service;

import com.edutrack.otp.model.OtpRecord;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
//import java.nio.charset.StandardCharsets;
//import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpService {

    private static final Logger log = LoggerFactory.getLogger(OtpService.class);

    // In-memory OTP store: email → OtpRecord
    // For production, swap this with Redis or a DB table.
    private final Map<String, OtpRecord> otpStore = new ConcurrentHashMap<>();

    private final JavaMailSender mailSender;
    private final SecureRandom random = new SecureRandom();

    @Value("${edutrack.otp.expiry-minutes:10}")
    private int expiryMinutes;

    @Value("${edutrack.otp.length:6}")
    private int otpLength;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${edutrack.mail.from-name:EduTrack School System}")
    private String fromName;

    public OtpService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    // ── Generate & send OTP ──────────────────────────────────────────────────
    public void sendOtp(String email, String userName) throws MessagingException {

        // Generate a numeric OTP of configured length
        String otp = generateOtp();

        LocalDateTime now = LocalDateTime.now();
        OtpRecord record = new OtpRecord(
            email, otp, now,
            now.plusMinutes(expiryMinutes),
            false, 0
        );
        otpStore.put(email.toLowerCase(), record);

        log.info("OTP generated for {} — sending email", email);
        sendEmail(email, userName, otp);
    }

    // ── Verify OTP entered by user ───────────────────────────────────────────
    public VerifyResult verifyOtp(String email, String inputOtp) {
        String key = email.toLowerCase();
        OtpRecord record = otpStore.get(key);

        if (record == null) {
            return new VerifyResult(false, "No OTP found for this email. Please request a new one.");
        }

        // Brute-force protection: max 5 attempts
        if (record.getAttemptCount() >= 5) {
            otpStore.remove(key);
            return new VerifyResult(false, "Too many failed attempts. Please request a new OTP.");
        }

        if (record.isUsed()) {
            return new VerifyResult(false, "This OTP has already been used. Please request a new one.");
        }

        if (LocalDateTime.now().isAfter(record.getExpiresAt())) {
            otpStore.remove(key);
            return new VerifyResult(false, "OTP has expired. Please request a new one.");
        }

        if (!record.getOtp().equals(inputOtp.trim())) {
            record.setAttemptCount(record.getAttemptCount() + 1);
            int remaining = 5 - record.getAttemptCount();
            return new VerifyResult(false, "Incorrect OTP. " + remaining + " attempt(s) remaining.");
        }

        // ✅ Success
        record.setUsed(true);
        otpStore.remove(key);  // clean up immediately after use
        log.info("OTP verified successfully for {}", email);
        return new VerifyResult(true, "Email verified successfully.");
    }

    // ── Cleanup task: remove expired OTPs every 5 minutes ───────────────────
    @Scheduled(fixedDelay = 5 * 60 * 1000)
    public void purgeExpiredOtps() {
        long before = otpStore.size();
        otpStore.entrySet().removeIf(e ->
            LocalDateTime.now().isAfter(e.getValue().getExpiresAt()) || e.getValue().isUsed()
        );
        long removed = before - otpStore.size();
        if (removed > 0) log.info("Purged {} expired/used OTP records", removed);
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private String generateOtp() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < otpLength; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    private void sendEmail(String toEmail, String userName, String otp) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
      try {
    MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail, fromName);
        helper.setTo(toEmail);
        helper.setSubject("EduTrack — Your Verification OTP");
        helper.setText(buildHtmlEmail(userName, otp), true);  // true = isHtml

        mailSender.send(message);
        log.info("OTP email dispatched to {}", toEmail);
      
      } catch(UnsupportedEncodingException e) {
        throw new MessagingException("Encoding error", e);
      }
      }

    private String buildHtmlEmail(String userName, String otp) {
        // OTP digits split for visual display
        String otpDigits = otp.chars()
            .mapToObj(c -> "<span style='display:inline-block;width:46px;height:56px;line-height:56px;"
                + "text-align:center;font-size:28px;font-weight:700;background:#f1f5f9;"
                + "border:2px solid #e2e8f0;border-radius:10px;margin:4px;color:#0f172a'>"
                + (char) c + "</span>")
            .reduce("", String::concat);

        return """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"/></head>
            <body style="margin:0;padding:0;background:#f8fafc;font-family:'Segoe UI',Arial,sans-serif">
              <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f8fafc;padding:40px 20px">
                <tr><td align="center">
                  <table width="560" cellpadding="0" cellspacing="0"
                         style="background:#ffffff;border-radius:16px;overflow:hidden;
                                border:1px solid #e2e8f0;max-width:560px">

                    <!-- Header -->
                    <tr>
                      <td style="background:linear-gradient(135deg,#1a56db,#1239a0);
                                 padding:28px 36px;text-align:center">
                        <div style="font-size:26px;font-weight:700;color:#fff;
                                    letter-spacing:-1px">EduTrack</div>
                        <div style="font-size:13px;color:rgba(255,255,255,0.7);margin-top:4px">
                          School Management System
                        </div>
                      </td>
                    </tr>

                    <!-- Body -->
                    <tr>
                      <td style="padding:36px">
                        <p style="font-size:16px;color:#0f172a;margin:0 0 8px">
                          Hello, <strong>%s</strong> 👋
                        </p>
                        <p style="font-size:14px;color:#475569;margin:0 0 28px;line-height:1.6">
                          You requested a one-time password to verify your EduTrack account.
                          Enter the OTP below within <strong>%d minutes</strong>.
                        </p>

                        <!-- OTP Box -->
                        <div style="text-align:center;margin:0 0 28px">
                          %s
                        </div>

                        <!-- Warning -->
                        <div style="background:#fef2f2;border:1px solid #fecaca;border-radius:10px;
                                    padding:14px 18px;font-size:13px;color:#991b1b;margin-bottom:24px">
                          ⚠️ <strong>Never share this OTP</strong> with anyone.
                          EduTrack staff will never ask for your OTP.
                          If you didn't request this, ignore this email.
                        </div>

                        <p style="font-size:13px;color:#94a3b8;margin:0">
                          This OTP expires in %d minutes and can only be used once.
                        </p>
                      </td>
                    </tr>

                    <!-- Footer -->
                    <tr>
                      <td style="background:#f8fafc;padding:18px 36px;border-top:1px solid #e2e8f0;
                                 text-align:center;font-size:12px;color:#94a3b8">
                        © 2025 EduTrack School Management System · Do not reply to this email
                      </td>
                    </tr>
                  </table>
                </td></tr>
              </table>
            </body>
            </html>
            """.formatted(userName, expiryMinutes, otpDigits, expiryMinutes);
    }

    // Simple result record
    public record VerifyResult(boolean success, String message) {}
}
