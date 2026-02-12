package com.alang.dto.chat;

import com.alang.dto.note.NotePreviewDto;
import lombok.Data;
import java.util.List;

/**
 * Response DTO for POST /chat/message
 *
 * ARCHITECTURAL NOTE:
 * - Frontend NEVER sees raw LLM response
 * - Backend may post-process, sanitize, or enhance the reply
 * - Notes are automatically extracted by backend (user didn't request them explicitly)
 */
@Data
public class ChatMessageResponse {
    /**
     * The actual reply to show to the user.
     * This has been processed/sanitized by backend.
     */
    private String reply;

    /**
     * Notes that were automatically created during this conversation turn.
     * User can choose to review/edit these later.
     */
    private List<NotePreviewDto> createdNotes;

    /**
     * Optional: Token usage for this request (for transparency/debugging)
     * TODO: Implement token tracking in LLMService
     */
    private TokenUsageDto tokenUsage;

    /**
     * Optional: Which model was used (for transparency)
     * Example: "gpt-4", "claude-3-sonnet"
     */
    private String modelUsed;
}
