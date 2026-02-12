package com.alang.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * User entity.
 *
 * TODO: Implement full JPA mappings
 * TODO: Add password hashing (use BCrypt)
 * TODO: Add account status (active, suspended, deleted)
 * TODO: Add email verification
 * TODO: Consider user preferences (notification settings, default model preference)
 */
@Entity
@Table(name = "users")
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String passwordHash; // TODO: Never store plain passwords

    @Column(nullable = false)
    private String displayName;

    /**
     * Languages the user is learning.
     * TODO: Implement many-to-many relationship with Language entity
     */
    @ElementCollection
    private Set<String> targetLanguageCodes = new HashSet<>();

    /**
     * Default language for new conversations
     */
    private String preferredLanguageCode;

    /**
     * Token usage tracking for cost control
     * TODO: Implement separate TokenUsage table for detailed tracking
     */
    private Long totalTokensUsed = 0L;
    private LocalDateTime lastTokenResetDate; // For monthly limits

    /**
     * Premium tier (for future monetization)
     * TODO: Implement subscription/tier system
     */
    private String tier = "free"; // "free", "premium"

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
