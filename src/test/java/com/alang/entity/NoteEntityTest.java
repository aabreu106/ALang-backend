package com.alang.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class NoteEntityTest {

    // ---- @PrePersist (onCreate) ----

    @Test
    void onCreate_setsTimestamps() {
        Note note = new Note();
        LocalDateTime before = LocalDateTime.now();

        note.onCreate();

        LocalDateTime after = LocalDateTime.now();
        assertThat(note.getCreatedAt()).isNotNull().isAfterOrEqualTo(before).isBeforeOrEqualTo(after);
        assertThat(note.getUpdatedAt()).isNotNull().isAfterOrEqualTo(before).isBeforeOrEqualTo(after);
    }

    @Test
    void onCreate_setsNextReviewAtApproximatelyOneDayFromNow_whenNull() {
        Note note = new Note();
        LocalDateTime before = LocalDateTime.now().plusDays(1);

        note.onCreate();

        LocalDateTime after = LocalDateTime.now().plusDays(1);
        assertThat(note.getNextReviewAt()).isNotNull().isAfterOrEqualTo(before).isBeforeOrEqualTo(after);
    }

    @Test
    void onCreate_doesNotOverrideExistingNextReviewAt() {
        Note note = new Note();
        LocalDateTime customDate = LocalDateTime.now().plusDays(7);
        note.setNextReviewAt(customDate);

        note.onCreate();

        assertThat(note.getNextReviewAt()).isEqualTo(customDate);
    }

    // ---- @PreUpdate (onUpdate) ----

    @Test
    void onUpdate_refreshesUpdatedAt_andDoesNotTouchCreatedAt() {
        Note note = new Note();
        note.onCreate();
        LocalDateTime createdAt = note.getCreatedAt();
        LocalDateTime originalUpdatedAt = note.getUpdatedAt();

        note.onUpdate();

        assertThat(note.getUpdatedAt()).isNotNull().isAfterOrEqualTo(originalUpdatedAt);
        assertThat(note.getCreatedAt()).isEqualTo(createdAt);
    }

    // ---- Field defaults ----

    @Test
    void defaultUserEdited_isFalse() {
        Note note = new Note();
        assertThat(note.getUserEdited()).isFalse();
    }

    @Test
    void defaultReviewCount_isZero() {
        Note note = new Note();
        assertThat(note.getReviewCount()).isEqualTo(0);
    }

    @Test
    void defaultEaseFactor_is2Point5() {
        Note note = new Note();
        assertThat(note.getEaseFactor()).isEqualTo(2.5);
    }

    @Test
    void defaultIntervalDays_is1() {
        Note note = new Note();
        assertThat(note.getIntervalDays()).isEqualTo(1);
    }

    @Test
    void defaultTags_isEmptyList() {
        Note note = new Note();
        assertThat(note.getTags()).isNotNull().isEmpty();
    }
}
