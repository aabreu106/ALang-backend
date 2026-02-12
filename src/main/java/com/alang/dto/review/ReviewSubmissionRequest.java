package com.alang.dto.review;

import lombok.Data;

/**
 * Request for POST /notes/reviewed
 *
 * User has reviewed a note (Anki-style).
 */
@Data
public class ReviewSubmissionRequest {
    private String noteId;

    /**
     * How well did the user remember this note?
     * 1 = completely forgot
     * 2 = barely remembered
     * 3 = remembered with effort
     * 4 = remembered easily
     * 5 = perfect recall
     *
     * This affects the next review interval (spaced repetition)
     */
    private int quality; // 1-5

    /**
     * Optional: Time spent reviewing (in seconds)
     * Can be used for analytics
     */
    private Integer timeSpentSeconds;
}
