package com.alang.controller;

import com.alang.dto.review.ReviewQueueResponse;
import com.alang.dto.review.ReviewSubmissionRequest;
import com.alang.service.ReviewService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ReviewControllerTest {

    private final ReviewService reviewService = mock(ReviewService.class);
    private final ReviewController reviewController = new ReviewController(reviewService);

    @Test
    void getReviewQueue_returns200WithResponse() {
        ReviewQueueResponse response = new ReviewQueueResponse();
        response.setDueNotes(List.of());
        response.setTotalNotes(5);
        response.setDueTodayCount(2);

        when(reviewService.getReviewQueue("user-1", "ja", 20)).thenReturn(response);

        ResponseEntity<ReviewQueueResponse> result = reviewController.getReviewQueue("ja", 20, "user-1");

        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody()).isEqualTo(response);
        verify(reviewService).getReviewQueue("user-1", "ja", 20);
    }

    @Test
    void submitReview_returns204() {
        ReviewSubmissionRequest request = new ReviewSubmissionRequest();
        request.setNoteId("note-1");
        request.setQuality(4);

        ResponseEntity<Void> result = reviewController.submitReview(request, "user-1");

        assertThat(result.getStatusCode().value()).isEqualTo(204);
        verify(reviewService).submitReview(request, "user-1");
    }
}
