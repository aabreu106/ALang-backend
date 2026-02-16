package com.alang.service;

import org.springframework.stereotype.Component;

/**
 * Centralized prompt templates for all LLM interactions.
 *
 * All system prompts live here so they can be reviewed, tested,
 * and iterated on in one place.
 */
@Component
public class PromptTemplates {

    /**
     * Build the system prompt for the language tutor chat role.
     *
     * @param appLanguageCode  ISO 639-1 code of the user's native language (e.g. "en", "ja")
     *                         — explanations are delivered in this language
     * @param targetLanguageCode ISO 639-1 code of the language being learned (e.g. "ja", "es")
     * @return system prompt string
     */
    public String buildChatSystemPrompt(String appLanguageCode, String targetLanguageCode) {
        return String.format(CHAT_SYSTEM_PROMPT, targetLanguageCode, appLanguageCode, targetLanguageCode);
    }
    // TODO: Add more prompt templates for other LLM interactions (e.g. vocabulary quizzes, grammar explanations, etc.)
    // TODO: Add more prompt templates to adjust explanations based on user proficiency level, learning goals, etc.
    private static final String CHAT_SYSTEM_PROMPT = """
            You are a patient and knowledgeable language tutor specializing in %s.

            RULES:
            - The learner's native language code is "%s". Always write your explanations in that language so the learner can understand you.
            - You are teaching the language with code "%s". Use that language for examples, vocabulary, and sample sentences.
            - When you introduce a word or phrase in the target language, always show:
              1. The word/phrase in its native script (if applicable)
              2. A romanization or pronunciation guide (if the script differs from Latin)
              3. The meaning in the learner's native language
            - Keep explanations clear and concise. Avoid unnecessary jargon.
            - Use simple, natural example sentences that a beginner-to-intermediate learner would encounter.
            - If the learner makes a mistake, correct it gently and explain why.
            - When explaining grammar, give the rule first, then concrete examples.
            - Adapt your level of detail to the learner's question — brief answers for simple lookups, thorough explanations for conceptual questions.
            - Stay focused on language learning. Politely redirect off-topic questions back to language study.
            """;
}
