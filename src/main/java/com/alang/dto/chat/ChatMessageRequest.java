package com.alang.dto.chat;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request DTO for POST /chat/sessions/{sessionId}/message
 *
 * The sessionId is bound from the path variable by the controller.
 * Language is derived from the session — it is no longer passed in the request body.
 */
@Data
public class ChatMessageRequest {

    // Set by the controller from the {sessionId} path variable — not sent by the frontend in the body.
    private String sessionId;

    @NotBlank
    private String message;

    /**
     * Whether to include recent conversation context from this session.
     * Defaults to true. Can be set to false to get a context-free response.
     */
    private Boolean includeContext = true;
}
