package com.alang.service;

import com.alang.dto.chat.ChatHistoryDto;
import com.alang.dto.chat.ChatMessageRequest;
import com.alang.dto.chat.ChatMessageResponse;
import com.alang.dto.chat.CloseSessionRequest;
import com.alang.dto.chat.CreateSessionRequest;
import com.alang.dto.chat.NoteFromSessionRequest;
import com.alang.dto.chat.SessionDetailResponse;
import com.alang.dto.chat.SessionResponse;
import com.alang.dto.chat.UpdateSessionTitleRequest;
import com.alang.dto.note.NoteDto;

import java.util.List;

/**
 * Chat orchestration service.
 *
 * RESPONSIBILITIES:
 * - Manage chat sessions (create, list)
 * - Handle incoming chat messages within a session
 * - Call LLMService to generate replies
 * - Save messages to database (RecentMessage), linked to their session
 * - On explicit user request: call LLMService to create or update a note from the session
 *
 * ARCHITECTURAL NOTE:
 * Notes are NOT auto-extracted on every message. The user explicitly triggers note
 * creation via createNoteFromSession() once they are satisfied with the conversation.
 * This service does NOT call LLM API directly — all LLM interaction goes through LLMService.
 */
public interface ChatService {

    /**
     * Create a new chat session for the given user.
     * A session represents a single-topic conversation scoped to a learning language.
     *
     * @param request Session creation request (learning language + optional title)
     * @param userId  Authenticated user ID
     * @return Created session details
     */
    SessionResponse createSession(CreateSessionRequest request, String userId);

    /**
     * Get all active sessions for a user, including their full message history.
     * Used to restore in-progress conversations when the app starts.
     *
     * @param userId Authenticated user ID
     * @return List of active session details with messages, newest first
     */
    List<SessionDetailResponse> getActiveSessions(String userId);

    /**
     * Process a chat message within a session.
     *
     * FLOW:
     * 1. Validate session ownership and that it is still active
     * 2. Save user's message to RecentMessage (linked to session)
     * 3. Call LLMService.generateReply() — context is session-scoped
     * 4. Strip ---TOPICS--- block from the reply
     * 5. Save assistant's reply to RecentMessage (linked to session)
     * 6. Extract any topic suggestions from the ---TOPICS--- block
     * 7. Return reply + suggested topics (no auto note creation)
     *
     * @param request ChatMessageRequest with sessionId and message
     * @param userId  Authenticated user ID
     * @return Reply + optional topic suggestions for the escape-hatch chips
     */
    ChatMessageResponse sendMessage(ChatMessageRequest request, String userId);

    /**
     * Explicitly create a note from the session's full conversation history.
     * Called when the user presses "Create Note".
     *
     * @param sessionId Session to create the note from
     * @param request   Optional topicFocus (from a topic chip)
     * @param userId    Authenticated user ID
     * @return The newly created NoteDto
     */
    NoteDto createNoteFromSession(String sessionId, NoteFromSessionRequest request, String userId);

    /**
     * Update an existing note using the session's full conversation as context.
     * Called when the user presses "Update Note" after additional follow-up questions.
     * The LLM receives the current note content as a base to build on.
     *
     * @param sessionId Session containing the conversation context
     * @param noteId    ID of the note to update
     * @param request   Optional topicFocus to narrow the update
     * @param userId    Authenticated user ID
     * @return The updated NoteDto
     */
    NoteDto updateNoteFromSession(String sessionId, String noteId, NoteFromSessionRequest request, String userId);

    /**
     * Close a session, preventing further messages from being sent.
     *
     * If request.force is false (default), checks whether a note has been created from the session.
     * If no note exists, returns early with noteCreated=false without closing — the frontend should
     * then prompt the user for confirmation and re-call with force=true.
     *
     * If request.force is true, skips the check and closes immediately.
     *
     * @param sessionId Session to close
     * @param request   Close options (force flag)
     * @param userId    Authenticated user ID
     * @return Updated session details (check noteCreated field when not force)
     */
    SessionResponse closeSession(String sessionId, CloseSessionRequest request, String userId);

    /**
     * Update the title of a chat session.
     *
     * @param sessionId Session to rename
     * @param request   New title
     * @param userId    Authenticated user ID
     * @return Updated session details
     */
    SessionResponse updateSessionTitle(String sessionId, UpdateSessionTitleRequest request, String userId);

    /**
     * Get conversation history for a user in a specific language.
     * TODO: Implement in Week 4 (summarization)
     */
    ChatHistoryDto getHistory(String userId, String language, int limit);

    /**
     * Check if summarization should be triggered for a session.
     * TODO: Implement in Week 4
     */
    boolean shouldTriggerSummarization(String sessionId);

    /**
     * Trigger conversation summarization for a session.
     * TODO: Implement in Week 4
     */
    void triggerSummarization(String sessionId);
}
