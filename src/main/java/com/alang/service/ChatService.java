package com.alang.service;

import com.alang.dto.chat.ChatHistoryDto;
import com.alang.dto.chat.ChatMessageRequest;
import com.alang.dto.chat.ChatMessageResponse;

/**
 * Chat orchestration service.
 *
 * RESPONSIBILITIES:
 * - Handle incoming chat messages
 * - Call LLMService to generate replies
 * - Save messages to database (RecentMessage)
 * - Trigger note extraction (via NoteService)
 * - Trigger conversation summarization when threshold reached
 * - Return response to controller
 *
 * ARCHITECTURAL NOTE:
 * This service orchestrates the chat flow, but does NOT call LLM API directly.
 * All LLM interaction goes through LLMService.
 */
public interface ChatService {

    /**
     * Process a chat message from the user.
     *
     * FLOW:
     * 1. Validate request (language exists, user exists)
     * 2. Save user's message to RecentMessage table
     * 3. Call LLMService.generateReply() to get response
     * 4. Save assistant's reply to RecentMessage table
     * 5. Call LLMService.extractNotes() to extract learning notes
     * 6. Save extracted notes via NoteService
     * 7. Check if summarization threshold reached (e.g., 10 messages)
     * 8. If yes, call LLMService.generateSummary() and save summary
     * 9. Delete old messages that have been summarized
     * 10. Return ChatMessageResponse to controller
     *
     * ERROR HANDLING (TODO: implement):
     * - LLM API failure: Return friendly error message, don't save broken response
     * - Rate limit exceeded: Inform user, suggest waiting or upgrading
     * - Invalid language: Return error
     *
     * @param request User's message request
     * @param userId Authenticated user ID
     * @return Chat response with reply and created notes
     */
    ChatMessageResponse sendMessage(ChatMessageRequest request, String userId);

    /**
     * Get conversation history for a user in a specific language.
     *
     * ARCHITECTURAL NOTE:
     * This returns SUMMARIES, not raw messages.
     * Only recent messages (last 5-10) are included for context.
     *
     * @param userId User ID
     * @param language Language code
     * @param limit Max number of summaries to return
     * @return Chat history with summaries and recent exchanges
     */
    ChatHistoryDto getHistory(String userId, String language, int limit);

    /**
     * Check if summarization should be triggered.
     *
     * Triggers:
     * - Message count threshold (e.g., every 10 messages)
     * - Token count threshold (e.g., context exceeds 2000 tokens)
     * - Time threshold (e.g., 1 hour since last summary)
     *
     * TODO: Implement configurable thresholds
     *
     * @param userId User ID
     * @param language Language code
     * @return true if summarization should run
     */
    boolean shouldTriggerSummarization(String userId, String language);

    /**
     * Trigger conversation summarization.
     *
     * FLOW:
     * 1. Load recent unsummarized messages
     * 2. Call LLMService.generateSummary()
     * 3. Save ConversationSummary to database
     * 4. Delete summarized messages from RecentMessage table
     * 5. Log summarization event for analytics
     *
     * TODO: Make this async (run in background)
     * TODO: Add retry logic if summarization fails
     *
     * @param userId User ID
     * @param language Language code
     */
    void triggerSummarization(String userId, String language);
}
