package com.alang.service.impl;

import com.alang.dto.note.NoteDto;
import com.alang.dto.note.NoteTagDto;
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
import com.alang.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final NoteRepository noteRepository;
    private final ReviewEventRepository reviewEventRepository;
    private final UserRepository userRepository;
    private final LanguageRepository languageRepository;

    @Override
    public ReviewQueueResponse getReviewQueue(String userId, String language, int limit) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        LocalDateTime now = LocalDateTime.now();
        PageRequest pageable = PageRequest.of(0, limit);

        List<Note> dueNotes;
        if (language != null) {
            Language lang = languageRepository.findById(language).orElse(null);
            dueNotes = lang != null
                    ? noteRepository.findDueForReviewByLanguage(user, lang, now, pageable)
                    : List.of();
        } else {
            dueNotes = noteRepository.findDueForReview(user, now, pageable);
        }

        long totalNotes = noteRepository.countByUser(user);
        long dueToday = noteRepository.countDueByEndOfDay(user, now.toLocalDate().atTime(23, 59, 59));

        ReviewQueueResponse response = new ReviewQueueResponse();
        response.setDueNotes(dueNotes.stream().map(this::toDto).toList());
        response.setTotalNotes((int) totalNotes);
        response.setDueTodayCount((int) dueToday);
        response.setEstimatedMinutes(dueNotes.size() * 2); // ~2 min per note
        return response;
    }

    @Override
    @Transactional
    public void submitReview(ReviewSubmissionRequest submission, String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        Note note = noteRepository.findByIdAndUser(submission.getNoteId(), user)
                .orElseThrow(() -> new NoteNotFoundException(submission.getNoteId()));

        int quality = submission.getQuality();
        if (quality < 1 || quality > 4) {
            throw new IllegalArgumentException("Quality must be between 1 and 4");
        }

        int previousInterval = note.getIntervalDays();
        double newEaseFactor = updateEaseFactor(note.getEaseFactor(), quality);
        int newInterval = calculateNextInterval(previousInterval, newEaseFactor, quality);

        ReviewEvent event = new ReviewEvent();
        event.setUser(user);
        event.setNote(note);
        event.setQuality(quality);
        event.setTimeSpentSeconds(submission.getTimeSpentSeconds());
        event.setPreviousIntervalDays(previousInterval);
        event.setNextIntervalDays(newInterval);
        event.setEaseFactor(newEaseFactor);
        reviewEventRepository.save(event);

        note.setEaseFactor(newEaseFactor);
        note.setIntervalDays(newInterval);
        note.setNextReviewAt(LocalDate.now().plusDays(newInterval).atStartOfDay());
        note.setLastReviewedAt(LocalDateTime.now());
        note.setReviewCount(note.getReviewCount() + 1);
        noteRepository.save(note);

        log.info("Review submitted: noteId={}, quality={}, prevInterval={}, newInterval={}, userId={}",
                note.getId(), quality, previousInterval, newInterval, userId);
    }

    /**
     * SM-2 interval calculation (adapted for 1-4 quality scale):
     * - 1 (forgot):     reset to 1 day
     * - 2   (hard):     keep current interval
     * - 3   (good):     interval * easeFactor
     * - 4   (easy):     interval * easeFactor * 1.3
     */
    @Override
    public int calculateNextInterval(int currentInterval, double easeFactor, int quality) {
        if (currentInterval == 0) { // First review
            return switch (quality) {
                case 1 -> 0; // Immediate review 
                case 2 -> 0;
                case 3 -> 1;
                case 4 -> 3;
                default -> throw new IllegalArgumentException("Quality must be 1-4");
            };
        }
        return switch (quality) {
            case 1    -> 1;
            case 2    -> Math.max(2, (int) Math.round(currentInterval * easeFactor));
            case 3    -> Math.max(2, (int) Math.round(currentInterval * easeFactor * 1.3));
            case 4    -> Math.max(2, (int) Math.round(currentInterval * easeFactor * 1.6));
            default   -> throw new IllegalArgumentException("Quality must be 1-4");
        };
    }

    /**
     * SM-2 ease factor update. Clamped to [1.0, 2.5].
     * - 1 (forgot):    -0.15
     * - 2 (hard):      -0.08
     * - 3 (good):       0.00
     * - 4 (perfect):   +0.10
     */
    @Override
    public double updateEaseFactor(double currentEaseFactor, int quality) {
        double delta = switch (quality) {
            case 1  -> -0.15;
            case 2  -> -0.08;
            case 3  ->  0.00;
            case 4  ->  0.10;
            default -> throw new IllegalArgumentException("Quality must be 1-4");
        };
        return Math.min(2.5, Math.max(1.0, currentEaseFactor + delta));
    }

    @Override
    public ReviewStats getReviewStats(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = now.toLocalDate().atTime(23, 59, 59);

        int totalNotes = (int) noteRepository.countByUser(user);
        int reviewedToday = (int) reviewEventRepository.countByUserAndReviewedAtBetween(user, startOfDay, now);
        int dueToday = (int) noteRepository.countDueByEndOfDay(user, endOfDay);

        Double avgQuality = reviewEventRepository.getAverageQuality(user);
        double averageRetention = avgQuality != null ? (avgQuality / 4.0) * 100.0 : 0.0;

        int streakDays = calculateStreak(user, now);

        return new ReviewStats(totalNotes, reviewedToday, dueToday, averageRetention, streakDays);
    }

    // Count consecutive days with at least one review, going backwards from today.
    // If user hasn't reviewed today, we start counting from yesterday so a streak
    // isn't broken mid-day.
    private int calculateStreak(User user, LocalDateTime now) {
        LocalDate date = now.toLocalDate();

        boolean reviewedToday = reviewEventRepository.countByUserAndReviewedAtBetween(
                user, date.atStartOfDay(), date.atTime(23, 59, 59)) > 0;

        if (!reviewedToday) {
            date = date.minusDays(1);
        }

        int streak = 0;
        while (true) {
            long count = reviewEventRepository.countByUserAndReviewedAtBetween(
                    user, date.atStartOfDay(), date.atTime(23, 59, 59));
            if (count == 0) break;
            streak++;
            date = date.minusDays(1);
        }
        return streak;
    }

    private NoteDto toDto(Note note) {
        NoteDto dto = new NoteDto();
        dto.setId(note.getId());
        dto.setType(note.getType());
        dto.setTeachingLanguage(note.getTeachingLanguage().getCode());
        dto.setLearningLanguage(note.getLearningLanguage().getCode());
        dto.setTitle(note.getTitle());
        dto.setSummary(note.getSummary());
        dto.setNoteContent(note.getNoteContent());
        dto.setStructuredContent(note.getStructuredContent());
        dto.setUserEdited(note.getUserEdited());
        dto.setReviewCount(note.getReviewCount());
        dto.setLastReviewedAt(note.getLastReviewedAt());
        dto.setNextReviewAt(note.getNextReviewAt());
        dto.setCreatedAt(note.getCreatedAt());
        dto.setUpdatedAt(note.getUpdatedAt());
        if (note.getTags() != null) {
            dto.setTags(note.getTags().stream()
                    .map(t -> new NoteTagDto(t.getTagCategory(), t.getTagValue()))
                    .toList());
        }
        return dto;
    }
}
