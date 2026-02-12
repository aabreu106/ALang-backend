package com.alang.controller;

import com.alang.dto.chat.ChatHistoryDto;
import com.alang.dto.chat.ChatMessageRequest;
import com.alang.dto.chat.ChatMessageResponse;
import com.alang.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Chat controller.
 *
 * ⚠️ CRITICAL ARCHITECTURAL PRINCIPLE:
 * This controller does NOT call LLM APIs.
 * This controller does NOT contain business logic.
 * This controller does NOT assemble prompts.
 *
 * All of that is in ChatService and LLMService.
 *
 * This controller ONLY:
 * - Validates HTTP requests
 * - Calls ChatService
 * - Returns HTTP responses
 *
 * Why this separation matters:
 * 1. SECURITY: Controllers are exposed to internet; they should be dumb
 * 2. TESTABILITY: Easy to test business logic without HTTP layer
 * 3. REUSABILITY: Can call ChatService from other places (async jobs, webhooks)
 * 4. COST CONTROL: All LLM logic centralized in LLMService
 *
 * TODO: Inject ChatService
 */
@RestController
@RequestMapping("/chat")
public class ChatController {

    // TODO: Inject ChatService
    // private final ChatService chatService;

    /**
     * POST /chat/message
     * Send a chat message and get AI response.
     *
     * This is the CORE endpoint of the application.
     *
     * WHAT THIS METHOD DOES:
     * - Extract user ID from JWT token
     * - Validate request body
     * - Call chatService.sendMessage()
     * - Return response
     *
     * WHAT THIS METHOD DOES NOT DO:
     * - Call LLM API (that's in LLMService)
     * - Assemble prompts (that's in LLMService)
     * - Extract notes (that's in NoteService via ChatService)
     * - Save to database (that's in ChatService)
     */
    @PostMapping("/message")
    public ResponseEntity<ChatMessageResponse> sendMessage(
        @Valid @RequestBody ChatMessageRequest request,
        @AuthenticationPrincipal String userId
    ) {
        // TODO: Call chatService.sendMessage(request, userId)
        // TODO: Return 200 OK with ChatMessageResponse
        //
        // Example implementation:
        // ChatMessageResponse response = chatService.sendMessage(request, userId);
        // return ResponseEntity.ok(response);
        throw new UnsupportedOperationException("TODO: Implement send message");
    }

    /**
     * GET /chat/history?language=ja&limit=20
     * Get conversation history (SUMMARIES, not raw messages).
     *
     * ARCHITECTURAL NOTE:
     * This returns summaries + recent messages, NOT full chat history.
     * See ChatHistoryDto for details.
     */
    @GetMapping("/history")
    public ResponseEntity<ChatHistoryDto> getHistory(
        @RequestParam String language,
        @RequestParam(defaultValue = "20") int limit,
        @AuthenticationPrincipal String userId
    ) {
        // TODO: Call chatService.getHistory(userId, language, limit)
        // TODO: Return 200 OK with ChatHistoryDto
        throw new UnsupportedOperationException("TODO: Implement get history");
    }
}
