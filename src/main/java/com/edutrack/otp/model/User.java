package com.edutrack.otp.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "email", unique = true, nullable = true)
    private String email;

    @Column(name = "role", nullable = false)
    private String role;

    @Column(name = "school_code")
    private String schoolCode;

    @Column(name = "class")
    private String className;

    @Column(name = "section")
    private String section;

    @Column(name = "roll_no")
    private Integer rollNo;

    @Column(name = "subject")
    private String subject;

    @Column(name = "child_id")
    private String childId;

    // BCrypt hash of the student's password (null for teachers/parents who use OTP)
    @Column(name = "password_hash")
    private String passwordHash;

    // Comma-separated class keys a teacher is assigned to, e.g. "6A,7B,9A"
    @Column(name = "teacher_classes", length = 500)
    private String teacherClasses;

    // JSON string of teacher's personal timetable schedule e.g. {"Monday_0":"9A","Tuesday_2":"10B"}
    @Column(name = "teacher_schedule", columnDefinition = "TEXT")
    private String teacherSchedule;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getSchoolCode() { return schoolCode; }
    public void setSchoolCode(String schoolCode) { this.schoolCode = schoolCode; }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }

    public Integer getRollNo() { return rollNo; }
    public void setRollNo(Integer rollNo) { this.rollNo = rollNo; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getChildId() { return childId; }
    public void setChildId(String childId) { this.childId = childId; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getTeacherClasses() { return teacherClasses; }
    public void setTeacherClasses(String teacherClasses) { this.teacherClasses = teacherClasses; }

    public String getTeacherSchedule() { return teacherSchedule; }
    public void setTeacherSchedule(String teacherSchedule) { this.teacherSchedule = teacherSchedule; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
