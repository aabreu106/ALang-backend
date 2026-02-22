package com.alang.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddLanguageRequest {
    @NotBlank
    private String languageCode;
}
