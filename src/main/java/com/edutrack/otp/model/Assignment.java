package com.edutrack.otp.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "assignments")
public class Assignment {

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "type", nullable = false)
    private String type; // assignment | notice | event | holiday

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "subject")
    private String subject;

    @Column(name = "target_class")
    private String targetClass;

    @Column(name = "due_date")
    private String dueDate;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "teacher_name")
    private String teacherName;

    @Column(name = "posted_at")
    private LocalDateTime postedAt = LocalDateTime.now();

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getTargetClass() { return targetClass; }
    public void setTargetClass(String targetClass) { this.targetClass = targetClass; }

    public String getDueDate() { return dueDate; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getTeacherName() { return teacherName; }
    public void setTeacherName(String teacherName) { this.teacherName = teacherName; }

    public LocalDateTime getPostedAt() { return postedAt; }
    public void setPostedAt(LocalDateTime postedAt) { this.postedAt = postedAt; }
}
