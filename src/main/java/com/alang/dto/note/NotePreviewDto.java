package com.alang.dto.note;

import com.alang.entity.NoteType;
import lombok.Data;

/**
 * Minimal note preview returned in chat responses.
 * User can click through to see full note details.
 */
@Data
public class NotePreviewDto {
    private String id;
    private NoteType type;
    private String title; // e.g., "は vs が"

    private Integer intervalDays; // derived mastery indicator from SM-2
}
