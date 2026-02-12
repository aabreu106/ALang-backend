package com.alang.controller;

import com.alang.dto.note.NoteDto;
import com.alang.dto.note.NoteListResponse;
import com.alang.dto.note.UpdateNoteRequest;
import com.alang.service.NoteService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Note management controller.
 *
 * ARCHITECTURAL NOTE:
 * Notes are primarily AUTO-GENERATED from chat conversations.
 * Users interact with these endpoints to view/edit/review notes.
 *
 * This controller is THIN:
 * - No business logic
 * - Just calls NoteService
 *
 * TODO: Inject NoteService
 */
@RestController
@RequestMapping("/notes")
public class NoteController {

    // TODO: Inject NoteService
    // private final NoteService noteService;

    /**
     * GET /notes
     * Get all notes for authenticated user.
     *
     * Supports filtering by language, type, confidence, search query.
     */
    @GetMapping
    public ResponseEntity<NoteListResponse> getNotes(
        @RequestParam(required = false) String language,
        @RequestParam(required = false) String type,
        @RequestParam(required = false) Double minConfidence,
        @RequestParam(required = false) String search,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int pageSize,
        @AuthenticationPrincipal String userId
    ) {
        // TODO: Call noteService.getNotes(userId, language, type, minConfidence, search, page, pageSize)
        // TODO: Return 200 OK with NoteListResponse
        throw new UnsupportedOperationException("TODO: Implement get notes");
    }

    /**
     * GET /notes/{id}
     * Get a single note by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<NoteDto> getNote(
        @PathVariable String id,
        @AuthenticationPrincipal String userId
    ) {
        // TODO: Call noteService.getNote(id, userId)
        // TODO: Return 200 OK with NoteDto
        // TODO: Return 404 if note not found
        // TODO: Return 403 if note belongs to different user
        throw new UnsupportedOperationException("TODO: Implement get note");
    }

    /**
     * PATCH /notes/{id}
     * Update a note (user edit).
     *
     * When user edits a note, it's marked as userEdited=true
     * to prevent auto-updates from future LLM responses.
     */
    @PatchMapping("/{id}")
    public ResponseEntity<NoteDto> updateNote(
        @PathVariable String id,
        @Valid @RequestBody UpdateNoteRequest request,
        @AuthenticationPrincipal String userId
    ) {
        // TODO: Call noteService.updateNote(id, request, userId)
        // TODO: Return 200 OK with updated NoteDto
        throw new UnsupportedOperationException("TODO: Implement update note");
    }

    /**
     * DELETE /notes/{id}
     * Delete a note.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNote(
        @PathVariable String id,
        @AuthenticationPrincipal String userId
    ) {
        // TODO: Call noteService.deleteNote(id, userId)
        // TODO: Return 204 No Content
        throw new UnsupportedOperationException("TODO: Implement delete note");
    }
}
