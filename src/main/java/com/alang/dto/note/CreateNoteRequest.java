package com.alang.dto.note;

import com.alang.entity.NoteType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request DTO for POST /notes
 * User manually creates a study note.
 */
@Data
public class CreateNoteRequest {
    @NotNull
    private NoteType type;

    @NotBlank
    private String language; // learning language code

    @NotBlank
    @Size(max = 60)
    private String title;

    private String summary;
    private String noteContent;
}
