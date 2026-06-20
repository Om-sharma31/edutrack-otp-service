package com.edutrack.otp.repository;

import com.edutrack.otp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findByRole(String role);
    List<User> findBySchoolCode(String schoolCode);
    List<User> findByRoleAndSchoolCode(String role, String schoolCode);
    // Used for student Roll-No login: class + section + rollNo is unique per school
    Optional<User> findByRoleAndClassNameAndSectionAndRollNoAndSchoolCode(
        String role, String className, String section, Integer rollNo, String schoolCode);
    // Fallback when schoolCode not yet configured
    Optional<User> findByRoleAndClassNameAndSectionAndRollNo(
        String role, String className, String section, Integer rollNo);
}
