package com.alang.controller;

import com.alang.dto.review.ReviewQueueResponse;
import com.alang.dto.review.ReviewSubmissionRequest;
import com.alang.service.ReviewService;
import jakarta.validation.Valid;
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
 *
 * TODO: Inject ReviewService
 */
@RestController
@RequestMapping("/review")
public class ReviewController {

    // TODO: Inject ReviewService
    // private final ReviewService reviewService;

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
        // TODO: Call reviewService.getReviewQueue(userId, language, limit)
        // TODO: Return 200 OK with ReviewQueueResponse
        throw new UnsupportedOperationException("TODO: Implement get review queue");
    }

    /**
     * POST /notes/reviewed
     * Submit a review result.
     *
     * User has reviewed a note (rated quality 1-5).
     * Backend updates next review schedule using SM-2 algorithm.
     */
    @PostMapping("/reviewed")
    public ResponseEntity<Void> submitReview(
        @Valid @RequestBody ReviewSubmissionRequest request,
        @AuthenticationPrincipal String userId
    ) {
        // TODO: Call reviewService.submitReview(request, userId)
        // TODO: Return 204 No Content
        throw new UnsupportedOperationException("TODO: Implement submit review");
    }
}
