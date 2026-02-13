package com.alang.dto.auth;

import lombok.Data;
import java.util.List;

@Data
public class UserResponse {
    private String userId;
    private String email;
    private String displayName;
    private String appLanguage; // Language used for UI/LLM explanations
    private List<String> targetLanguages; // Languages they're learning
}
