package com.alang.dto.chat;

import lombok.Data;

/**
 * Request DTO for POST /chat/sessions/{sessionId}/note
 * and PUT /chat/sessions/{sessionId}/note/{noteId}
 *
 * topicFocus is optional. When present (user clicked a topic chip from a broad-question
 * response), the LLM creates a note specifically about that topic rather than
 * summarizing the whole conversation.
 */
@Data
public class NoteFromSessionRequest {

    private String topicFocus; // nullable — e.g. "exceptions to el/la rule"
}
