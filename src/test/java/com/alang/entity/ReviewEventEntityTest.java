package com.alang.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ReviewEventEntityTest {

    @Test
    void onCreate_setsReviewedAt() {
        ReviewEvent event = new ReviewEvent();
        LocalDateTime before = LocalDateTime.now();

        event.onCreate();

        LocalDateTime after = LocalDateTime.now();
        assertThat(event.getReviewedAt()).isNotNull().isAfterOrEqualTo(before).isBeforeOrEqualTo(after);
    }
}
