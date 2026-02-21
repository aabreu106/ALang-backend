package com.alang.dto.chat;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Response DTO for session endpoints.
 */
@Data
public class SessionResponse {

    private String id;
    private String learningLanguage; // language code, e.g. "es"
    private String teachingLanguage; // language code, e.g. "en"
    private String status;           // "active" | "closed"
    private String title;            // may be null if not set
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime closedAt;  // null if session is still active
    private int messageCount;        // number of RecentMessages in this session
}
