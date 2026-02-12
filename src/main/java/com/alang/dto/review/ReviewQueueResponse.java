package com.alang.dto.review;

import com.alang.dto.note.NoteDto;
import lombok.Data;
import java.util.List;

/**
 * Response for GET /review/queue
 *
 * Returns notes that are due for review based on spaced repetition algorithm.
 *
 * ARCHITECTURAL NOTE:
 * - Scheduling algorithm (SM-2, Anki-style) should be in ReviewService
 * - This is NOT just "show all notes" - it's intelligent scheduling
 */
@Data
public class ReviewQueueResponse {
    /**
     * Notes due for review right now
     */
    private List<NoteDto> dueNotes;

    /**
     * Total notes in the system
     */
    private int totalNotes;

    /**
     * Notes due today (including dueNotes)
     */
    private int dueTodayCount;

    /**
     * Estimated review time in minutes (optional)
     * Based on average time per note
     */
    private Integer estimatedMinutes;
}
