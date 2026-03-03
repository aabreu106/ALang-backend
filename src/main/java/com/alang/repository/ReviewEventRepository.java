package com.alang.repository;

import com.alang.entity.ReviewEvent;
import com.alang.entity.User;
import com.alang.entity.Note;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Review event repository.
 *
 * Used for:
 * 1. Tracking review history
 * 2. Analytics (review frequency)
 * 3. Algorithm tuning
 *
 * TODO: Add indexes on (userId, reviewedAt) for analytics queries
 */
@Repository
public interface ReviewEventRepository extends JpaRepository<ReviewEvent, String> {

    /**
     * Get review history for a note.
     */
    List<ReviewEvent> findByNoteOrderByReviewedAtDesc(Note note);

    /**
     * Get review history for a user.
     */
    List<ReviewEvent> findByUserOrderByReviewedAtDesc(User user);

    /**
     * Count reviews for a user on a specific date (for streak calculation).
     */
    long countByUserAndReviewedAtBetween(User user, LocalDateTime start, LocalDateTime end);

    /**
     * Get reviews for a user in a date range (for analytics).
     */
    List<ReviewEvent> findByUserAndReviewedAtBetween(User user, LocalDateTime start, LocalDateTime end);

// TODO: Add more analytics queries
    // - Reviews per day/week
    // - Average time spent per review
    // - Quality trends over time
}
