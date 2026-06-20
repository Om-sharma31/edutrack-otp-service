# EduTrack OTP Email Service — Setup Guide

## Overview
This Spring Boot microservice handles real OTP email sending and verification
for the EduTrack School Management System.

**Endpoints:**
- `POST /api/otp/send`   → generates OTP and emails it to the user
- `POST /api/otp/verify` → checks the OTP the user entered
- `GET  /api/otp/health` → health check

---

## Step 1 — Prerequisites
- Java 17+  →  `java -version`
- Maven 3.8+  →  `mvn -version`
- A Gmail account (or any SMTP provider)

---

## Step 2 — Get a Gmail App Password

> You CANNOT use your normal Gmail password. You need an **App Password**.

1. Go to [myaccount.google.com](https://myaccount.google.com)
2. Security → 2-Step Verification → enable it if not already on
3. Security → Search "App passwords"
4. Select app: **Mail**, device: **Windows Computer** → click Generate
5. Copy the 16-character password (e.g. `abcd efgh ijkl mnop`)

---

## Step 3 — Configure credentials

Open `src/main/resources/application.properties` and replace:

```properties
spring.mail.username=YOUR_GMAIL_ADDRESS@gmail.com
spring.mail.password=YOUR_16_CHAR_APP_PASSWORD
```

Example:
```properties
spring.mail.username=myschool.edutrack@gmail.com
spring.mail.password=abcd efgh ijkl mnop
```

---

## Step 4 — Run the server

```bash
cd edutrack-otp-service
mvn spring-boot:run
```

Server starts at: `http://localhost:8080`

---

## Step 5 — Test it manually

**Send OTP:**
```bash
curl -X POST http://localhost:8080/api/otp/send \
  -H "Content-Type: application/json" \
  -d '{"email":"test@gmail.com","userName":"Test User"}'
```

Expected response:
```json
{"success":true,"message":"OTP sent to test@gmail.com. Valid for 10 minutes."}
```

**Verify OTP** (use the 6-digit code from the email):
```bash
curl -X POST http://localhost:8080/api/otp/verify \
  -H "Content-Type: application/json" \
  -d '{"email":"test@gmail.com","otp":"YOUR_6_DIGIT_OTP"}'
```

Expected response:
```json
{"success":true,"message":"Email verified successfully."}
```

---

## Step 6 — Connect to EduTrack frontend

In the EduTrack web app artifact, the OTP server URL is set to:
```
http://localhost:8080
```

The frontend calls:
1. `/api/otp/send` when user fills the registration form
2. `/api/otp/verify` when user enters the 6-digit OTP

---

## Using a different email provider (not Gmail)

**Outlook / Hotmail:**
```properties
spring.mail.host=smtp-mail.outlook.com
spring.mail.port=587
spring.mail.username=you@outlook.com
spring.mail.password=YOUR_PASSWORD
```

**Yahoo:**
```properties
spring.mail.host=smtp.mail.yahoo.com
spring.mail.port=587
spring.mail.username=you@yahoo.com
spring.mail.password=YOUR_APP_PASSWORD
```

---

## Security Notes
- OTPs expire after 10 minutes
- Max 5 verification attempts per OTP (brute-force protection)
- Each OTP can only be used once
- Expired OTPs are automatically purged every 5 minutes
- For production: replace in-memory store with Redis or a DB table

---

## Project Structure
```
edutrack-otp-service/
├── pom.xml
└── src/main/
    ├── java/com/edutrack/otp/
    │   ├── OtpServiceApplication.java   ← entry point
    │   ├── controller/
    │   │   └── OtpController.java       ← REST endpoints
    │   ├── service/
    │   │   └── OtpService.java          ← OTP logic + email sending
    │   ├── model/
    │   │   ├── OtpRecord.java           ← OTP data model
    │   │   └── Dtos.java               ← request/response shapes
    │   └── config/
    │       └── CorsConfig.java          ← CORS setup
    └── resources/
        └── application.properties       ← SMTP credentials here
```
