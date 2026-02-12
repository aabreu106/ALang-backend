package com.alang.service;

import com.alang.dto.note.NoteDto;
import com.alang.dto.note.NoteListResponse;
import com.alang.dto.note.UpdateNoteRequest;

import java.util.List;

/**
 * Note management service.
 *
 * RESPONSIBILITIES:
 * - Create notes (auto-extracted from chat or manually created)
 * - Update notes (user edits)
 * - Retrieve notes (with filtering, search)
 * - Delete notes
 * - Manage note relationships (related notes)
 *
 * ARCHITECTURAL NOTE:
 * Most notes are created AUTOMATICALLY by ChatService after extracting from LLM responses.
 * Users can manually edit notes afterward.
 */
public interface NoteService {

    /**
     * Create a new note.
     *
     * Usually called by ChatService after extracting notes from LLM response.
     * Can also be called directly if user manually creates a note.
     *
     * @param noteDto Note data
     * @param userId User creating the note
     * @return Created note with ID
     */
    NoteDto createNote(NoteDto noteDto, String userId);

    /**
     * Batch create notes (for efficiency).
     * Called by ChatService when multiple notes are extracted from single LLM response.
     *
     * @param notes List of notes to create
     * @param userId User ID
     * @return Created notes with IDs
     */
    List<NoteDto> createNotes(List<NoteDto> notes, String userId);

    /**
     * Get a single note by ID.
     *
     * @param noteId Note ID
     * @param userId User ID (for authorization check)
     * @return Note details
     * @throws NoteNotFoundException if note doesn't exist
     * @throws UnauthorizedException if note belongs to different user
     */
    NoteDto getNote(String noteId, String userId);

    /**
     * Get all notes for a user with optional filtering.
     *
     * Filters:
     * - language: Filter by language code
     * - type: Filter by note type (vocab, grammar, exception)
     * - minConfidence: Only notes with confidence >= this value
     * - search: Full-text search in title, summary, examples
     *
     * TODO: Implement pagination
     * TODO: Implement sorting (by date, confidence, review status)
     *
     * @param userId User ID
     * @param language Optional language filter
     * @param type Optional type filter
     * @param minConfidence Optional confidence filter
     * @param searchQuery Optional search query
     * @param page Page number (0-indexed)
     * @param pageSize Page size
     * @return Paginated list of notes
     */
    NoteListResponse getNotes(
        String userId,
        String language,
        String type,
        Double minConfidence,
        String searchQuery,
        int page,
        int pageSize
    );

    /**
     * Update a note.
     * User can edit auto-generated notes to improve accuracy.
     *
     * When user edits a note, set userEdited=true to prevent auto-updates.
     *
     * @param noteId Note ID
     * @param updateRequest Update data
     * @param userId User ID (for authorization)
     * @return Updated note
     */
    NoteDto updateNote(String noteId, UpdateNoteRequest updateRequest, String userId);

    /**
     * Delete a note.
     *
     * @param noteId Note ID
     * @param userId User ID (for authorization)
     */
    void deleteNote(String noteId, String userId);

    /**
     * Find similar notes (for de-duplication and relationships).
     *
     * When extracting notes from LLM, check if similar note already exists.
     * If yes, update existing note instead of creating duplicate.
     *
     * Similarity criteria:
     * - Same language and type
     * - Title similarity (fuzzy match)
     * - Concept similarity (TODO: semantic embedding search)
     *
     * TODO: Implement fuzzy matching or embedding-based similarity
     *
     * @param noteDto Note to check similarity for
     * @param userId User ID
     * @return List of similar existing notes
     */
    List<NoteDto> findSimilarNotes(NoteDto noteDto, String userId);
}
