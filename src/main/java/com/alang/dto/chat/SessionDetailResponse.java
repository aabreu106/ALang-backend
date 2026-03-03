package com.alang.dto.chat;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Full session details including conversation messages.
 * Returned by GET /chat/sessions/active so the frontend can restore
 * the active sessions after the user restarts the app.
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
    private String noteId;           // null until a note is created from this session
    private List<MessageDto> messages;
}
