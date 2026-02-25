package com.alang.controller;

import com.alang.dto.review.ReviewQueueResponse;
import com.alang.dto.review.ReviewSubmissionRequest;
import com.alang.service.ReviewService;
import com.alang.service.ReviewService.ReviewStats;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Spaced repetition review controller.
 *
 * ARCHITECTURAL NOTE:
 * This is the "Anki-lite" functionality.
 * ReviewService handles all the spaced repetition algorithm logic.
 *
 * This controller is THIN:
 * - Just calls ReviewService
 * - Returns HTTP responses
 */
@RestController
@RequestMapping("/review")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    /**
     * GET /review/queue
     * Get notes due for review.
     *
     * Optional language filter to review only specific language.
     */
    @GetMapping("/queue")
    public ResponseEntity<ReviewQueueResponse> getReviewQueue(
        @RequestParam(required = false) String language,
        @RequestParam(defaultValue = "20") int limit,
        @AuthenticationPrincipal String userId
    ) {
        return ResponseEntity.ok(reviewService.getReviewQueue(userId, language, limit));
    }

    /**
     * POST /review/reviewed
     * Submit a review result.
     *
     * User has reviewed a note (rated quality 1-4).
     * Backend updates next review schedule using SM-2 algorithm.
     */
    @PostMapping("/reviewed")
    public ResponseEntity<Void> submitReview(
        @Valid @RequestBody ReviewSubmissionRequest request,
        @AuthenticationPrincipal String userId
    ) {
        reviewService.submitReview(request, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /review/stats
     * Get review statistics for the authenticated user.
     */
    @GetMapping("/stats")
    public ResponseEntity<ReviewStats> getReviewStats(
        @AuthenticationPrincipal String userId
    ) {
        return ResponseEntity.ok(reviewService.getReviewStats(userId));
    }
}
