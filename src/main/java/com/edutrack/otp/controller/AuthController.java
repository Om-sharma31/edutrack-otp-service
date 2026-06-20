package com.edutrack.otp.controller;

import com.edutrack.otp.model.User;
import com.edutrack.otp.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepo;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthController(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    /**
     * Student login — Roll Number + Class + Section + Password.
     * Body: { rollNo, className, section, schoolCode, password }
     */
    @PostMapping("/student-login")
    public ResponseEntity<Map<String, Object>> studentLogin(@RequestBody Map<String, Object> body) {
        String className  = body.getOrDefault("className", "").toString().trim();
        String section    = body.getOrDefault("section",   "").toString().trim().toUpperCase();
        String schoolCode = body.getOrDefault("schoolCode","").toString().trim();
        String password   = body.getOrDefault("password",  "").toString();

        int rollNo;
        try {
            rollNo = Integer.parseInt(body.getOrDefault("rollNo", "0").toString().trim());
        } catch (NumberFormatException e) {
            return ResponseEntity.ok(Map.of("success", false, "message", "Invalid roll number"));
        }

        if (className.isEmpty() || section.isEmpty() || password.isEmpty() || rollNo == 0)
            return ResponseEntity.ok(Map.of("success", false, "message", "Please fill in all fields"));

        // Try with schoolCode first, then without (for schools not yet configured)
        Optional<User> userOpt = schoolCode.isEmpty()
            ? userRepo.findByRoleAndClassNameAndSectionAndRollNo("student", className, section, rollNo)
            : userRepo.findByRoleAndClassNameAndSectionAndRollNoAndSchoolCode(
                "student", className, section, rollNo, schoolCode);

        // Fallback without schoolCode if not found
        if (userOpt.isEmpty() && !schoolCode.isEmpty()) {
            userOpt = userRepo.findByRoleAndClassNameAndSectionAndRollNo(
                "student", className, section, rollNo);
        }

        if (userOpt.isEmpty())
            return ResponseEntity.ok(Map.of("success", false,
                "message", "No student found with Roll No " + rollNo + " in Class " + className + section));

        User u = userOpt.get();

        if (u.getPasswordHash() == null || u.getPasswordHash().isBlank())
            return ResponseEntity.ok(Map.of("success", false,
                "message", "Password not set yet. Ask your class teacher to set your password."));

        if (!passwordEncoder.matches(password, u.getPasswordHash()))
            return ResponseEntity.ok(Map.of("success", false, "message", "Incorrect password"));

        return ResponseEntity.ok(Map.of(
            "success", true,
            "user", Map.of(
                "id",         u.getId(),
                "name",       u.getName(),
                "email",      u.getEmail() != null ? u.getEmail() : "",
                "role",       "student",
                "class",      u.getClassName() != null ? u.getClassName() : "",
                "section",    u.getSection()   != null ? u.getSection()   : "",
                "rollNo",     u.getRollNo()    != null ? u.getRollNo()    : 0,
                "schoolCode", u.getSchoolCode()!= null ? u.getSchoolCode(): ""
            )
        ));
    }

    /**
     * Set or reset a student's password.
     * Body: { userId, password }
     * Called by a teacher/admin or by the student themselves (changing their own password).
     */
    @PutMapping("/set-password")
    public ResponseEntity<Map<String, Object>> setPassword(@RequestBody Map<String, String> body) {
        String userId   = body.getOrDefault("userId",   "").trim();
        String password = body.getOrDefault("password", "").trim();

        if (userId.isEmpty() || password.isEmpty())
            return ResponseEntity.ok(Map.of("success", false, "message", "userId and password are required"));
        if (password.length() < 4)
            return ResponseEntity.ok(Map.of("success", false, "message", "Password must be at least 4 characters"));

        return userRepo.findById(userId).map(u -> {
            u.setPasswordHash(passwordEncoder.encode(password));
            userRepo.save(u);
            return ResponseEntity.ok(Map.<String, Object>of("success", true, "message", "Password updated"));
        }).orElse(ResponseEntity.ok(Map.of("success", false, "message", "User not found")));
    }

    /**
     * Change password — requires the current password for verification.
     * Body: { userId, currentPassword, newPassword }
     */
    @PutMapping("/change-password")
    public ResponseEntity<Map<String, Object>> changePassword(@RequestBody Map<String, String> body) {
        String userId      = body.getOrDefault("userId",          "").trim();
        String currentPass = body.getOrDefault("currentPassword", "");
        String newPass     = body.getOrDefault("newPassword",     "").trim();

        if (userId.isEmpty() || currentPass.isEmpty() || newPass.isEmpty())
            return ResponseEntity.ok(Map.of("success", false, "message", "All fields are required"));
        if (newPass.length() < 4)
            return ResponseEntity.ok(Map.of("success", false, "message", "New password must be at least 4 characters"));

        return userRepo.findById(userId).map(u -> {
            if (u.getPasswordHash() == null || !passwordEncoder.matches(currentPass, u.getPasswordHash()))
                return ResponseEntity.ok(Map.<String, Object>of("success", false, "message", "Current password is incorrect"));
            u.setPasswordHash(passwordEncoder.encode(newPass));
            userRepo.save(u);
            return ResponseEntity.ok(Map.<String, Object>of("success", true, "message", "Password changed successfully"));
        }).orElse(ResponseEntity.ok(Map.of("success", false, "message", "User not found")));
    }
}
