package com.alang.controller;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReviewControllerTest {

    private final ReviewController reviewController = new ReviewController();

    @Test
    void getReviewQueue_throwsUnsupportedOperation() {
        assertThatThrownBy(() -> reviewController.getReviewQueue("ja", 20, "user-1"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void submitReview_throwsUnsupportedOperation() {
        assertThatThrownBy(() -> reviewController.submitReview(null, "user-1"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
