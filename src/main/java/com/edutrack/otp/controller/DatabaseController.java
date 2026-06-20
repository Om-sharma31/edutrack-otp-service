package com.edutrack.otp.controller;

import com.edutrack.otp.model.*;
import com.edutrack.otp.repository.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/db")
public class DatabaseController {

    private final UserRepository userRepo;
    private final ReviewRepository reviewRepo;
    private final MarkRepository markRepo;
    private final AttendanceRepository attendanceRepo;
    private final AssignmentRepository assignmentRepo;

    public DatabaseController(UserRepository userRepo, ReviewRepository reviewRepo,
                               MarkRepository markRepo, AttendanceRepository attendanceRepo,
                               AssignmentRepository assignmentRepo) {
        this.userRepo = userRepo;
        this.reviewRepo = reviewRepo;
        this.markRepo = markRepo;
        this.attendanceRepo = attendanceRepo;
        this.assignmentRepo = assignmentRepo;
    }

    // ── USERS ─────────────────────────────────────────────────────────────────

    @PostMapping("/users")
    public ResponseEntity<Map<String, Object>> saveUser(@RequestBody Map<String, String> body) {
        String email = body.getOrDefault("email", "").trim();
        if (email.isEmpty())
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Email is required"));
        if (userRepo.existsByEmail(email))
            return ResponseEntity.ok(Map.of("success", false, "message", "Email already registered"));
        User user = new User();
        user.setId(body.getOrDefault("id", UUID.randomUUID().toString()));
        user.setName(body.getOrDefault("name", ""));
        user.setEmail(email);
        user.setRole(body.getOrDefault("role", "student"));
        user.setSchoolCode(body.getOrDefault("schoolCode", ""));
        user.setChildId(body.getOrDefault("childId", ""));
        userRepo.save(user);
        return ResponseEntity.ok(Map.of("success", true, "message", "User saved successfully"));
    }

    @GetMapping("/users/email/{email}")
    public ResponseEntity<Map<String, Object>> getUserByEmail(@PathVariable String email) {
        return userRepo.findByEmail(email)
            .map(u -> ResponseEntity.ok(Map.of("success", true, "user", userToMap(u))))
            .orElse(ResponseEntity.ok(Map.of("success", false, "message", "User not found")));
    }

    @GetMapping("/users")
    public ResponseEntity<List<Map<String, Object>>> getAllUsers() {
        return ResponseEntity.ok(userRepo.findAll().stream()
            .map(this::userToMap).collect(Collectors.toList()));
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<Map<String, Object>> updateUser(@PathVariable String id,
                                                          @RequestBody Map<String, Object> body) {
        return userRepo.findById(id).map(u -> {
            if (body.containsKey("name"))    u.setName(body.get("name").toString());
            if (body.containsKey("className")) u.setClassName(body.get("className").toString());
            if (body.containsKey("section")) u.setSection(body.get("section").toString());
            if (body.containsKey("rollNo") && body.get("rollNo") != null)
                u.setRollNo(Integer.parseInt(body.get("rollNo").toString()));
            if (body.containsKey("subject")) u.setSubject(body.get("subject").toString());
            if (body.containsKey("teacherClasses"))
                u.setTeacherClasses(body.get("teacherClasses").toString());
            userRepo.save(u);
            return ResponseEntity.ok(Map.<String, Object>of("success", true, "user", userToMap(u)));
        }).orElse(ResponseEntity.ok(Map.of("success", false, "message", "User not found")));
    }

    // ── STUDENTS ──────────────────────────────────────────────────────────────

    @GetMapping("/students")
    public ResponseEntity<List<Map<String, Object>>> getAllStudents(
            @RequestParam(required = false) String schoolCode) {
        List<User> students = (schoolCode != null && !schoolCode.isEmpty())
            ? userRepo.findByRoleAndSchoolCode("student", schoolCode)
            : userRepo.findByRole("student");
        return ResponseEntity.ok(students.stream().map(this::studentToMap).collect(Collectors.toList()));
    }

    @PostMapping("/students")
    public ResponseEntity<Map<String, Object>> addStudent(@RequestBody Map<String, Object> body) {
        try {
            String email = body.getOrDefault("email", "").toString().trim();
            if (!email.isEmpty() && userRepo.existsByEmail(email))
                return ResponseEntity.ok(Map.of("success", false, "message", "Email already registered"));

            // Check duplicate: same roll number in same class + section
            String cls = body.containsKey("className")
                ? body.get("className").toString()
                : body.getOrDefault("class", "").toString();
            String section = body.getOrDefault("section", "").toString().toUpperCase();
            int rollNo = body.get("rollNo") != null ? Integer.parseInt(body.get("rollNo").toString()) : 0;
            if (rollNo > 0) {
                Optional<User> existing = userRepo.findByRoleAndClassNameAndSectionAndRollNo(
                    "student", cls, section, rollNo);
                if (existing.isPresent())
                    return ResponseEntity.ok(Map.of("success", false,
                        "message", "A student with Roll No " + rollNo + " already exists in Class " + cls + section));
            }

            User u = new User();
            u.setId(body.getOrDefault("id", UUID.randomUUID().toString()).toString());
            u.setName(body.getOrDefault("name", "").toString());
            u.setEmail(email.isEmpty() ? null : email);
            u.setRole("student");
            u.setSchoolCode(body.getOrDefault("schoolCode", "").toString());
            u.setClassName(cls);
            u.setSection(section);
            if (rollNo > 0) u.setRollNo(rollNo);
            userRepo.save(u);
            return ResponseEntity.ok(Map.of("success", true, "id", u.getId(), "student", studentToMap(u)));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", false, "message", "Database error: " + e.getMessage()));
        }
    }

    @PostMapping("/students/bulk")
    public ResponseEntity<Map<String, Object>> bulkAddStudents(@RequestBody List<Map<String, Object>> students) {
        int added = 0, skipped = 0;
        for (Map<String, Object> body : students) {
            String email = body.getOrDefault("email", "").toString().trim();
            if (!email.isEmpty() && userRepo.existsByEmail(email)) { skipped++; continue; }
            User u = new User();
            u.setId(body.getOrDefault("id", UUID.randomUUID().toString()).toString());
            u.setName(body.getOrDefault("name", "").toString());
            u.setEmail(email.isEmpty() ? null : email);
            u.setRole("student");
            u.setSchoolCode(body.getOrDefault("schoolCode", "").toString());
            u.setClassName(body.getOrDefault("class", "").toString());
            u.setSection(body.getOrDefault("section", "").toString());
            if (body.get("rollNo") != null)
                u.setRollNo(Integer.parseInt(body.get("rollNo").toString()));
            userRepo.save(u);
            added++;
        }
        return ResponseEntity.ok(Map.of("success", true, "added", added, "skipped", skipped));
    }

    // ── TEACHERS ──────────────────────────────────────────────────────────────

    @GetMapping("/teachers")
    public ResponseEntity<List<Map<String, Object>>> getAllTeachers(
            @RequestParam(required = false) String schoolCode) {
        List<User> teachers = (schoolCode != null && !schoolCode.isEmpty())
            ? userRepo.findByRoleAndSchoolCode("teacher", schoolCode)
            : userRepo.findByRole("teacher");
        return ResponseEntity.ok(teachers.stream().map(this::teacherToMap).collect(Collectors.toList()));
    }

    @PutMapping("/users/{id}/teacher-setup")
    public ResponseEntity<Map<String, Object>> saveTeacherSetup(@PathVariable String id,
                                                                  @RequestBody Map<String, Object> body) {
        return userRepo.findById(id).map(u -> {
            if (body.containsKey("subject"))
                u.setSubject(body.get("subject").toString());
            if (body.containsKey("classes")) {
                @SuppressWarnings("unchecked")
                List<String> classes = (List<String>) body.get("classes");
                u.setTeacherClasses(String.join(",", classes));
            }
            if (body.containsKey("teacherSchedule"))
                u.setTeacherSchedule(body.get("teacherSchedule").toString());
            userRepo.save(u);
            return ResponseEntity.ok(Map.<String, Object>of("success", true, "teacher", teacherToMap(u)));
        }).orElse(ResponseEntity.ok(Map.of("success", false, "message", "Teacher not found")));
    }

    // ── REVIEWS ───────────────────────────────────────────────────────────────

    @PostMapping("/reviews")
    public ResponseEntity<Map<String, Object>> saveReview(@RequestBody Map<String, String> body) {
        Review review = new Review();
        review.setStudentId(body.get("studentId"));
        review.setTeacherName(body.getOrDefault("teacherName", ""));
        review.setSubject(body.getOrDefault("subject", "General"));
        review.setReviewText(body.getOrDefault("reviewText", ""));
        review.setReviewType(body.getOrDefault("reviewType", "positive"));
        reviewRepo.save(review);
        return ResponseEntity.ok(Map.of("success", true, "message", "Review saved"));
    }

    @GetMapping("/reviews/{studentId}")
    public ResponseEntity<List<Review>> getReviews(@PathVariable String studentId) {
        return ResponseEntity.ok(reviewRepo.findByStudentId(studentId));
    }

    // ── MARKS ─────────────────────────────────────────────────────────────────

    @PostMapping("/marks")
    public ResponseEntity<Map<String, Object>> saveMark(@RequestBody Map<String, Object> body) {
        Mark mark = new Mark();
        mark.setStudentId((String) body.get("studentId"));
        mark.setSubject(body.getOrDefault("subject", "").toString());
        mark.setTestName(body.getOrDefault("testName", "").toString());
        mark.setScore(Integer.parseInt(body.getOrDefault("score", 0).toString()));
        mark.setMaxScore(Integer.parseInt(body.getOrDefault("maxScore", 100).toString()));
        markRepo.save(mark);
        return ResponseEntity.ok(Map.of("success", true, "message", "Mark saved"));
    }

    @GetMapping("/marks/{studentId}")
    public ResponseEntity<List<Mark>> getMarks(@PathVariable String studentId) {
        return ResponseEntity.ok(markRepo.findByStudentId(studentId));
    }

    /**
     * Returns marks in the nested frontend format:
     * { "Mathematics": { "W1": 22, "Mid-Year": 44, "Final": 88 }, ... }
     */
    @GetMapping("/marks/{studentId}/formatted")
    public ResponseEntity<Map<String, Object>> getMarksFormatted(@PathVariable String studentId) {
        List<Mark> marks = markRepo.findByStudentId(studentId);
        Map<String, Map<String, Object>> result = new LinkedHashMap<>();
        for (Mark m : marks) {
            result.computeIfAbsent(m.getSubject(), k -> new LinkedHashMap<>())
                  .put(m.getTestName(), m.getScore());
        }
        return ResponseEntity.ok(Map.of("success", true, "marks", result));
    }

    // ── ATTENDANCE ────────────────────────────────────────────────────────────

    @PostMapping("/attendance")
    public ResponseEntity<Map<String, Object>> saveAttendance(@RequestBody Map<String, String> body) {
        Attendance att = new Attendance();
        att.setStudentId(body.get("studentId"));
        att.setWeek(body.getOrDefault("week", ""));
        att.setDay(body.getOrDefault("day", ""));
        att.setStatus(body.getOrDefault("status", "present"));
        attendanceRepo.save(att);
        return ResponseEntity.ok(Map.of("success", true, "message", "Attendance saved"));
    }

    @GetMapping("/attendance/{studentId}")
    public ResponseEntity<List<Attendance>> getAttendance(@PathVariable String studentId) {
        return ResponseEntity.ok(attendanceRepo.findByStudentId(studentId));
    }

    /**
     * Returns attendance in the nested frontend format:
     * { "Week 1": { "Monday": "present", "Tuesday": "absent" }, ... }
     */
    @GetMapping("/attendance/{studentId}/formatted")
    public ResponseEntity<Map<String, Object>> getAttendanceFormatted(@PathVariable String studentId) {
        List<Attendance> records = attendanceRepo.findByStudentId(studentId);
        Map<String, Map<String, String>> result = new LinkedHashMap<>();
        for (Attendance a : records) {
            result.computeIfAbsent(a.getWeek(), k -> new LinkedHashMap<>())
                  .put(a.getDay(), a.getStatus());
        }
        return ResponseEntity.ok(Map.of("success", true, "attendance", result));
    }

    // ── ASSIGNMENTS / NOTICES ─────────────────────────────────────────────────

    @PostMapping("/assignments")
    public ResponseEntity<Map<String, Object>> saveAssignment(@RequestBody Map<String, String> body) {
        String id = body.getOrDefault("id", UUID.randomUUID().toString());
        if (assignmentRepo.existsById(id))
            return ResponseEntity.ok(Map.of("success", false, "message", "Already exists"));
        Assignment a = new Assignment();
        a.setId(id);
        a.setType(body.getOrDefault("type", "notice"));
        a.setTitle(body.getOrDefault("title", ""));
        a.setSubject(body.getOrDefault("subject", ""));
        a.setTargetClass(body.getOrDefault("targetClass", ""));
        a.setDueDate(body.getOrDefault("dueDate", ""));
        a.setDescription(body.getOrDefault("description", ""));
        a.setTeacherName(body.getOrDefault("teacherName", ""));
        a.setPostedAt(LocalDateTime.now());
        assignmentRepo.save(a);
        return ResponseEntity.ok(Map.of("success", true, "message", "Assignment saved"));
    }

    @GetMapping("/assignments")
    public ResponseEntity<List<Assignment>> getAllAssignments() {
        return ResponseEntity.ok(assignmentRepo.findAllByOrderByPostedAtDesc());
    }

    @GetMapping("/assignments/class/{targetClass}")
    public ResponseEntity<List<Assignment>> getAssignmentsForClass(@PathVariable String targetClass) {
        return ResponseEntity.ok(
            assignmentRepo.findAllByOrderByPostedAtDesc().stream()
                .filter(a -> a.getTargetClass() == null || a.getTargetClass().isEmpty()
                          || a.getTargetClass().equals(targetClass))
                .collect(Collectors.toList())
        );
    }

    // ── PRIVATE HELPERS ───────────────────────────────────────────────────────

    private Map<String, Object> userToMap(User u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",         u.getId());
        m.put("name",       u.getName());
        m.put("email",      u.getEmail());
        m.put("role",       u.getRole());
        m.put("schoolCode", u.getSchoolCode() != null ? u.getSchoolCode() : "");
        m.put("childId",    u.getChildId()    != null ? u.getChildId()    : "");
        return m;
    }

    private Map<String, Object> studentToMap(User u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",         u.getId());
        m.put("name",       u.getName());
        m.put("email",      u.getEmail());
        m.put("class",      u.getClassName()  != null ? u.getClassName()  : "");
        m.put("section",    u.getSection()    != null ? u.getSection()    : "");
        m.put("rollNo",     u.getRollNo()     != null ? u.getRollNo()     : 0);
        m.put("schoolCode", u.getSchoolCode() != null ? u.getSchoolCode() : "");
        m.put("marks",      new HashMap<>());
        return m;
    }

    private Map<String, Object> teacherToMap(User u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",      u.getId());
        m.put("name",    u.getName());
        m.put("email",   u.getEmail());
        m.put("subject", u.getSubject() != null ? u.getSubject() : "");
        String tcs = u.getTeacherClasses();
        List<String> classes = (tcs != null && !tcs.isBlank())
            ? Arrays.asList(tcs.split(","))
            : new ArrayList<>();
        m.put("classes", classes);
        // teacherSchedule is stored as a JSON string; send as-is for frontend to parse
        m.put("teacherScheduleJson", u.getTeacherSchedule() != null ? u.getTeacherSchedule() : "");
        return m;
    }
}
