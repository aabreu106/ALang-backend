package com.alang.service.impl;

import com.alang.dto.review.ReviewQueueResponse;
import com.alang.dto.review.ReviewSubmissionRequest;
import com.alang.entity.Language;
import com.alang.entity.Note;
import com.alang.entity.ReviewEvent;
import com.alang.entity.User;
import com.alang.exception.NoteNotFoundException;
import com.alang.exception.UserNotFoundException;
import com.alang.repository.LanguageRepository;
import com.alang.repository.NoteRepository;
import com.alang.repository.ReviewEventRepository;
import com.alang.repository.UserRepository;
import com.alang.service.ReviewService.ReviewStats;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ReviewServiceImplTest {

    private final NoteRepository noteRepository = mock(NoteRepository.class);
    private final ReviewEventRepository reviewEventRepository = mock(ReviewEventRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final LanguageRepository languageRepository = mock(LanguageRepository.class);

    private final ReviewServiceImpl service = new ReviewServiceImpl(
            noteRepository, reviewEventRepository, userRepository, languageRepository);

    private User user;
    private Language language;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId("user-1");
        user.setEmail("test@example.com");
        user.setDisplayName("Test User");
        user.setPasswordHash("hash");
        user.setAppLanguageCode("en");

        language = new Language();
        language.setCode("ja");
    }

    // -------------------------------------------------------------------------
    // calculateNextInterval
    // -------------------------------------------------------------------------

    @Nested
    class CalculateNextInterval {

        @Test
        void firstReview_quality1_returnsZero() {
            assertThat(service.calculateNextInterval(0, 2.5, 1)).isEqualTo(0);
        }

        @Test
        void firstReview_quality2_returnsZero() {
            assertThat(service.calculateNextInterval(0, 2.5, 2)).isEqualTo(0);
        }

        @Test
        void firstReview_quality3_returnsOne() {
            assertThat(service.calculateNextInterval(0, 2.5, 3)).isEqualTo(1);
        }

        @Test
        void firstReview_quality4_returnsThree() {
            assertThat(service.calculateNextInterval(0, 2.5, 4)).isEqualTo(3);
        }

        @Test
        void subsequentReview_quality1_alwaysResetsToOne() {
            assertThat(service.calculateNextInterval(10, 2.5, 1)).isEqualTo(1);
        }

        @Test
        void subsequentReview_quality2_usesEaseFactor() {
            // round(5 * 2.5) = 13, max(2, 13) = 13
            assertThat(service.calculateNextInterval(5, 2.5, 2)).isEqualTo(13);
        }

        @Test
        void subsequentReview_quality2_clampsToMinimumTwo() {
            // round(1 * 1.0) = 1, max(2, 1) = 2
            assertThat(service.calculateNextInterval(1, 1.0, 2)).isEqualTo(2);
        }

        @Test
        void subsequentReview_quality3_usesEaseFactor() {
            // round(5 * 2.5) = 13, max(2, 13) = 13
            assertThat(service.calculateNextInterval(5, 2.5, 3)).isEqualTo(13);
        }

        @Test
        void subsequentReview_quality3_clampsToMinimumTwo() {
            assertThat(service.calculateNextInterval(1, 1.0, 3)).isEqualTo(2);
        }

        @Test
        void subsequentReview_quality4_appliesEasyBonus() {
            // round(5 * 2.5 * 1.3) = round(16.25) = 16, max(2, 16) = 16
            assertThat(service.calculateNextInterval(5, 2.5, 4)).isEqualTo(16);
        }

        @Test
        void subsequentReview_quality4_clampsToMinimumTwo() {
            assertThat(service.calculateNextInterval(1, 1.0, 4)).isEqualTo(2);
        }

        @Test
        void invalidQuality_throwsIllegalArgumentException() {
            assertThatThrownBy(() -> service.calculateNextInterval(5, 2.5, 5))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // -------------------------------------------------------------------------
    // updateEaseFactor
    // -------------------------------------------------------------------------

    @Nested
    class UpdateEaseFactor {

        @Test
        void quality1_decreasesByPointFifteen() {
            assertThat(service.updateEaseFactor(2.5, 1)).isCloseTo(2.35, within(0.001));
        }

        @Test
        void quality2_decreasesByPointZeroEight() {
            assertThat(service.updateEaseFactor(2.5, 2)).isCloseTo(2.42, within(0.001));
        }

        @Test
        void quality3_doesNotChange() {
            assertThat(service.updateEaseFactor(2.0, 3)).isCloseTo(2.0, within(0.001));
        }

        @Test
        void quality4_increasesByPointTen() {
            assertThat(service.updateEaseFactor(2.0, 4)).isCloseTo(2.1, within(0.001));
        }

        @Test
        void clampedToMaximumTwoPointFive() {
            assertThat(service.updateEaseFactor(2.5, 4)).isCloseTo(2.5, within(0.001));
        }

        @Test
        void clampedToMinimumOne() {
            assertThat(service.updateEaseFactor(1.0, 1)).isCloseTo(1.0, within(0.001));
        }

        @Test
        void invalidQuality_throwsIllegalArgumentException() {
            assertThatThrownBy(() -> service.updateEaseFactor(2.5, 0))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // -------------------------------------------------------------------------
    // getReviewQueue
    // -------------------------------------------------------------------------

    @Nested
    class GetReviewQueue {

        @Test
        void userNotFound_throwsUserNotFoundException() {
            when(userRepository.findById("missing")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getReviewQueue("missing", null, 20))
                    .isInstanceOf(UserNotFoundException.class);
        }

        @Test
        void noLanguageFilter_returnsDueNotes() {
            Note note = buildNote("note-1", 5, 2.5);
            when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
            when(noteRepository.findDueForReview(eq(user), any(), any())).thenReturn(List.of(note));
            when(noteRepository.countByUser(user)).thenReturn(10L);
            when(noteRepository.countDueByEndOfDay(eq(user), any())).thenReturn(3L);

            ReviewQueueResponse response = service.getReviewQueue("user-1", null, 20);

            assertThat(response.getDueNotes()).hasSize(1);
            assertThat(response.getTotalNotes()).isEqualTo(10);
            assertThat(response.getDueTodayCount()).isEqualTo(3);
            assertThat(response.getEstimatedMinutes()).isEqualTo(2); // 1 note * 2 min
        }

        @Test
        void withLanguageFilter_languageExists_returnsFilteredNotes() {
            Note note = buildNote("note-1", 5, 2.5);
            when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
            when(languageRepository.findById("ja")).thenReturn(Optional.of(language));
            when(noteRepository.findDueForReviewByLanguage(eq(user), eq(language), any(), any()))
                    .thenReturn(List.of(note));
            when(noteRepository.countByUser(user)).thenReturn(5L);
            when(noteRepository.countDueByEndOfDay(eq(user), any())).thenReturn(1L);

            ReviewQueueResponse response = service.getReviewQueue("user-1", "ja", 10);

            assertThat(response.getDueNotes()).hasSize(1);
            verify(noteRepository).findDueForReviewByLanguage(eq(user), eq(language), any(), any());
            verify(noteRepository, never()).findDueForReview(any(), any(), any());
        }

        @Test
        void withLanguageFilter_languageNotFound_returnsEmptyList() {
            when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
            when(languageRepository.findById("xx")).thenReturn(Optional.empty());
            when(noteRepository.countByUser(user)).thenReturn(5L);
            when(noteRepository.countDueByEndOfDay(eq(user), any())).thenReturn(0L);

            ReviewQueueResponse response = service.getReviewQueue("user-1", "xx", 20);

            assertThat(response.getDueNotes()).isEmpty();
            assertThat(response.getEstimatedMinutes()).isEqualTo(0);
        }

        @Test
        void estimatedMinutesIsNoteDurationTimesTwo() {
            List<Note> notes = List.of(buildNote("n1", 1, 2.5), buildNote("n2", 1, 2.5), buildNote("n3", 1, 2.5));
            when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
            when(noteRepository.findDueForReview(eq(user), any(), any())).thenReturn(notes);
            when(noteRepository.countByUser(user)).thenReturn(3L);
            when(noteRepository.countDueByEndOfDay(eq(user), any())).thenReturn(3L);

            ReviewQueueResponse response = service.getReviewQueue("user-1", null, 20);

            assertThat(response.getEstimatedMinutes()).isEqualTo(6);
        }
    }

    // -------------------------------------------------------------------------
    // submitReview
    // -------------------------------------------------------------------------

    @Nested
    class SubmitReview {

        @Test
        void userNotFound_throwsUserNotFoundException() {
            when(userRepository.findById("missing")).thenReturn(Optional.empty());

            ReviewSubmissionRequest req = buildRequest("note-1", 3, 30);
            assertThatThrownBy(() -> service.submitReview(req, "missing"))
                    .isInstanceOf(UserNotFoundException.class);
        }

        @Test
        void noteNotFound_throwsNoteNotFoundException() {
            when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
            when(noteRepository.findByIdAndUser("missing-note", user)).thenReturn(Optional.empty());

            ReviewSubmissionRequest req = buildRequest("missing-note", 3, 30);
            assertThatThrownBy(() -> service.submitReview(req, "user-1"))
                    .isInstanceOf(NoteNotFoundException.class);
        }

        @Test
        void invalidQualityZero_throwsIllegalArgumentException() {
            when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
            Note note = buildNote("note-1", 5, 2.5);
            when(noteRepository.findByIdAndUser("note-1", user)).thenReturn(Optional.of(note));

            ReviewSubmissionRequest req = buildRequest("note-1", 0, 30);
            assertThatThrownBy(() -> service.submitReview(req, "user-1"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Quality must be between 1 and 4");
        }

        @Test
        void invalidQualityFive_throwsIllegalArgumentException() {
            when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
            Note note = buildNote("note-1", 5, 2.5);
            when(noteRepository.findByIdAndUser("note-1", user)).thenReturn(Optional.of(note));

            ReviewSubmissionRequest req = buildRequest("note-1", 5, 30);
            assertThatThrownBy(() -> service.submitReview(req, "user-1"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void validSubmission_savesReviewEvent() {
            when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
            Note note = buildNote("note-1", 5, 2.5);
            when(noteRepository.findByIdAndUser("note-1", user)).thenReturn(Optional.of(note));

            service.submitReview(buildRequest("note-1", 3, 45), "user-1");

            ArgumentCaptor<ReviewEvent> eventCaptor = ArgumentCaptor.forClass(ReviewEvent.class);
            verify(reviewEventRepository).save(eventCaptor.capture());
            ReviewEvent saved = eventCaptor.getValue();
            assertThat(saved.getQuality()).isEqualTo(3);
            assertThat(saved.getTimeSpentSeconds()).isEqualTo(45);
            assertThat(saved.getPreviousIntervalDays()).isEqualTo(5);
            assertThat(saved.getUser()).isEqualTo(user);
            assertThat(saved.getNote()).isEqualTo(note);
        }

        @Test
        void validSubmission_updatesNoteInterval() {
            when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
            Note note = buildNote("note-1", 5, 2.5);
            int expectedInterval = service.calculateNextInterval(5, 2.5, 3);
            when(noteRepository.findByIdAndUser("note-1", user)).thenReturn(Optional.of(note));

            service.submitReview(buildRequest("note-1", 3, 30), "user-1");

            ArgumentCaptor<Note> noteCaptor = ArgumentCaptor.forClass(Note.class);
            verify(noteRepository).save(noteCaptor.capture());
            Note saved = noteCaptor.getValue();
            assertThat(saved.getIntervalDays()).isEqualTo(expectedInterval);
            assertThat(saved.getReviewCount()).isEqualTo(1);
        }

        @Test
        void validSubmission_updatesEaseFactor() {
            when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
            Note note = buildNote("note-1", 5, 2.5);
            double expectedEase = service.updateEaseFactor(2.5, 4);
            when(noteRepository.findByIdAndUser("note-1", user)).thenReturn(Optional.of(note));

            service.submitReview(buildRequest("note-1", 4, 20), "user-1");

            ArgumentCaptor<Note> noteCaptor = ArgumentCaptor.forClass(Note.class);
            verify(noteRepository).save(noteCaptor.capture());
            assertThat(noteCaptor.getValue().getEaseFactor()).isCloseTo(expectedEase, within(0.001));
        }

        @Test
        void validSubmission_setsNextReviewAtInFuture() {
            when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
            Note note = buildNote("note-1", 5, 2.5);
            when(noteRepository.findByIdAndUser("note-1", user)).thenReturn(Optional.of(note));

            LocalDateTime before = LocalDateTime.now();
            service.submitReview(buildRequest("note-1", 3, 30), "user-1");

            ArgumentCaptor<Note> noteCaptor = ArgumentCaptor.forClass(Note.class);
            verify(noteRepository).save(noteCaptor.capture());
            assertThat(noteCaptor.getValue().getNextReviewAt()).isAfter(before);
            assertThat(noteCaptor.getValue().getLastReviewedAt()).isAfter(before.minusSeconds(1));
        }

        @Test
        void qualityOne_resetsIntervalToOne() {
            when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
            Note note = buildNote("note-1", 30, 2.5); // long current interval
            when(noteRepository.findByIdAndUser("note-1", user)).thenReturn(Optional.of(note));

            service.submitReview(buildRequest("note-1", 1, 10), "user-1");

            ArgumentCaptor<Note> noteCaptor = ArgumentCaptor.forClass(Note.class);
            verify(noteRepository).save(noteCaptor.capture());
            assertThat(noteCaptor.getValue().getIntervalDays()).isEqualTo(1);
        }
    }

    // -------------------------------------------------------------------------
    // getReviewStats
    // -------------------------------------------------------------------------

    @Nested
    class GetReviewStats {

        @Test
        void userNotFound_throwsUserNotFoundException() {
            when(userRepository.findById("missing")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getReviewStats("missing"))
                    .isInstanceOf(UserNotFoundException.class);
        }

        @Test
        void returnsCorrectTotals() {
            when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
            when(noteRepository.countByUser(user)).thenReturn(15L);
            when(reviewEventRepository.countByUserAndReviewedAtBetween(eq(user), any(), any())).thenReturn(5L);
            when(noteRepository.countDueByEndOfDay(eq(user), any())).thenReturn(3L);
            when(reviewEventRepository.getAverageQuality(user)).thenReturn(4.0);

            ReviewStats stats = service.getReviewStats("user-1");

            assertThat(stats.getTotalNotes()).isEqualTo(15);
            assertThat(stats.getReviewedToday()).isEqualTo(5);
            assertThat(stats.getDueToday()).isEqualTo(3);
        }

        @Test
        void averageRetentionCalculatedFromAverageQuality() {
            when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
            when(noteRepository.countByUser(user)).thenReturn(10L);
            when(reviewEventRepository.countByUserAndReviewedAtBetween(eq(user), any(), any())).thenReturn(0L);
            when(noteRepository.countDueByEndOfDay(eq(user), any())).thenReturn(0L);
            when(reviewEventRepository.getAverageQuality(user)).thenReturn(4.0);

            ReviewStats stats = service.getReviewStats("user-1");

            // (4.0 / 5.0) * 100 = 80.0
            assertThat(stats.getAverageRetention()).isCloseTo(80.0, within(0.01));
        }

        @Test
        void averageRetentionIsZeroWhenNoReviews() {
            when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
            when(noteRepository.countByUser(user)).thenReturn(5L);
            when(reviewEventRepository.countByUserAndReviewedAtBetween(eq(user), any(), any())).thenReturn(0L);
            when(noteRepository.countDueByEndOfDay(eq(user), any())).thenReturn(0L);
            when(reviewEventRepository.getAverageQuality(user)).thenReturn(null);

            ReviewStats stats = service.getReviewStats("user-1");

            assertThat(stats.getAverageRetention()).isCloseTo(0.0, within(0.01));
        }

        @Test
        void streakIsZeroWhenNeverReviewed() {
            when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
            when(noteRepository.countByUser(user)).thenReturn(5L);
            when(noteRepository.countDueByEndOfDay(eq(user), any())).thenReturn(0L);
            when(reviewEventRepository.getAverageQuality(user)).thenReturn(null);
            // All date window queries return 0
            when(reviewEventRepository.countByUserAndReviewedAtBetween(eq(user), any(), any())).thenReturn(0L);

            ReviewStats stats = service.getReviewStats("user-1");

            assertThat(stats.getStreakDays()).isEqualTo(0);
        }

        @Test
        void streakCountsConsecutiveDaysWithReviewsToday() {
            when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
            when(noteRepository.countByUser(user)).thenReturn(5L);
            when(noteRepository.countDueByEndOfDay(eq(user), any())).thenReturn(0L);
            when(reviewEventRepository.getAverageQuality(user)).thenReturn(3.0);

            // countByUserAndReviewedAtBetween is called many times for streak calculation.
            // Simulate reviews today and 2 days prior, but not 3 days ago.
            // We return 1 for the first 3 calls (today, yesterday, day before), then 0.
            when(reviewEventRepository.countByUserAndReviewedAtBetween(eq(user), any(), any()))
                    .thenReturn(1L, 1L, 1L, 0L);

            ReviewStats stats = service.getReviewStats("user-1");

            assertThat(stats.getStreakDays()).isEqualTo(3);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Note buildNote(String id, int intervalDays, double easeFactor) {
        Language teaching = new Language();
        teaching.setCode("en");

        Note note = new Note();
        note.setId(id);
        note.setUser(user);
        note.setTitle("Test Note");
        note.setSummary("Summary");
        note.setIntervalDays(intervalDays);
        note.setEaseFactor(easeFactor);
        note.setReviewCount(0);
        note.setTeachingLanguage(teaching);
        note.setLearningLanguage(language);
        return note;
    }

    private ReviewSubmissionRequest buildRequest(String noteId, int quality, int timeSpentSeconds) {
        ReviewSubmissionRequest req = new ReviewSubmissionRequest();
        req.setNoteId(noteId);
        req.setQuality(quality);
        req.setTimeSpentSeconds(timeSpentSeconds);
        return req;
    }
}
