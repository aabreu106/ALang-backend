package com.alang.service;

import com.alang.dto.review.ReviewQueueResponse;
import com.alang.dto.review.ReviewSubmissionRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Spaced repetition review service (Anki-style).
 *
 * RESPONSIBILITIES:
 * - Calculate review schedules using spaced repetition algorithm
 * - Provide review queue (notes due for review)
 * - Process review submissions (update schedules based on recall quality)
 * - Analytics (review stats, retention rates)
 *
 * ALGORITHM:
 * Use SM-2 or Anki algorithm for spaced repetition.
 * See: https://www.supermemo.com/en/archives1990-2015/english/ol/sm2
 */
public interface ReviewService {

    /**
     * Get notes due for review.
     *
     * Returns notes where nextReviewAt <= now, ordered by priority:
     * 1. Overdue notes (oldest first)
     * 2. Notes due today (by scheduled time)
     *
     * @param userId User ID
     * @param language Optional language filter (null = all languages)
     * @param limit Max notes to return
     * @return Review queue
     */
    ReviewQueueResponse getReviewQueue(String userId, String language, int limit);

    /**
     * Submit a review result.
     *
     * FLOW:
     * 1. Load the note
     * 2. Calculate next review interval based on quality (SM-2 algorithm)
     * 3. Update note's nextReviewAt, easeFactor, intervalDays, reviewCount
     * 4. Save ReviewEvent record (for analytics)
     * 5. Return updated note
     *
     * SM-2 ALGORITHM (TODO: implement):
     * - quality 1-2 (forgot): Reset interval to 1 day, reduce ease factor
     * - quality 3 (hard): Keep current interval, slightly reduce ease factor
     * - quality 4 (good): Increase interval by ease factor
     * - quality 5 (easy): Increase interval by ease factor * 1.3
     *
     * Ease factor starts at 2.5, ranges from 1.3 to 2.5
     *
     * @param submission Review submission (noteId, quality, timeSpent)
     * @param userId User ID (for authorization)
     */
    void submitReview(ReviewSubmissionRequest submission, String userId);

    /**
     * Calculate next review interval using SM-2 algorithm.
     *
     * @param currentInterval Current interval in days
     * @param easeFactor Current ease factor
     * @param quality Quality rating (1-4)
     * @return Next interval in days
     */
    int calculateNextInterval(int currentInterval, double easeFactor, int quality);

    /**
     * Update ease factor based on quality.
     *
     * @param currentEaseFactor Current ease factor
     * @param quality Quality rating (1-4)
     * @return Updated ease factor
     */
    double updateEaseFactor(double currentEaseFactor, int quality);

    /**
     * Get review statistics for a user.
     *
     * Stats:
     * - Total notes
     * - Notes reviewed today
     * - Average retention rate
     * - Notes due today/this week
     * - Review streak (days in a row)
     *
     * TODO: Implement analytics dashboard
     *
     * @param userId User ID
     * @return Review statistics
     */
    ReviewStats getReviewStats(String userId);

    /**
     * Review statistics DTO.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    class ReviewStats {
        private int totalNotes;
        private int reviewedToday;
        private int dueToday;
        private double averageRetention; // % of reviews with quality >= 4
        private int streakDays;
    }
}
