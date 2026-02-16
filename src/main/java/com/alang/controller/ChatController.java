package com.alang.controller;

import com.alang.dto.chat.ChatHistoryDto;
import com.alang.dto.chat.ChatMessageRequest;
import com.alang.dto.chat.ChatMessageResponse;
import com.alang.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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
     * POST /chat/message
     * Send a chat message and get AI response.
     */
    @PostMapping("/message")
    public ResponseEntity<ChatMessageResponse> sendMessage(
        @Valid @RequestBody ChatMessageRequest request,
        @AuthenticationPrincipal String userId
    ) {
        ChatMessageResponse response = chatService.sendMessage(request, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /chat/history?language=ja&limit=20
     * Get conversation history (summaries + recent messages).
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
