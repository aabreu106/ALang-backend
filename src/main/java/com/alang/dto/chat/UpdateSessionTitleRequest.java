package com.alang.dto.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request DTO for PATCH /chat/sessions/{sessionId}/title
 */
@Data
public class UpdateSessionTitleRequest {

    @NotBlank
    @Size(max = 255)
    private String title;
}
