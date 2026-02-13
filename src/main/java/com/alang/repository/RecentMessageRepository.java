package com.alang.repository;

import com.alang.entity.RecentMessage;
import com.alang.entity.User;
import com.alang.entity.Language;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Recent message repository.
 *
 * ARCHITECTURAL NOTE:
 * These are TEMPORARY messages with TTL.
 * They should be deleted after summarization.
 *
 * TODO: Implement cleanup job to delete expired messages
 * TODO: Add index on expiresAt for efficient cleanup
 */
@Repository
public interface RecentMessageRepository extends JpaRepository<RecentMessage, String> {

    /**
     * Get recent messages for a user in a specific learning language.
     * Ordered by creation time (oldest first for context assembly).
     */
    List<RecentMessage> findByUserAndLearningLanguageOrderByCreatedAtAsc(
        User user,
        Language learningLanguage,
        Pageable pageable
    );

    /**
     * Get recent messages for a user and learning language (all, not paginated).
     * Used when generating summaries.
     */
    List<RecentMessage> findByUserAndLearningLanguageOrderByCreatedAtAsc(User user, Language learningLanguage);

    /**
     * Delete expired messages (for TTL cleanup).
     * Call this from a scheduled job.
     *
     * @Modifying indicates this is a write operation
     */
    @Modifying
    @Query("DELETE FROM RecentMessage m WHERE m.expiresAt < :now")
    void deleteExpiredMessages(LocalDateTime now);

    /**
     * Delete messages for a user and learning language (after summarization).
     */
    @Modifying
    void deleteByUserAndLearningLanguage(User user, Language learningLanguage);

    /**
     * Delete messages created before a certain time (for cleanup after summarization).
     */
    @Modifying
    void deleteByUserAndLearningLanguageAndCreatedAtBefore(User user, Language learningLanguage, LocalDateTime before);

    /**
     * Count messages for a user and learning language (to check summarization threshold).
     */
    long countByUserAndLearningLanguage(User user, Language learningLanguage);
}
