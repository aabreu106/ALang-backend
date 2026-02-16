package com.alang.repository;

import com.alang.entity.Note;
import com.alang.entity.NoteType;
import com.alang.entity.User;
import com.alang.entity.Language;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Note repository.
 *
 * TODO: Implement full-text search (PostgreSQL full-text search or Elasticsearch)
 * TODO: Add indexes on frequently queried fields (userId, language, type, nextReviewAt)
 */
@Repository
public interface NoteRepository extends JpaRepository<Note, String> {

    /**
     * Find all notes for a user.
     */
    Page<Note> findByUser(User user, Pageable pageable);

    /**
     * Find notes by user and learning language.
     */
    Page<Note> findByUserAndLearningLanguage(User user, Language language, Pageable pageable);

    /**
     * Find notes by user, learning language, and type.
     */
    Page<Note> findByUserAndLearningLanguageAndType(User user, Language language, NoteType type, Pageable pageable);

    /**
     * Find note by ID and user (for authorization check).
     */
    Optional<Note> findByIdAndUser(String id, User user);

    /**
     * Count notes for a user.
     */
    long countByUser(User user);

    /**
     * Full-text search in notes.
     * TODO: Implement using PostgreSQL full-text search or @Query with LIKE
     *
     * Example query:
     * @Query("SELECT n FROM Note n WHERE n.user = :user AND " +
     *        "(LOWER(n.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
     *        "LOWER(n.summary) LIKE LOWER(CONCAT('%', :search, '%')))")
     */
    @Query("SELECT n FROM Note n WHERE n.user = :user AND " +
           "(LOWER(n.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(n.summary) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Note> searchNotes(@Param("user") User user, @Param("search") String search, Pageable pageable);

    /**
     * Find notes with matching or similar titles for de-duplication.
     * Uses case-insensitive LIKE match on title.
     */
    @Query("SELECT n FROM Note n WHERE n.user = :user AND n.learningLanguage = :language " +
           "AND LOWER(n.title) = LOWER(:title)")
    List<Note> findByUserAndLearningLanguageAndTitleIgnoreCase(
            @Param("user") User user,
            @Param("language") Language language,
            @Param("title") String title);
}
