package com.alang.controller;

import com.alang.dto.note.CreateNoteRequest;
import com.alang.dto.note.NoteDto;
import com.alang.dto.note.NoteListResponse;
import com.alang.dto.note.UpdateNoteRequest;
import com.alang.service.NoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
 */
@RestController
@RequestMapping("/notes")
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;

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
        return ResponseEntity.ok(noteService.getNotes(userId, language, type, minConfidence, search, page, pageSize));
    }

    /**
     * POST /notes
     * Manually create a study note.
     */
    @PostMapping
    public ResponseEntity<NoteDto> createNote(
        @Valid @RequestBody CreateNoteRequest request,
        @AuthenticationPrincipal String userId
    ) {
        NoteDto noteDto = new NoteDto();
        noteDto.setType(request.getType());
        noteDto.setLearningLanguage(request.getLanguage());
        noteDto.setTitle(request.getTitle());
        noteDto.setSummary(request.getSummary());
        noteDto.setNoteContent(request.getNoteContent());
        noteDto.setUserEdited(true);
        return ResponseEntity.status(HttpStatus.CREATED).body(noteService.createNote(noteDto, userId));
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
        return ResponseEntity.ok(noteService.getNote(id, userId));
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
        return ResponseEntity.ok(noteService.updateNote(id, request, userId));
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
        noteService.deleteNote(id, userId);
        return ResponseEntity.noContent().build();
    }
}
