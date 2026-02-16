package com.alang.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
     * The language used for UI and LLM explanations.
     * e.g., "ja" means the user receives explanations in Japanese.
     */
    @Column(nullable = false)
    private String appLanguageCode;

    /**
     * Languages the user is learning, stored as a PostgreSQL array.
     */
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "VARCHAR(255)[]")
    private List<String> targetLanguageCodes = new ArrayList<>();

    /**
     * Token usage tracking for cost control
     * TODO: Implement separate TokenUsage table for detailed tracking
     */
    private Long totalDailyTokensUsed = 0L;
    private LocalDateTime lastTokenResetDate; // For monthly limits

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(columnDefinition = "user_tier")
    private UserTier tier = UserTier.free;

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
