package com.alang.dto.chat;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Full session details including conversation messages.
 * Returned by GET /chat/session/{sessionId} so the frontend can restore
 * an active session after the user restarts the app.
 */
@Data
public class SessionDetailResponse {
    private String id;
    private String learningLanguage;
    private String teachingLanguage;
    private String status;
    private String title;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime closedAt;
    private boolean noteCreated;
    private List<MessageDto> messages;
}
