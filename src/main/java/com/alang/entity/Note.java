package com.alang.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Structured learning note extracted from conversations.
 *
 * ARCHITECTURAL NOTE:
 * - These are AUTO-GENERATED from LLM responses (via NoteService)
 * - Users can edit them after creation
 * - Used for spaced repetition review system
 *
 * TODO: Implement full JPA relationships
 * TODO: Add search/indexing for notes (full-text search)
 * TODO: Consider versioning (track edits)
 */
@Entity
@Table(name = "notes")
@Data
public class Note {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * The language the note explains in (e.g., English for an English-speaking user)
     */
    @ManyToOne
    @JoinColumn(name = "teaching_language_code", nullable = false)
    private Language teachingLanguage;

    /**
     * The language being taught/explained (e.g., Japanese if learning Japanese)
     */
    @ManyToOne
    @JoinColumn(name = "learning_language_code", nullable = false)
    private Language learningLanguage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "note_type")
    private NoteType type;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String noteContent;


    /**
     * Has the user manually edited this note?
     * If true, don't auto-update it from future LLM responses
     */
    @Column(nullable = false)
    private Boolean userEdited = false;

    /**
     * Spaced repetition metadata
     */
    private Integer reviewCount = 0;
    private LocalDateTime lastReviewedAt;
    private LocalDateTime nextReviewAt;

    /**
     * Ease factor for spaced repetition (SM-2 algorithm)
     * TODO: Implement in ReviewService
     */
    private Double easeFactor = 2.5;

    /**
     * Current interval in days
     */
    private Integer intervalDays = 1;

    /**
     * TODO: Related notes
     * @ManyToMany relationships to other notes
     * Example: "は" note relates to "が" note
     */

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (nextReviewAt == null) {
            // First review in 1 day
            nextReviewAt = LocalDateTime.now().plusDays(1);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
