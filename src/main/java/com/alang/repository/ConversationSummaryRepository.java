package com.alang.repository;

import com.alang.entity.ConversationSummary;
import com.alang.entity.User;
import com.alang.entity.Language;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Conversation summary repository.
 *
 * ARCHITECTURAL NOTE:
 * Summaries are the solution to unbounded chat history.
 * They are created periodically and old messages are deleted.
 *
 * TODO: Add index on (userId, language, createdAt) for efficient querying
 */
@Repository
public interface ConversationSummaryRepository extends JpaRepository<ConversationSummary, String> {

    /**
     * Get recent summaries for a user in a specific learning language.
     * Ordered by creation time (newest first).
     *
     * Used when assembling context for LLM.
     */
    List<ConversationSummary> findByUserAndLearningLanguageOrderByCreatedAtDesc(
        User user,
        Language learningLanguage,
        Pageable pageable
    );

    /**
     * Get all summaries for a user and learning language (for history view).
     */
    List<ConversationSummary> findByUserAndLearningLanguageOrderByCreatedAtDesc(User user, Language learningLanguage);

    /**
     * Count summaries for analytics.
     */
    long countByUser(User user);

    // TODO: Add method to find summaries by topic (for semantic search)
    // TODO: Add cleanup method for very old summaries (optional)
}
