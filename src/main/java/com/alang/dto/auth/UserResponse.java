package com.alang.dto.auth;

import lombok.Data;
import java.util.List;

@Data
public class UserResponse {
    private String userId;
    private String email;
    private String displayName;
    private List<String> targetLanguages; // Languages they're learning
    private String preferredLanguage; // Default for new chats
}
