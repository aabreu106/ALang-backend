package com.alang.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.List;

@Data
public class SignupRequest {
    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 8)
    private String password;

    @NotBlank
    private String displayName;

    // The language used for UI and LLM explanations
    @NotBlank
    private String appLanguageCode; // e.g., "en", "ja"

    // Languages the user wants to learn
    private List<String> targetLanguageCodes; // e.g., ["ja", "es"]
}
