package com.alang.dto.chat;

import lombok.Data;
import java.util.List;

/**
 * Response DTO for POST /chat/sessions/{sessionId}/message
 *
 * ARCHITECTURAL NOTE:
 * Notes are no longer auto-created on every message. The user explicitly
 * triggers note creation via POST /chat/sessions/{sessionId}/note.
 *
 * suggestedTopics is populated only when the LLM detects 3+ distinct learnable
 * topics in its response (broad questions). The frontend renders these as chips
 * the user can click to create a focused note for each topic.
 */
@Data
public class ChatMessageResponse {

    private String reply;

    /**
     * Non-null only when the LLM returned a ---TOPICS--- block.
     * Each entry is a short topic title (≤40 chars).
     * Frontend renders these as "Create note for: [topic]" chips.
     */
    private List<String> suggestedTopics;

    private TokenUsageDto tokenUsage;

    private String modelUsed;
}
