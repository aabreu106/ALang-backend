package com.alang.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class UserEntityTest {

    // ---- @PrePersist (onCreate) ----

    @Test
    void onCreate_setsCreatedAtAndUpdatedAt() {
        User user = new User();
        LocalDateTime before = LocalDateTime.now();

        user.onCreate();

        LocalDateTime after = LocalDateTime.now();
        assertThat(user.getCreatedAt()).isNotNull().isAfterOrEqualTo(before).isBeforeOrEqualTo(after);
        assertThat(user.getUpdatedAt()).isNotNull().isAfterOrEqualTo(before).isBeforeOrEqualTo(after);
    }

    @Test
    void onCreate_setsBothTimestampsToSameApproximateTime() {
        User user = new User();
        user.onCreate();

        // Both are set in the same method call so they should be equal or within a tiny window
        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(user.getUpdatedAt()).isNotNull();
        assertThat(user.getUpdatedAt()).isAfterOrEqualTo(user.getCreatedAt());
    }

    // ---- @PreUpdate (onUpdate) ----

    @Test
    void onUpdate_refreshesUpdatedAt() {
        User user = new User();
        user.onCreate();
        LocalDateTime originalUpdatedAt = user.getUpdatedAt();

        // A slight delay isn't guaranteed at nanosecond resolution, so just verify
        // onUpdate sets updatedAt to a non-null value and doesn't touch createdAt
        LocalDateTime createdAt = user.getCreatedAt();
        user.onUpdate();

        assertThat(user.getUpdatedAt()).isNotNull().isAfterOrEqualTo(originalUpdatedAt);
        assertThat(user.getCreatedAt()).isEqualTo(createdAt); // createdAt must not change
    }

    // ---- Field defaults ----

    @Test
    void defaultTier_isFree() {
        User user = new User();
        assertThat(user.getTier()).isEqualTo(UserTier.free);
    }

    @Test
    void defaultTotalDailyTokensUsed_isZero() {
        User user = new User();
        assertThat(user.getTotalDailyTokensUsed()).isEqualTo(0L);
    }

    @Test
    void defaultTargetLanguageCodes_isEmptyList() {
        User user = new User();
        assertThat(user.getTargetLanguageCodes()).isNotNull().isEmpty();
    }
}
