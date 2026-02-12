package com.alang.dto.chat;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request DTO for POST /chat/message
 *
 * This is what the frontend sends. Notice:
 * - No raw LLM prompt
 * - No model selection (backend decides based on depth/intent)
 * - Intent helps backend choose between cheap/premium models
 */
@Data
public class ChatMessageRequest {
    @NotBlank
    private String language; // e.g., "ja", "es"

    @NotBlank
    private String message; // User's question

    /**
     * Intent hint for model selection and prompt engineering.
     * Examples: "grammar_explanation", "vocabulary", "casual_chat", "correction_request"
     *
     * ARCHITECTURAL NOTE:
     * - "casual_chat" -> cheap model
     * - "grammar_explanation" -> premium model
     * - This is decided in LLMService, NOT in frontend
     */
    private String intent;

    /**
     * Depth hint for response detail.
     * Examples: "brief", "normal", "detailed"
     *
     * ARCHITECTURAL NOTE:
     * - Affects token usage
     * - "brief" -> cheap model, shorter context
     * - "detailed" -> premium model, longer context
     */
    private String depth;

    /**
     * Optional: Include recent conversation context
     * If true, backend will load recent ConversationSummary (NOT raw messages)
     */
    private Boolean includeContext = true;
}
