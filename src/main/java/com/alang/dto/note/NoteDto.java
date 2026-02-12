package com.alang.dto.note;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Full note details for GET /notes/{id}
 */
@Data
public class NoteDto {
    private String id;
    private String type; // "vocab", "grammar", "exception"
    private String language; // "ja", "es", etc.

    private String title; // e.g., "Difference between は and が"
    private String summary; // Short explanation (1-2 sentences)
    private String detailedExplanation; // Longer explanation if available

    /**
     * Concrete examples showing usage
     * Example: ["私は学生です (I am a student)", "誰が来ましたか (Who came?)"]
     */
    private List<String> examples;

    /**
     * Related notes (by ID)
     * Example: Related grammar points, similar vocabulary
     */
    private List<String> relatedNoteIds;

    /**
     * Confidence score from extraction
     */
    private Double confidence;

    /**
     * User can manually override/edit notes
     */
    private Boolean userEdited = false;

    /**
     * Review metadata
     */
    private Integer reviewCount = 0;
    private LocalDateTime lastReviewedAt;
    private LocalDateTime nextReviewAt; // Spaced repetition scheduling

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
