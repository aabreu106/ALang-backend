package com.alang.dto.chat;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request DTO for POST /chat/sessions
 */
@Data
public class CreateSessionRequest {

    @NotBlank
    private String language; // learning language code, e.g. "es", "ja"

    private String title; // optional user-supplied label for the session
}
