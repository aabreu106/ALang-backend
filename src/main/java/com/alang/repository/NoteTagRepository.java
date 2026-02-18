package com.alang.repository;

import com.alang.entity.Note;
import com.alang.entity.NoteTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NoteTagRepository extends JpaRepository<NoteTag, String> {

    List<NoteTag> findByNote(Note note);

    void deleteByNote(Note note);

    /**
     * Find all distinct tag values for a given category across a user's notes.
     * Useful for populating filter dropdowns on the frontend.
     */
    @Query("SELECT DISTINCT t.tagValue FROM NoteTag t WHERE t.note.user.id = :userId AND t.tagCategory = :category ORDER BY t.tagValue")
    List<String> findDistinctTagValuesByUserAndCategory(@Param("userId") String userId, @Param("category") String category);
}
