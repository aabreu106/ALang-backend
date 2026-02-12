package com.alang.dto.meta;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Response for GET /meta/languages
 *
 * Lists supported languages for learning.
 */
@Data
@AllArgsConstructor
public class LanguageDto {
    private String code; // ISO 639-1: "ja", "es", "fr"
    private String name; // "Japanese", "Spanish", "French"
    private String nativeName; // "日本語", "Español", "Français"
    private boolean supported; // Is this language fully supported by our LLM prompts?
}
