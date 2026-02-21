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
    void buildChatSystemPrompt_includesTopicsDelimiter() {
        String prompt = promptTemplates.buildChatSystemPrompt("English", "Japanese");

        assertThat(prompt).contains(PromptTemplates.TOPICS_DELIMITER);
    }

    @Test
    void buildChatSystemPrompt_worksWithDifferentLanguages() {
        String prompt = promptTemplates.buildChatSystemPrompt("日本語", "Korean");

        assertThat(prompt).contains("日本語");
        assertThat(prompt).contains("Korean");
    }

    // --- stripTopicsBlock ---

    @Test
    void stripTopicsBlock_removesTopicsJson() {
        String raw = "Here is an explanation.\n---TOPICS---\n[\"topic1\"]";

        String result = PromptTemplates.stripTopicsBlock(raw);

        assertThat(result).isEqualTo("Here is an explanation.");
    }

    @Test
    void stripTopicsBlock_returnsFullTextWhenNoDelimiter() {
        String raw = "Just a plain reply with no topics.";

        String result = PromptTemplates.stripTopicsBlock(raw);

        assertThat(result).isEqualTo("Just a plain reply with no topics.");
    }

    @Test
    void stripTopicsBlock_returnsEmptyStringForNull() {
        assertThat(PromptTemplates.stripTopicsBlock(null)).isEmpty();
    }

    @Test
    void stripTopicsBlock_trimsWhitespace() {
        String raw = "  Some text  \n---TOPICS---\n[\"topic1\"]";

        String result = PromptTemplates.stripTopicsBlock(raw);

        assertThat(result).isEqualTo("Some text");
    }

    // --- TOPICS_DELIMITER constant ---

    @Test
    void topicsDelimiter_hasExpectedValue() {
        assertThat(PromptTemplates.TOPICS_DELIMITER).isEqualTo("---TOPICS---");
    }
}
