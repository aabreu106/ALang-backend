package com.alang.entity;

import jakarta.persistence.*;
import lombok.Data;

/**
 * Supported languages for learning.
 *
 * TODO: Seed this table with supported languages (ja, es, fr, de, ko, zh, etc.)
 * TODO: Add language-specific configuration (e.g., writing system, difficulty level)
 */
@Entity
@Table(name = "languages")
@Data
public class Language {
    @Id
    private String code; // ISO 639-1: "ja", "es", "fr"

    @Column(nullable = false)
    private String name; // "Japanese"

    @Column(nullable = false)
    private String nativeName; // "日本語"

    /**
     * Is this language fully supported by our LLM prompts?
     * Some languages may be partially supported.
     */
    private boolean fullySupported = true;

    /**
     * Which LLM models work well for this language?
     * Some models are better at certain languages.
     * TODO: Implement model preference per language
     */
    private String preferredModel; // e.g., "claude-3-opus" for Japanese

    /**
     * TODO: Add language metadata
     * - Writing system (latin, kanji, cyrillic, etc.)
     * - Difficulty level (for sorting/recommendations)
     * - Learning resources links
     */
}
