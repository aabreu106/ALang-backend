package com.alang.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Centralized prompt templates for all LLM interactions.
 *
 * All system prompts live here so they can be reviewed, tested,
 * and iterated on in one place.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PromptTemplates {

    // ---- Delimiters ----

    public static final String TOPICS_DELIMITER = "---TOPICS---";

    // ---- Chat prompt ----

    /**
     * Build the system prompt for the language tutor chat role.
     * Does NOT include note extraction instructions — notes are created explicitly
     * by the user at the end of a session, not auto-extracted on every message.
     *
     * @param appLanguageName    Full name of the user's native language (e.g. "English")
     * @param targetLanguageName Full name of the language being learned (e.g. "Spanish")
     * @return system prompt string
     */
    public String buildChatSystemPrompt(String appLanguageName, String targetLanguageName) {
        return String.format(CHAT_SYSTEM_PROMPT, targetLanguageName, appLanguageName, targetLanguageName);
    }

    // ---- Note creation prompts ----

    /**
     * Build the system prompt for the explicit note-creation LLM call.
     * Instructs the LLM to return ONLY valid JSON — no surrounding text.
     *
     * @param appLanguageName    Full name of the user's native language (e.g. "English")
     * @param targetLanguageName Full name of the language being learned (e.g. "Spanish")
     * @return system prompt string
     */
    public String buildNoteCreationSystemPrompt(String appLanguageName, String targetLanguageName) {
        return String.format(NOTE_CREATION_SYSTEM_PROMPT, targetLanguageName, appLanguageName, targetLanguageName);
    }

    /**
     * Build the user prompt for creating a new note from a session conversation.
     *
     * @param messages   Ordered session messages as role→content maps
     * @param topicFocus Optional topic to focus on (from a topic chip). Null = general note.
     * @return user prompt string
     */
    public String buildNoteCreationUserPrompt(List<Map<String, String>> messages, String topicFocus) {
        StringBuilder sb = new StringBuilder();
        if (topicFocus != null && !topicFocus.isBlank()) {
            sb.append("Create a study note specifically about: ").append(topicFocus.trim()).append("\n\n");
        } else {
            sb.append("Create a study note capturing the most important concepts from this conversation.\n\n");
        }
        appendConversation(sb, messages);
        return sb.toString();
    }

    /**
     * Build the user prompt for updating an existing note from a session conversation.
     * Passes the current note JSON to the LLM so it can build on it rather than
     * regenerating from scratch.
     *
     * @param messages         Ordered session messages as role→content maps
     * @param existingNoteJson Current note content serialized to JSON
     * @param topicFocus       Optional topic focus. Null = update the whole note.
     * @return user prompt string
     */
    public String buildNoteUpdateUserPrompt(List<Map<String, String>> messages,
                                            String existingNoteJson,
                                            String topicFocus) {
        StringBuilder sb = new StringBuilder();
        sb.append("Update the following existing study note based on new information in the conversation below.\n");
        if (topicFocus != null && !topicFocus.isBlank()) {
            sb.append("Focus the update on: ").append(topicFocus.trim()).append("\n");
        }
        sb.append("\nExisting note:\n").append(existingNoteJson).append("\n\n");
        appendConversation(sb, messages);
        return sb.toString();
    }

    private void appendConversation(StringBuilder sb, List<Map<String, String>> messages) {
        sb.append("Conversation:\n");
        for (Map<String, String> msg : messages) {
            String role = msg.get("role");
            String content = msg.get("content");
            sb.append("user".equals(role) ? "Learner: " : "Tutor: ");
            sb.append(content).append("\n");
        }
    }

    // ---- Topic suggestion helpers ----

    /**
     * Extract the list of suggested topics from the ---TOPICS--- block, if present.
     * Returns an empty list if no block is found or if JSON parsing fails.
     */
    public static List<String> extractTopics(String rawResponse, ObjectMapper mapper) {
        if (rawResponse == null) return List.of();
        int idx = rawResponse.indexOf(TOPICS_DELIMITER);
        if (idx == -1) return List.of();
        String jsonPart = rawResponse.substring(idx + TOPICS_DELIMITER.length()).trim();
        try {
            return mapper.readValue(jsonPart, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse topics JSON: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Strip the ---TOPICS--- block (and everything after it) from the LLM reply,
     * returning only the user-facing explanation text.
     */
    public static String stripTopicsBlock(String rawResponse) {
        if (rawResponse == null) return "";
        int idx = rawResponse.indexOf(TOPICS_DELIMITER);
        if (idx == -1) return rawResponse.trim();
        return rawResponse.substring(0, idx).trim();
    }

    // ---- Private prompt templates ----

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

            TOPIC SUGGESTION:
            If your response covers 3 or more distinctly learnable topics — each one representing a concept worth its own dedicated study note — append a topics block using the format below. Otherwise, omit this section entirely.

            ---TOPICS---
            ["topic title 1", "topic title 2", "topic title 3"]

            Rules for topic suggestion:
            - Only append when 3 or more genuinely separate, learnable concepts are covered.
            - Topic titles must be short (40 characters max).
            - Use the target language's standard notation or a recognized short name for each topic.
            - Do not include topics that are trivially obvious or merely restate the user's question.
            - If fewer than 3 distinct topics exist, do NOT append this section at all.
            - The JSON array must be valid. Double-quoted strings only. No trailing commas.
            """;

    private static final String NOTE_CREATION_SYSTEM_PROMPT = """
            You are a language learning assistant that creates structured study notes from tutor conversations.

            The target language is %s. The learner's native language is %s.
            Notes must help the learner review %s concepts.

            Respond with ONLY valid JSON matching this exact schema (no other text, no markdown fences):
            {
              "type": "vocab | grammar | exception | other",
              "title": "short title (under 60 characters)",
              "summary": "1-2 sentence explanation in the learner's native language",
              "content": "fuller explanation with examples, in the learner's native language",
              "structured": { <type-specific fields> },
              "tags": [{ "category": "...", "value": "..." }]
            }

            Type-specific "structured" fields:
            - vocab:     { "word": "...", "reading": "...", "meaning": "...", "partOfSpeech": "noun|verb|adjective|adverb|particle|other", "exampleSentences": ["..."], "commonMistakes": ["..."] }
            - grammar:   { "pattern": "...", "meaning": "...", "explanation": "...", "formality": "casual|polite|formal", "exampleSentences": ["..."], "commonMistakes": ["..."] }
            - exception: { "rule": "...", "exception": "...", "explanation": "...", "exampleSentences": ["..."] }
            - other:     use any relevant key-value pairs

            Tag categories and example values:
            - "topic": food, travel, work, family, weather, shopping, health, school, daily_life, culture, sports, technology, entertainment
            - "formality": casual, polite, formal, slang, literary
            - "difficulty": beginner, intermediate, advanced
            - "function": contrast, cause, condition, request, permission, comparison, negation, sequence, description, greeting

            Rules:
            - "type" must be exactly one of: vocab, grammar, exception, other.
            - "title" is the target-language word, phrase, or grammar point name. Keep it short (under 60 characters).
            - "summary" is 1-2 sentences — a quick reminder of the concept.
            - "content" is the full explanation with examples. Match the quality of a good tutor explanation.
            - "structured" is required. Always include it, even if some fields are empty strings or empty arrays.
            - "tags" is an array of 1-4 tags using the categories and values listed above. Lowercase values.
            - Be sure to include any example sentences from the assistant's explanations in the "exampleSentences" field, even if they are simple or repetitive. Examples are crucial for learning.
            - Write "summary" and "content" in the learner's native language, with inline target-language examples.
            - Return ONLY the JSON object. No surrounding text, no markdown code fences.
            """;

}
