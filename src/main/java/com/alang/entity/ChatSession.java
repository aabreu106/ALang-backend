package com.alang.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * A chat session represents a single-topic conversation between a user and the language tutor.
 *
 * ARCHITECTURAL NOTE:
 * Notes are NOT auto-extracted from messages anymore. Instead, the user explicitly
 * triggers note creation via POST /chat/sessions/{id}/note once they are satisfied
 * with the conversation. This produces one well-formed note per session/topic.
 *
 * Sessions are scoped to a user + learning language pair.
 * Each session has its own isolated message history (via session_id on recent_messages).
 */
@Entity
@Table(name = "chat_sessions")
@Data
public class ChatSession {

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

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "session_status")
    private SessionStatus status = SessionStatus.active;

    /**
     * Optional user-supplied or LLM-derived label for the session.
     * e.g. "el/la exceptions", "て-form conjugation"
     */
    private String title;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Set when the session status transitions to 'closed'.
     * Informational — used by Week 4 summarization to scope cleanup.
     */
    private LocalDateTime closedAt;

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
