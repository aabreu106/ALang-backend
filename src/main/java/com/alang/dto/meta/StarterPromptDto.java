package com.alang.dto.meta;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Response for GET /meta/starter-prompts
 *
 * Pre-written example questions to help users get started.
 */
@Data
@AllArgsConstructor
public class StarterPromptDto {
    private String id;
    private String language; // "ja", "es", etc.
    private String category; // "grammar", "vocabulary", "culture"
    private String promptText; // e.g., "What's the difference between は and が?"
    private String intent; // Corresponding intent for ChatMessageRequest
}
