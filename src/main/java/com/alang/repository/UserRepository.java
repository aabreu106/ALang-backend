package com.alang.repository;

import com.alang.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * User repository.
 *
 * Spring Data JPA will auto-implement basic CRUD methods.
 *
 * TODO: Add custom queries if needed
 * TODO: Add query methods for user search, analytics
 */
@Repository
public interface UserRepository extends JpaRepository<User, String> {

    /**
     * Find user by email (for login).
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if email already exists (for signup validation).
     */
    boolean existsByEmail(String email);

    // TODO: Add custom query methods as needed
    // Example:
    // List<User> findByTier(String tier);
    // List<User> findByCreatedAtAfter(LocalDateTime date);
}
