package com.alang.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ConversationSummaryEntityTest {

    @Test
    void onCreate_setsCreatedAt() {
        ConversationSummary summary = new ConversationSummary();
        LocalDateTime before = LocalDateTime.now();

        summary.onCreate();

        LocalDateTime after = LocalDateTime.now();
        assertThat(summary.getCreatedAt()).isNotNull().isAfterOrEqualTo(before).isBeforeOrEqualTo(after);
    }
}
