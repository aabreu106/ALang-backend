package com.alang.dto.chat;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request DTO for POST /chat/message
 *
 * This is what the frontend sends. Notice:
 * - No raw LLM prompt
 * - No model selection (backend decides based on user tier)
 */
@Data
public class ChatMessageRequest {
    @NotBlank
    private String language; // e.g., "ja", "es"

    @NotBlank
    private String message; // User's question

    /**
     * Optional: Include recent conversation context
     * If true, backend will load recent ConversationSummary (NOT raw messages)
     */
    private Boolean includeContext = true;
}
