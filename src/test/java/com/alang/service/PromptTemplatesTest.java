package com.alang.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PromptTemplatesTest {

    private final PromptTemplates promptTemplates = new PromptTemplates();

    // --- buildChatSystemPrompt ---

    @Test
    void buildChatSystemPrompt_includesTargetLanguage() {
        String prompt = promptTemplates.buildChatSystemPrompt("English", "Japanese");

        assertThat(prompt).contains("Japanese");
    }

    @Test
    void buildChatSystemPrompt_includesAppLanguage() {
        String prompt = promptTemplates.buildChatSystemPrompt("English", "Japanese");

        assertThat(prompt).contains("English");
    }

    @Test
    void buildChatSystemPrompt_includesNotesDelimiter() {
        String prompt = promptTemplates.buildChatSystemPrompt("English", "Japanese");

        assertThat(prompt).contains(PromptTemplates.NOTES_DELIMITER);
    }

    @Test
    void buildChatSystemPrompt_worksWithDifferentLanguages() {
        String prompt = promptTemplates.buildChatSystemPrompt("日本語", "Korean");

        assertThat(prompt).contains("日本語");
        assertThat(prompt).contains("Korean");
    }

    // --- buildNoteExtractionPrompt ---

    @Test
    void buildNoteExtractionPrompt_includesBothLanguages() {
        String prompt = promptTemplates.buildNoteExtractionPrompt("Spanish", "French");

        assertThat(prompt).contains("Spanish");
        assertThat(prompt).contains("French");
    }

    @Test
    void buildNoteExtractionPrompt_includesNoteTypes() {
        String prompt = promptTemplates.buildNoteExtractionPrompt("English", "Japanese");

        assertThat(prompt).contains("vocab");
        assertThat(prompt).contains("grammar");
        assertThat(prompt).contains("exception");
        assertThat(prompt).contains("other");
    }

    // --- stripNotesBlock ---

    @Test
    void stripNotesBlock_removesNotesJson() {
        String raw = "Here is an explanation.\n---NOTES_JSON---\n{\"notes\":[]}";

        String result = PromptTemplates.stripNotesBlock(raw);

        assertThat(result).isEqualTo("Here is an explanation.");
    }

    @Test
    void stripNotesBlock_returnsFullTextWhenNoDelimiter() {
        String raw = "Just a plain reply with no notes.";

        String result = PromptTemplates.stripNotesBlock(raw);

        assertThat(result).isEqualTo("Just a plain reply with no notes.");
    }

    @Test
    void stripNotesBlock_returnsEmptyStringForNull() {
        assertThat(PromptTemplates.stripNotesBlock(null)).isEmpty();
    }

    @Test
    void stripNotesBlock_trimsWhitespace() {
        String raw = "  Some text  \n---NOTES_JSON---\n{\"notes\":[]}";

        String result = PromptTemplates.stripNotesBlock(raw);

        assertThat(result).isEqualTo("Some text");
    }

    // --- extractNotesJson ---

    @Test
    void extractNotesJson_returnsJsonAfterDelimiter() {
        String raw = "Explanation\n---NOTES_JSON---\n{\"notes\":[{\"type\":\"vocab\"}]}";

        String result = PromptTemplates.extractNotesJson(raw);

        assertThat(result).isEqualTo("{\"notes\":[{\"type\":\"vocab\"}]}");
    }

    @Test
    void extractNotesJson_returnsNullWhenNoDelimiter() {
        String raw = "Just a reply, no notes";

        assertThat(PromptTemplates.extractNotesJson(raw)).isNull();
    }

    @Test
    void extractNotesJson_returnsNullForNull() {
        assertThat(PromptTemplates.extractNotesJson(null)).isNull();
    }

    @Test
    void extractNotesJson_trimsWhitespace() {
        String raw = "Text\n---NOTES_JSON---\n  {\"notes\":[]}  ";

        String result = PromptTemplates.extractNotesJson(raw);

        assertThat(result).isEqualTo("{\"notes\":[]}");
    }

    // --- NOTES_DELIMITER constant ---

    @Test
    void notesDelimiter_hasExpectedValue() {
        assertThat(PromptTemplates.NOTES_DELIMITER).isEqualTo("---NOTES_JSON---");
    }
}
