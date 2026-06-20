package com.edutrack.otp.repository;

import com.edutrack.otp.model.Mark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MarkRepository extends JpaRepository<Mark, Long> {
    List<Mark> findByStudentId(String studentId);
}
