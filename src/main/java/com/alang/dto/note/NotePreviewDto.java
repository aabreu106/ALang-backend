package com.alang.dto.note;

import lombok.Data;

/**
 * Minimal note preview returned in chat responses.
 * User can click through to see full note details.
 */
@Data
public class NotePreviewDto {
    private String id;
    private String type; // "vocab", "grammar", "exception"
    private String title; // e.g., "は vs が"

    /**
     * Confidence score (0.0 to 1.0) indicating how confident the LLM is about this note.
     * Low confidence notes should be flagged for user review.
     *
     * ARCHITECTURAL NOTE:
     * - Extracted by LLMService from LLM response metadata
     * - Low confidence (<0.3) = needs review
     * - High confidence (>0.7) = probably accurate
     */
    private Double confidence;
}
