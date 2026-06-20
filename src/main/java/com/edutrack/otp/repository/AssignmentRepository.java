package com.edutrack.otp.repository;

import com.edutrack.otp.model.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AssignmentRepository extends JpaRepository<Assignment, String> {
    List<Assignment> findByTargetClassOrTargetClassIsNullOrTargetClassOrderByPostedAtDesc(
        String targetClass1, String targetClass2);
    List<Assignment> findAllByOrderByPostedAtDesc();
}
