package com.alang.dto.note;

import lombok.Data;
import java.util.List;

/**
 * Request DTO for PATCH /notes/{id}
 * User can edit auto-generated notes.
 */
@Data
public class UpdateNoteRequest {
    private String title;
    private String summary;
    private String detailedExplanation;
    private List<String> examples;

    /**
     * User can manually adjust confidence if they think note is accurate
     */
    private Double confidence;
}
