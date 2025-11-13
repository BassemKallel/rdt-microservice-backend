package com.replate.usermanagementservice.repository;
import com.replate.usermanagementservice.dto.UserResponse;
import com.replate.usermanagementservice.model.User;
import com.replate.usermanagementservice.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findAllByIsValidatedFalseAndRoleIn(List<UserRole> roles);
}