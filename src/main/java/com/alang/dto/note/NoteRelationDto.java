package com.alang.dto.note;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NoteRelationDto {
    private String relatedNoteId;
    private String relatedNoteTitle;
    private String relationType;
}
