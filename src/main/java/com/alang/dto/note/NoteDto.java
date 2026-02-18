package com.alang.dto.note;

import com.alang.entity.NoteType;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Full note details for GET /notes/{id}
 */
@Data
public class NoteDto {
    private String id;
    private NoteType type;
    private String teachingLanguage; // language the note explains in
    private String learningLanguage; // language being taught/explained

    private String title; // e.g., "Difference between は and が"
    private String summary; // Short explanation (1-2 sentences)
    private String noteContent; // Longer explanation if available

    /**
     * Type-specific structured data.
     * Schema varies by NoteType (vocab, grammar, exception, other).
     */
    private Map<String, Object> structuredContent;

    /**
     * Controlled tags for categorization and filtering.
     */
    private List<NoteTagDto> tags;

    /**
     * Related notes (linked via note_relations table).
     */
    private List<NoteRelationDto> relations;

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
