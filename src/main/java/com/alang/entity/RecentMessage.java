package com.alang.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;

/**
 * Recent chat messages (TEMPORARY storage).
 *
 * CRITICAL ARCHITECTURAL NOTE:
 * These are EPHEMERAL. They should have a TTL (e.g., 24-48 hours).
 * After TTL, they are summarized into ConversationSummary and DELETED.
 *
 * Why not keep all messages forever?
 * 1. Database bloat (millions of messages)
 * 2. Token cost (can't send all to LLM)
 * 3. Privacy (old messages can be purged)
 * 4. Performance (queries get slower)
 *
 * Only recent messages (last 5-10 exchanges) are kept for immediate context.
 * Everything else is condensed into ConversationSummary.
 *
 * TODO: Implement TTL mechanism
 * Options:
 * 1. Application-level cleanup job (scheduled task)
 * 2. PostgreSQL pg_cron extension
 * 3. Move to Redis with native TTL support
 *
 * TODO: Implement summarization trigger
 * When message count exceeds threshold, trigger summarization and delete old messages
 */
@Entity
@Table(name = "recent_messages")
@Data
public class RecentMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "teaching_language_code", nullable = false)
    private Language teachingLanguage;

    @ManyToOne
    @JoinColumn(name = "learning_language_code", nullable = false)
    private Language learningLanguage;

    @ManyToOne
    @JoinColumn(name = "session_id", nullable = false)
    private ChatSession session;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "role_type")
    private RoleType role;

    /**
     * The actual message content
     */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    /**
     * Metadata: which model generated this (for assistant messages)
     */
    private String modelUsed;

    /**
     * Token count for this message
     * Used for context budget management
     */
    private Integer tokenCount;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * When should this message be deleted?
     * Set to createdAt + 48 hours (or whatever TTL you choose)
     */
    private LocalDateTime expiresAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (expiresAt == null) {
            // Default TTL: 48 hours
            expiresAt = LocalDateTime.now().plusHours(48);
        }
    }

    /**
     * TODO: Implement cleanup job
     * Example Spring scheduled task:
     *
     * @Scheduled(cron = "0 0 * * * *") // Every hour
     * public void cleanupExpiredMessages() {
     *     recentMessageRepository.deleteByExpiresAtBefore(LocalDateTime.now());
     * }
     *
     * TODO: Before deleting, check if these messages have been summarized
     * Don't delete messages that haven't been summarized yet!
     */
}
