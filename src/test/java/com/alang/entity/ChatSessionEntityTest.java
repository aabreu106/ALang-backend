package com.alang.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ChatSessionEntityTest {

    // ---- @PrePersist (onCreate) ----

    @Test
    void onCreate_setsCreatedAtAndUpdatedAt() {
        ChatSession session = new ChatSession();
        LocalDateTime before = LocalDateTime.now();

        session.onCreate();

        LocalDateTime after = LocalDateTime.now();
        assertThat(session.getCreatedAt()).isNotNull().isAfterOrEqualTo(before).isBeforeOrEqualTo(after);
        assertThat(session.getUpdatedAt()).isNotNull().isAfterOrEqualTo(before).isBeforeOrEqualTo(after);
    }

    // ---- @PreUpdate (onUpdate) ----

    @Test
    void onUpdate_refreshesUpdatedAt_andDoesNotTouchCreatedAt() {
        ChatSession session = new ChatSession();
        session.onCreate();
        LocalDateTime createdAt = session.getCreatedAt();
        LocalDateTime originalUpdatedAt = session.getUpdatedAt();

        session.onUpdate();

        assertThat(session.getUpdatedAt()).isNotNull().isAfterOrEqualTo(originalUpdatedAt);
        assertThat(session.getCreatedAt()).isEqualTo(createdAt);
    }

    // ---- Field defaults ----

    @Test
    void defaultStatus_isActive() {
        ChatSession session = new ChatSession();
        assertThat(session.getStatus()).isEqualTo(SessionStatus.active);
    }

    @Test
    void closedAt_isNullByDefault() {
        ChatSession session = new ChatSession();
        assertThat(session.getClosedAt()).isNull();
    }
}
