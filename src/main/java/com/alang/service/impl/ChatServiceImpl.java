package com.alang.service.impl;

import com.alang.dto.chat.ChatHistoryDto;
import com.alang.dto.chat.ChatMessageRequest;
import com.alang.dto.chat.ChatMessageResponse;
import com.alang.dto.chat.CloseSessionRequest;
import com.alang.dto.chat.CreateSessionRequest;
import com.alang.dto.chat.MessageDto;
import com.alang.dto.chat.NoteFromSessionRequest;
import com.alang.dto.chat.SessionDetailResponse;
import com.alang.dto.chat.SessionResponse;
import com.alang.dto.chat.UpdateSessionTitleRequest;
import com.alang.dto.note.NoteDto;
import com.alang.dto.note.UpdateNoteRequest;
import com.alang.entity.ChatSession;
import com.alang.entity.Language;
import com.alang.entity.RecentMessage;
import com.alang.entity.RoleType;
import com.alang.entity.SessionStatus;
import com.alang.entity.User;
import com.alang.exception.UnauthorizedException;
import com.alang.exception.UserNotFoundException;
import com.alang.repository.ChatSessionRepository;
import com.alang.repository.LanguageRepository;
import com.alang.repository.RecentMessageRepository;
import com.alang.repository.UserRepository;
import com.alang.service.ChatService;
import com.alang.service.LLMService;
import com.alang.service.NoteService;
import com.alang.service.PromptTemplates;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final LLMService llmService;
    private final NoteService noteService;
    private final UserRepository userRepository;
    private final LanguageRepository languageRepository;
    private final RecentMessageRepository recentMessageRepository;
    private final ChatSessionRepository chatSessionRepository;
    private final ObjectMapper objectMapper;

    // ---- Session management ----

    @Override
    @Transactional
    public SessionResponse createSession(CreateSessionRequest request, String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        Language teachingLanguage = languageRepository.findById(user.getAppLanguageCode())
                .orElseThrow(() -> new IllegalStateException("App language not found: " + user.getAppLanguageCode()));
        Language learningLanguage = languageRepository.findById(request.getLanguage())
                .orElseThrow(() -> new IllegalArgumentException("Language not supported: " + request.getLanguage()));

        ChatSession session = new ChatSession();
        session.setUser(user);
        session.setTeachingLanguage(teachingLanguage);
        session.setLearningLanguage(learningLanguage);
        session.setStatus(SessionStatus.active);
        session.setTitle(request.getTitle());

        ChatSession saved = chatSessionRepository.save(session);
        log.info("Created chat session: id={}, userId={}, language={}", saved.getId(), userId, request.getLanguage());

        return toSessionResponse(saved, 0);
    }

    @Override
    public List<SessionDetailResponse> getActiveSessions(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        List<ChatSession> sessions = chatSessionRepository.findByUserAndStatusOrderByCreatedAtDesc(user, SessionStatus.active);

        return sessions.stream()
                .map(session -> {
                    List<RecentMessage> messages = recentMessageRepository.findBySessionOrderByCreatedAtAsc(session);
                    return toSessionDetailResponse(session, messages);
                })
                .toList();
    }

    // ---- Messaging ----

    @Override
    @Transactional
    public ChatMessageResponse sendMessage(ChatMessageRequest request, String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        ChatSession session = chatSessionRepository.findByIdAndUser(request.getSessionId(), user)
                .orElseThrow(() -> new UnauthorizedException("Session not found or access denied"));

        if (session.getStatus() == SessionStatus.closed) {
            throw new IllegalStateException("Cannot send messages to a closed session");
        }

        Language teachingLanguage = session.getTeachingLanguage();
        Language learningLanguage = session.getLearningLanguage();

        // 1. Save user's message linked to session
        RecentMessage userMessage = new RecentMessage();
        userMessage.setUser(user);
        userMessage.setTeachingLanguage(teachingLanguage);
        userMessage.setLearningLanguage(learningLanguage);
        userMessage.setSession(session);
        userMessage.setRole(RoleType.user);
        userMessage.setContent(request.getMessage());
        recentMessageRepository.save(userMessage);

        // 2. Call LLM — session context is resolved inside LLMServiceImpl via sessionId
        LLMService.LLMResponse llmResponse = llmService.generateReply(request, userId);
        llmService.recordTokenUsage(userId, llmResponse.getTokenUsage());

        String rawReply = llmResponse.getReply();

        // 3. Strip ---TOPICS--- block for clean user-facing reply
        String cleanReply = PromptTemplates.stripTopicsBlock(rawReply);

        // 4. Save assistant's reply (clean version) linked to session
        RecentMessage assistantMessage = new RecentMessage();
        assistantMessage.setUser(user);
        assistantMessage.setTeachingLanguage(teachingLanguage);
        assistantMessage.setLearningLanguage(learningLanguage);
        assistantMessage.setSession(session);
        assistantMessage.setRole(RoleType.assistant);
        assistantMessage.setContent(cleanReply);
        assistantMessage.setModelUsed(llmResponse.getModelUsed());
        assistantMessage.setTokenCount(llmResponse.getTokenUsage().getTotalTokens());
        recentMessageRepository.save(assistantMessage);

        // 5. Extract topic suggestions (non-null only for broad questions covering 3+ topics)
        List<String> suggestedTopics = PromptTemplates.extractTopics(rawReply, objectMapper);

        log.info("Chat message processed: sessionId={}, userId={}, model={}, topics={}",
                request.getSessionId(), userId, llmResponse.getModelUsed(), suggestedTopics.size());

        ChatMessageResponse response = new ChatMessageResponse();
        response.setReply(cleanReply);
        response.setTokenUsage(llmResponse.getTokenUsage());
        response.setModelUsed(llmResponse.getModelUsed());
        response.setSuggestedTopics(suggestedTopics.isEmpty() ? null : suggestedTopics);
        return response;
    }

    // ---- Note creation / update from session ----

    @Override
    @Transactional
    public NoteDto createNoteFromSession(String sessionId, NoteFromSessionRequest request, String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        ChatSession session = chatSessionRepository.findByIdAndUser(sessionId, user)
                .orElseThrow(() -> new UnauthorizedException("Session not found or access denied"));

        List<RecentMessage> messages = recentMessageRepository.findBySessionOrderByCreatedAtAsc(session);
        if (messages.isEmpty()) {
            throw new IllegalStateException("Cannot create a note from an empty session");
        }

        NoteDto generatedNote = llmService.generateNoteFromConversation(
                toMessageContext(messages),
                request.getTopicFocus(),
                null, // creating new note, not updating
                session.getLearningLanguage(),
                session.getTeachingLanguage(),
                userId);

        NoteDto savedNote = noteService.createNote(generatedNote, userId);

        session.setNoteCreated(true);
        chatSessionRepository.save(session);

        log.info("Note created from session: sessionId={}, noteId={}, userId={}, topic={}",
                sessionId, savedNote.getId(), userId, request.getTopicFocus());

        return savedNote;
    }

    @Override
    @Transactional
    public NoteDto updateNoteFromSession(String sessionId, String noteId, NoteFromSessionRequest request, String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        ChatSession session = chatSessionRepository.findByIdAndUser(sessionId, user)
                .orElseThrow(() -> new UnauthorizedException("Session not found or access denied"));

        // Verify note ownership (throws NoteNotFoundException / UnauthorizedException if invalid)
        NoteDto existingNote = noteService.getNote(noteId, userId);

        List<RecentMessage> messages = recentMessageRepository.findBySessionOrderByCreatedAtAsc(session);
        if (messages.isEmpty()) {
            throw new IllegalStateException("Cannot update a note from an empty session");
        }

        NoteDto generatedNote = llmService.generateNoteFromConversation(
                toMessageContext(messages),
                request.getTopicFocus(),
                existingNote, // LLM uses this as the base to build on
                session.getLearningLanguage(),
                session.getTeachingLanguage(),
                userId);

        UpdateNoteRequest updateRequest = new UpdateNoteRequest();
        updateRequest.setTitle(generatedNote.getTitle());
        updateRequest.setSummary(generatedNote.getSummary());
        updateRequest.setNoteContent(generatedNote.getNoteContent());
        updateRequest.setStructuredContent(generatedNote.getStructuredContent());
        updateRequest.setTags(generatedNote.getTags());

        NoteDto updatedNote = noteService.updateNote(noteId, updateRequest, userId, false);

        log.info("Note updated from session: sessionId={}, noteId={}, userId={}, topic={}",
                sessionId, noteId, userId, request.getTopicFocus());

        return updatedNote;
    }

    // ---- Session lifecycle ----

    @Override
    @Transactional
    public SessionResponse closeSession(String sessionId, CloseSessionRequest request, String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // Check whether a note has been created for this session before proceeding.
        // If force=false and no note exists, return early so the frontend can prompt the user.
        if (!request.isForce() && !chatSessionRepository.existsByIdAndUserAndNoteCreatedTrue(sessionId, user)) {
            ChatSession session = chatSessionRepository.findByIdAndUser(sessionId, user)
                    .orElseThrow(() -> new UnauthorizedException("Session not found or access denied"));
            log.info("Session close blocked: note not yet created, force=false. sessionId={}, userId={}", sessionId, userId);
            return toSessionResponse(session, (int) recentMessageRepository.countBySession(session));
        }

        ChatSession session = chatSessionRepository.findByIdAndUser(sessionId, user)
                .orElseThrow(() -> new UnauthorizedException("Session not found or access denied"));

        if (session.getStatus() == SessionStatus.closed) {
            throw new IllegalStateException("Session is already closed");
        }

        session.setStatus(SessionStatus.closed);
        session.setClosedAt(java.time.LocalDateTime.now());
        ChatSession saved = chatSessionRepository.save(session);

        log.info("Closed session: sessionId={}, userId={}", sessionId, userId);
        return toSessionResponse(saved, (int) recentMessageRepository.countBySession(saved));
    }


    @Override
    @Transactional
    public SessionResponse updateSessionTitle(String sessionId, UpdateSessionTitleRequest request, String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        ChatSession session = chatSessionRepository.findByIdAndUser(sessionId, user)
                .orElseThrow(() -> new UnauthorizedException("Session not found or access denied"));

        session.setTitle(request.getTitle());
        ChatSession saved = chatSessionRepository.save(session);

        log.info("Updated session title: sessionId={}, userId={}", sessionId, userId);
        return toSessionResponse(saved, (int) recentMessageRepository.countBySession(saved));
    }

    // ---- Week 4 stubs ----

    @Override
    public ChatHistoryDto getHistory(String userId, String language, int limit) {
        // TODO: Implement history retrieval (Week 4)
        throw new UnsupportedOperationException("TODO: Implement history retrieval");
    }

    @Override
    public boolean shouldTriggerSummarization(String sessionId) {
        // TODO: Implement summarization trigger logic (Week 4)
        return false;
    }

    @Override
    public void triggerSummarization(String sessionId) {
        // TODO: Implement summarization (Week 4)
        throw new UnsupportedOperationException("TODO: Implement summarization");
    }

    // ---- Private helpers ----

    private List<Map<String, String>> toMessageContext(List<RecentMessage> messages) {
        return messages.stream()
                .map(m -> Map.of("role", m.getRole().name(), "content", m.getContent()))
                .toList();
    }

    private SessionDetailResponse toSessionDetailResponse(ChatSession session, List<RecentMessage> messages) {
        SessionDetailResponse response = new SessionDetailResponse();
        response.setId(session.getId());
        response.setLearningLanguage(session.getLearningLanguage().getCode());
        response.setTeachingLanguage(session.getTeachingLanguage().getCode());
        response.setStatus(session.getStatus().name());
        response.setTitle(session.getTitle());
        response.setCreatedAt(session.getCreatedAt());
        response.setUpdatedAt(session.getUpdatedAt());
        response.setClosedAt(session.getClosedAt());
        response.setNoteCreated(Boolean.TRUE.equals(session.getNoteCreated()));
        response.setMessages(messages.stream().map(m -> {
            MessageDto dto = new MessageDto();
            dto.setRole(m.getRole().name());
            dto.setContent(m.getContent());
            dto.setCreatedAt(m.getCreatedAt());
            return dto;
        }).toList());
        return response;
    }

    private SessionResponse toSessionResponse(ChatSession session, int messageCount) {
        SessionResponse dto = new SessionResponse();
        dto.setId(session.getId());
        dto.setLearningLanguage(session.getLearningLanguage().getCode());
        dto.setTeachingLanguage(session.getTeachingLanguage().getCode());
        dto.setStatus(session.getStatus().name());
        dto.setTitle(session.getTitle());
        dto.setCreatedAt(session.getCreatedAt());
        dto.setUpdatedAt(session.getUpdatedAt());
        dto.setClosedAt(session.getClosedAt());
        dto.setMessageCount(messageCount);
        dto.setNoteCreated(Boolean.TRUE.equals(session.getNoteCreated()));
        return dto;
    }
}
