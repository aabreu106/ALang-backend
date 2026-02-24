package com.alang.controller;

import com.alang.dto.chat.ChatHistoryDto;
import com.alang.dto.chat.ChatMessageRequest;
import com.alang.dto.chat.ChatMessageResponse;
import com.alang.dto.chat.CloseSessionRequest;
import com.alang.dto.chat.CreateSessionRequest;
import com.alang.dto.chat.NoteFromSessionRequest;
import com.alang.dto.chat.SessionResponse;
import com.alang.dto.note.NoteDto;
import com.alang.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Chat controller.
 *
 * This controller ONLY:
 * - Validates HTTP requests
 * - Calls ChatService
 * - Returns HTTP responses
 *
 * All business logic and LLM interaction is in ChatService and LLMService.
 */
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /**
     * POST /chat/sessions
     * Create a new single-topic chat session.
     */
    @PostMapping("/sessions")
    public ResponseEntity<SessionResponse> createSession(
            @Valid @RequestBody CreateSessionRequest request,
            @AuthenticationPrincipal String userId
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(chatService.createSession(request, userId));
    }

    /**
     * GET /chat/sessions?language=es&limit=20
     * List recent sessions for the authenticated user.
     */
    @GetMapping("/sessions")
    public ResponseEntity<List<SessionResponse>> getSessions(
            @RequestParam(required = false) String language,
            @RequestParam(defaultValue = "20") int limit,
            @AuthenticationPrincipal String userId
    ) {
        return ResponseEntity.ok(chatService.getSessions(userId, language, limit));
    }

    /**
     * POST /chat/sessions/{sessionId}/message
     * Send a message within a session and get the AI reply.
     * The sessionId is bound into the request object so LLMService can resolve session context.
     */
    @PostMapping("/sessions/{sessionId}/message")
    public ResponseEntity<ChatMessageResponse> sendMessage(
            @PathVariable String sessionId,
            @Valid @RequestBody ChatMessageRequest request,
            @AuthenticationPrincipal String userId
    ) {
        request.setSessionId(sessionId);
        return ResponseEntity.ok(chatService.sendMessage(request, userId));
    }

    /**
     * POST /chat/sessions/{sessionId}/close
     * Close a session, preventing further messages from being sent.
     *
     * If force=false (default), the backend checks whether a note has been created.
     * If not, it returns the session with noteCreated=false without closing — the frontend
     * should prompt the user to confirm, then re-call with force=true.
     */
    @PostMapping("/sessions/{sessionId}/close")
    public ResponseEntity<SessionResponse> closeSession(
            @PathVariable String sessionId,
            @RequestBody(required = false) CloseSessionRequest request,
            @AuthenticationPrincipal String userId
    ) {
        if (request == null) {
            request = new CloseSessionRequest();
        }
        return ResponseEntity.ok(chatService.closeSession(sessionId, request, userId));
    }

    /**
     * POST /chat/sessions/{sessionId}/note
     * Explicitly create a note from the session's conversation.
     * Called when the user presses "Create Note".
     * Request body may be empty ({}) or include a topicFocus for targeted note creation.
     */
    @PostMapping("/sessions/{sessionId}/note")
    public ResponseEntity<NoteDto> createNote(
            @PathVariable String sessionId,
            @RequestBody(required = false) NoteFromSessionRequest request,
            @AuthenticationPrincipal String userId
    ) {
        if (request == null) {
            request = new NoteFromSessionRequest();
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(chatService.createNoteFromSession(sessionId, request, userId));
    }

    /**
     * PUT /chat/sessions/{sessionId}/note/{noteId}
     * Re-generate an existing note using the LLM and the session's full conversation.
     * Called when the user presses "Update Note" after additional follow-up questions.
     * Request body may be empty ({}) or include a topicFocus.
     */
    @PutMapping("/sessions/{sessionId}/note/{noteId}")
    public ResponseEntity<NoteDto> updateNote(
            @PathVariable String sessionId,
            @PathVariable String noteId,
            @RequestBody(required = false) NoteFromSessionRequest request,
            @AuthenticationPrincipal String userId
    ) {
        if (request == null) {
            request = new NoteFromSessionRequest();
        }
        return ResponseEntity.ok(chatService.updateNoteFromSession(sessionId, noteId, request, userId));
    }

    /**
     * GET /chat/history?language=ja&limit=20
     * Get conversation history (summaries + recent messages).
     * TODO: Implement in Week 4
     */
    @GetMapping("/history")
    public ResponseEntity<ChatHistoryDto> getHistory(
            @RequestParam String language,
            @RequestParam(defaultValue = "20") int limit,
            @AuthenticationPrincipal String userId
    ) {
        ChatHistoryDto history = chatService.getHistory(userId, language, limit);
        return ResponseEntity.ok(history);
    }
}
