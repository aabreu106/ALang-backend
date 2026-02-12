package com.alang.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Record of a review session (Anki-style).
 *
 * Tracks when user reviewed a note and how well they recalled it.
 * Used for spaced repetition algorithm and analytics.
 *
 * TODO: Implement analytics queries (review accuracy over time, etc.)
 */
@Entity
@Table(name = "review_events")
@Data
public class ReviewEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "note_id", nullable = false)
    private Note note;

    /**
     * Quality of recall (1-5 scale, like Anki)
     * 1 = completely forgot
     * 2 = barely remembered (after seeing answer)
     * 3 = remembered with effort
     * 4 = remembered easily
     * 5 = perfect recall
     */
    @Column(nullable = false)
    private Integer quality;

    /**
     * How long did the user spend reviewing this note? (in seconds)
     * Optional, for analytics
     */
    private Integer timeSpentSeconds;

    /**
     * What was the interval before this review?
     * Used for algorithm tuning
     */
    private Integer previousIntervalDays;

    /**
     * What was the next interval calculated?
     * Used for algorithm tuning and debugging
     */
    private Integer nextIntervalDays;

    /**
     * Ease factor at time of review
     */
    private Double easeFactor;

    @Column(nullable = false, updatable = false)
    private LocalDateTime reviewedAt;

    @PrePersist
    protected void onCreate() {
        reviewedAt = LocalDateTime.now();
    }

    /**
     * TODO: Implement SM-2 or Anki algorithm in ReviewService
     * References:
     * - SM-2: https://www.supermemo.com/en/archives1990-2015/english/ol/sm2
     * - Anki: https://faqs.ankiweb.net/what-spaced-repetition-algorithm.html
     */
}
