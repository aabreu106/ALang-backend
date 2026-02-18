package com.alang.controller;

import com.alang.dto.meta.LanguageDto;
import com.alang.dto.meta.StarterPromptDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Meta/utility endpoints.
 *
 * RESPONSIBILITIES:
 * - List supported languages
 * - Provide starter prompts for onboarding
 * - Other metadata that doesn't fit elsewhere
 *
 * TODO: Implement language and starter prompt services
 */
@RestController
@RequestMapping("/meta")
public class MetaController {

    /**
     * GET /meta/languages
     * Get list of supported languages.
     *
     * Used by frontend for language selection dropdown.
     */
    @GetMapping("/languages")
    public ResponseEntity<List<LanguageDto>> getLanguages() {
        // TODO: Load from Language table
        // TODO: Return list of LanguageDto
        //
        // Example hardcoded response:
        // List<LanguageDto> languages = List.of(
        //     new LanguageDto("ja", "Japanese", "日本語", true),
        //     new LanguageDto("es", "Spanish", "Español", true),
        //     new LanguageDto("fr", "French", "Français", true),
        //     new LanguageDto("de", "German", "Deutsch", true),
        //     new LanguageDto("ko", "Korean", "한국어", true),
        //     new LanguageDto("zh", "Chinese", "中文", true)
        // );
        // return ResponseEntity.ok(languages);
        throw new UnsupportedOperationException("TODO: Implement get languages");
    }

    /**
     * GET /meta/starter-prompts?language=ja
     * Get example prompts to help users get started.
     *
     * These are pre-written questions like:
     * - "What's the difference between は and が?"
     * - "How do I conjugate する verbs?"
     * - "Explain the て-form"
     */
    @GetMapping("/starter-prompts")
    public ResponseEntity<List<StarterPromptDto>> getStarterPrompts(
        @RequestParam String language
    ) {
        // TODO: Load from database or config file
        // TODO: Return list of StarterPromptDto
        //
        // Example hardcoded response for Japanese:
        // if ("ja".equals(language)) {
        //     List<StarterPromptDto> prompts = List.of(
        //         new StarterPromptDto("1", "ja", "grammar", "What's the difference between は and が?"),
        //         new StarterPromptDto("2", "ja", "grammar", "How do I use the て-form?"),
        //         new StarterPromptDto("3", "ja", "vocabulary", "What are common counter words?")
        //     );
        //     return ResponseEntity.ok(prompts);
        // }
        throw new UnsupportedOperationException("TODO: Implement get starter prompts");
    }
}
