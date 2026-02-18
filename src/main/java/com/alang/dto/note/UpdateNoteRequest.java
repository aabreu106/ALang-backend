package com.alang.dto.note;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Request DTO for PATCH /notes/{id}
 * User can edit auto-generated notes.
 */
@Data
public class UpdateNoteRequest {
    private String title;
    private String summary;
    private String noteContent;
    private Map<String, Object> structuredContent;
    private List<NoteTagDto> tags;
}
