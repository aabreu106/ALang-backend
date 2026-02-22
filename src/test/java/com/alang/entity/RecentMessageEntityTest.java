package com.alang.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class RecentMessageEntityTest {

    // ---- @PrePersist (onCreate) ----

    @Test
    void onCreate_setsCreatedAt() {
        RecentMessage message = new RecentMessage();
        LocalDateTime before = LocalDateTime.now();

        message.onCreate();

        LocalDateTime after = LocalDateTime.now();
        assertThat(message.getCreatedAt()).isNotNull().isAfterOrEqualTo(before).isBeforeOrEqualTo(after);
    }

    @Test
    void onCreate_setsDefaultExpiresAtTo48HoursFromNow_whenNull() {
        RecentMessage message = new RecentMessage();
        LocalDateTime before = LocalDateTime.now().plusHours(48);

        message.onCreate();

        LocalDateTime after = LocalDateTime.now().plusHours(48);
        assertThat(message.getExpiresAt()).isNotNull().isAfterOrEqualTo(before).isBeforeOrEqualTo(after);
    }

    @Test
    void onCreate_doesNotOverrideExistingExpiresAt() {
        RecentMessage message = new RecentMessage();
        LocalDateTime customExpiry = LocalDateTime.now().plusHours(1);
        message.setExpiresAt(customExpiry);

        message.onCreate();

        assertThat(message.getExpiresAt()).isEqualTo(customExpiry);
    }

    @Test
    void onCreate_expiresAtIsApproximately48HoursAfterCreatedAt() {
        RecentMessage message = new RecentMessage();
        message.onCreate();

        // expiresAt should be ~48h after createdAt
        assertThat(message.getExpiresAt()).isAfterOrEqualTo(message.getCreatedAt().plusHours(47));
        assertThat(message.getExpiresAt()).isBeforeOrEqualTo(message.getCreatedAt().plusHours(49));
    }
}
