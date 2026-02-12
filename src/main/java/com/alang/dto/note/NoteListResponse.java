package com.alang.dto.note;

import lombok.Data;
import java.util.List;

/**
 * Response for GET /notes
 * Supports filtering by language, type, confidence, etc.
 */
@Data
public class NoteListResponse {
    private List<NoteDto> notes;
    private int totalCount;
    private int page;
    private int pageSize;
}
