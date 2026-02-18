package com.alang.repository;

import com.alang.entity.Note;
import com.alang.entity.NoteRelation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NoteRelationRepository extends JpaRepository<NoteRelation, String> {

    List<NoteRelation> findBySourceNote(Note sourceNote);

    /**
     * Find all relations where this note is either source or target.
     */
    @Query("SELECT r FROM NoteRelation r WHERE r.sourceNote = :note OR r.targetNote = :note")
    List<NoteRelation> findAllRelationsForNote(@Param("note") Note note);

    void deleteBySourceNoteOrTargetNote(Note sourceNote, Note targetNote);
}
