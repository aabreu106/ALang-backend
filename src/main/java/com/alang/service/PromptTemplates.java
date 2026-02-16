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

    public static final String NOTES_DELIMITER = "---NOTES_JSON---";

    /**
     * Build the system prompt for the language tutor chat role.
     * Includes instructions for structured note extraction.
     *
     * @param appLanguageName  Full name of the user's native language (e.g. "English", "Japanese")
     *                         — explanations are delivered in this language
     * @param targetLanguageName Full name of the language being learned (e.g. "Japanese", "Spanish")
     * @return system prompt string
     */
    public String buildChatSystemPrompt(String appLanguageName, String targetLanguageName) {
        return String.format(CHAT_SYSTEM_PROMPT, targetLanguageName, appLanguageName, targetLanguageName);
    }

    /**
     * Build a prompt for a standalone note extraction call.
     * Used as a fallback when the inline extraction from chat fails or
     * when re-extracting notes from an existing response.
     *
     * @param appLanguageName   Full name of the user's native language
     * @param targetLanguageName Full name of the language being learned
     * @return system prompt for note extraction
     */
    public String buildNoteExtractionPrompt(String appLanguageName, String targetLanguageName) {
        return String.format(NOTE_EXTRACTION_PROMPT, targetLanguageName, appLanguageName, targetLanguageName);
    }

    /**
     * Extract the user-facing reply from an LLM response, stripping the notes JSON block.
     *
     * @param rawResponse Full LLM response (may contain notes delimiter + JSON)
     * @return Clean reply text without the notes block
     */
    public static String stripNotesBlock(String rawResponse) {
        if (rawResponse == null) return "";
        int idx = rawResponse.indexOf(NOTES_DELIMITER);
        if (idx == -1) return rawResponse.trim();
        return rawResponse.substring(0, idx).trim();
    }

    /**
     * Extract the raw JSON string after the notes delimiter.
     *
     * @param rawResponse Full LLM response
     * @return JSON string, or null if no notes block found
     */
    public static String extractNotesJson(String rawResponse) {
        if (rawResponse == null) return null;
        int idx = rawResponse.indexOf(NOTES_DELIMITER);
        if (idx == -1) return null;
        return rawResponse.substring(idx + NOTES_DELIMITER.length()).trim();
    }

    // TODO: Add more prompt templates for other LLM interactions (e.g. vocabulary quizzes, grammar explanations, etc.)
    // TODO: Add more prompt templates to adjust explanations based on user proficiency level, learning goals, etc.

    private static final String CHAT_SYSTEM_PROMPT = """
            You are a patient and knowledgeable language tutor specializing in %s.

            RULES:
            - The learner's native language is %s. Always write your explanations in that language so the learner can understand you.
            - You are teaching %s. Use that language for examples, vocabulary, and sample sentences.
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

            NOTE EXTRACTION:
            After your explanation, if you taught any vocabulary, grammar rules, or language exceptions, \
            append a structured JSON block so the system can save study notes for the learner.

            Format — place the delimiter on its own line, then valid JSON:

            ---NOTES_JSON---
            {
              "notes": [
                {
                  "type": "vocab | grammar | exception | other",
                  "title": "short title (the word, phrase, or grammar point)",
                  "summary": "one-sentence explanation in the learner's native language",
                  "content": "fuller explanation with examples, in the learner's native language"
                }
              ]
            }

            Rules for notes:
            - Only include notes when you actually taught something. Casual greetings or off-topic replies should have NO notes block.
            - "type" must be exactly one of: vocab, grammar, exception, other.
            - "title" should be the target-language word/phrase or grammar point name. Keep it short (under 60 characters).
            - "summary" is 1-2 sentences max — a quick reminder of the concept.
            - "content" is the full explanation including examples. Use the same quality as your main explanation.
            - Extract 1-3 notes per response. Do not over-extract — only distinct concepts the learner should review.
            - Write "summary" and "content" in the learner's native language, but include target-language examples inline.
            - If you corrected a mistake, extract it as type "exception" or "grammar" as appropriate.
            - The JSON must be valid. Do not include trailing commas or comments.
            """;
    // This prompt is a fallback for when we want to extract notes from an existing response, or if the inline extraction fails. It focuses solely on the note extraction task.
    private static final String NOTE_EXTRACTION_PROMPT = """
            You are a language learning assistant that extracts structured study notes from tutor explanations.

            The target language is %s. The learner's native language is %s. Notes should help the learner review %s concepts.

            Given a tutor's response, extract any vocabulary, grammar rules, or language exceptions into structured JSON.

            Respond with ONLY valid JSON in this format (no other text):
            {
              "notes": [
                {
                  "type": "vocab | grammar | exception | other",
                  "title": "short title (the word, phrase, or grammar point)",
                  "summary": "one-sentence explanation in the learner's native language",
                  "content": "fuller explanation with examples, in the learner's native language"
                }
              ]
            }

            Rules:
            - "type" must be exactly one of: vocab, grammar, exception, other.
            - "title" should be the target-language word/phrase or grammar point name. Under 60 characters.
            - "summary" is 1-2 sentences — a quick reminder.
            - "content" is the full explanation with examples.
            - Extract 1-3 notes max. Only distinct concepts worth reviewing.
            - If the response contains no teachable content, return: {"notes": []}
            - Write "summary" and "content" in the learner's native language, with target-language examples inline.
            """;
}
