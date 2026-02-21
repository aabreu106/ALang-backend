package com.alang.service;

import com.alang.dto.chat.ChatMessageRequest;
import com.alang.dto.chat.TokenUsageDto;
import com.alang.dto.note.NoteDto;
import com.alang.entity.ConversationSummary;
import com.alang.entity.Language;
import com.alang.entity.RecentMessage;

import java.util.List;
import java.util.Map;

/**
 * ⚠️ CRITICAL: This is THE ONLY service that talks to external LLM APIs.
 *
 * ARCHITECTURAL PRINCIPLES:
 * 1. NO controller should EVER call LLM API directly
 * 2. NO other service should call LLM API directly
 * 3. ALL LLM interaction flows through this service
 *
 * Why centralize LLM logic here?
 * 1. COST CONTROL: Single place to enforce rate limits, token budgets
 * 2. MODEL SELECTION: Choose model based on user tier
 * 3. PROMPT ENGINEERING: Centralized prompt templates and optimization
 * 4. TOKEN ACCOUNTING: Track usage for billing, analytics, limits
 * 5. PROVIDER ABSTRACTION: Can switch providers (Ollama, OpenAI, Anthropic, etc.) without touching controllers
 * 6. TESTING: Easy to mock this service in tests
 * 7. MONITORING: Single place to log, trace, and alert on LLM usage
 *
 * RESPONSIBILITIES:
 * - Model selection (cheap vs premium)
 * - Context assembly (summaries + recent messages)
 * - Prompt construction (system prompts, user prompts)
 * - Token budget enforcement
 * - Response parsing
 * - Note extraction
 * - Conversation summarization
 * - Error handling (rate limits, timeouts, invalid responses)
 */
public interface LLMService {

    /**
     * Generate a reply to user's message.
     *
     * ARCHITECTURAL FLOW:
     * 1. Determine which model to use (based on user tier)
     * 2. Load conversation context (summaries + recent messages)
     * 3. Check token budget (user daily limit, per-request limit)
     * 4. Assemble prompt (system prompt + context + user message)
     * 5. Call LLM API
     * 6. Parse response
     * 7. Track token usage
     * 8. Return reply
     *
     * MODEL SELECTION STRATEGY:
     * - User tier="free" -> CHEAP model
     * - User tier="pro" -> STANDARD model
     *
     * CONTEXT ASSEMBLY (TODO: implement in impl):
     * - Load last 2-3 ConversationSummary records for this user+language
     * - Load last 5-10 RecentMessage records
     * - Calculate total token count of context
     * - If context exceeds budget, drop oldest summaries first
     * - Never drop recent messages (they're critical for coherence)
     *
     * TOKEN BUDGET ENFORCEMENT (TODO: implement in impl):
     * - Check user.totalDailyTokensUsed against daily limit
     * - Free tier: 100k tokens/month
     * - Premium tier: 1M tokens/month
     * - Reject request if over limit (or auto-downgrade to cheap model)
     *
     * @param request User's message + metadata
     * @param userId User making the request
     * @return LLM's reply + metadata (model used, token usage)
     * @throws RateLimitExceededException if user over monthly token limit
     * @throws LLMProviderException if LLM API fails
     */
    LLMResponse generateReply(ChatMessageRequest request, String userId);

    /**
     * Generate a single structured note from a session's conversation history.
     *
     * Called when the user explicitly presses "Create Note" or "Update Note".
     * Returns an unpersisted NoteDto — ChatService is responsible for persisting it.
     *
     * @param sessionMessages Ordered session messages as role→content maps
     * @param topicFocus      Optional topic to focus on (from a topic chip). Null = general note.
     * @param existingNote    Null for a new note; non-null for an LLM-powered update.
     *                        When non-null, the prompt asks the LLM to update this note
     *                        using the full conversation as context.
     * @param learningLanguage The language being learned
     * @param appLanguage      The user's native language (for explanations)
     * @param userId           For token budget checking and recording
     * @return Parsed NoteDto (not yet persisted)
     * @throws RateLimitExceededException if user is over their token budget
     * @throws LLMProviderException       if the LLM call fails or returns unparseable JSON
     */
    NoteDto generateNoteFromConversation(
            List<Map<String, String>> sessionMessages,
            String topicFocus,
            NoteDto existingNote,
            Language learningLanguage,
            Language appLanguage,
            String userId
    );

    /**
     * Generate a conversation summary from recent messages.
     *
     * ARCHITECTURAL NOTE:
     * This is THE KEY to managing unbounded chat history.
     *
     * When to trigger summarization? (TODO: decide on strategy)
     * Option 1: Every N messages (e.g., every 10 exchanges)
     * Option 2: When context token count exceeds threshold
     * Option 3: When user ends a "session" (e.g., 1 hour of inactivity)
     * Recommended: Combination of #1 and #2
     *
     * How it works:
     * 1. Load recent messages (e.g., last 10 exchanges)
     * 2. Send to LLM with summarization prompt
     * 3. LLM condenses 10 exchanges into 1-2 paragraphs
     * 4. Extract key topics covered
     * 5. Return ConversationSummary object
     * 6. ChatService persists summary and DELETES original messages
     *
     * PROMPT ENGINEERING (TODO: implement in impl):
     * "Summarize the following conversation between a language learner and tutor.
     * Focus on: what concepts were discussed, what the user understood, what they struggled with.
     * Be concise (2-3 sentences max)."
     *
     * @param recentMessages Messages to summarize (typically 10-20)
     * @param userId User ID (for metadata)
     * @param language Language code (for metadata)
     * @return Conversation summary
     */
    ConversationSummary generateSummary(List<RecentMessage> recentMessages, String userId, String language);

    /**
     * Calculate token count for a given text.
     * Used for context budget management.
     *
     * TODO: Implement using tiktoken or similar tokenizer library
     * Different models have different tokenizers (GPT vs Claude)
     *
     * @param text Text to count tokens for
     * @param model Model name (different models have different tokenizers)
     * @return Token count
     */
    int countTokens(String text, String model);

    /**
     * Check if user has enough tokens remaining in their monthly budget.
     *
     * @param userId User ID
     * @param estimatedTokens Estimated tokens for upcoming request
     * @return true if user can proceed, false if over limit
     */
    boolean checkTokenBudget(String userId, int estimatedTokens);

    /**
     * Record token usage for a user (for billing/limits).
     *
     * @param userId User ID
     * @param tokenUsage Token usage details
     */
    void recordTokenUsage(String userId, TokenUsageDto tokenUsage);

    /**
     * Choose which LLM model to use based on user tier.
     *
     * Model tiers:
     * - CHEAP: Used for free tier users
     * - STANDARD: Used for pro tier users
     *
     * @param userId User ID (to check tier)
     * @return Model identifier (e.g., "gpt-4-turbo", "gpt-3.5-turbo")
     */
    String selectModel(String userId);

    /**
     * LLM response wrapper.
     * Contains reply + metadata.
     */
    class LLMResponse {
        private String reply;
        private String modelUsed;
        private TokenUsageDto tokenUsage;

        public LLMResponse(String reply, String modelUsed, TokenUsageDto tokenUsage) {
            this.reply = reply;
            this.modelUsed = modelUsed;
            this.tokenUsage = tokenUsage;
        }

        public String getReply() { return reply; }
        public String getModelUsed() { return modelUsed; }
        public TokenUsageDto getTokenUsage() { return tokenUsage; }
    }
}
