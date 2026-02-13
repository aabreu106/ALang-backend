package com.alang.dto.note;

import lombok.Data;

/**
 * Request DTO for PATCH /notes/{id}
 * User can edit auto-generated notes.
 */
@Data
public class UpdateNoteRequest {
    private String title;
    private String summary;
    private String noteContent;

}
