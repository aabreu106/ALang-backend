package com.alang.repository;

import com.alang.entity.ChatSession;
import com.alang.entity.Language;
import com.alang.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, String> {

    /**
     * List sessions for a user filtered by learning language, newest first.
     * Used to populate the session picker on the frontend.
     */
    List<ChatSession> findByUserAndLearningLanguageOrderByCreatedAtDesc(
            User user, Language learningLanguage, Pageable pageable);

    /**
     * List all sessions for a user across all languages, newest first.
     */
    List<ChatSession> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    /**
     * Find a session by ID and verify it belongs to the given user.
     * Used as an authorization check before any session operation.
     */
    Optional<ChatSession> findByIdAndUser(String id, User user);
}
