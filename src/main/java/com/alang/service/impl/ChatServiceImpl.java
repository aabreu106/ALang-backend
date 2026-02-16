package com.alang.service.impl;

import com.alang.dto.chat.ChatHistoryDto;
import com.alang.dto.chat.ChatMessageRequest;
import com.alang.dto.chat.ChatMessageResponse;
import com.alang.entity.Language;
import com.alang.entity.RecentMessage;
import com.alang.entity.User;
import com.alang.exception.UserNotFoundException;
import com.alang.repository.LanguageRepository;
import com.alang.repository.RecentMessageRepository;
import com.alang.repository.UserRepository;
import com.alang.dto.note.NoteDto;
import com.alang.dto.note.NotePreviewDto;
import com.alang.service.ChatService;
import com.alang.service.LLMService;
import com.alang.service.NoteService;
import com.alang.service.PromptTemplates;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final LLMService llmService;
    private final NoteService noteService;
    private final UserRepository userRepository;
    private final LanguageRepository languageRepository;
    private final RecentMessageRepository recentMessageRepository;

    @Override
    @Transactional
    public ChatMessageResponse sendMessage(ChatMessageRequest request, String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        Language teachingLanguage = languageRepository.findById(user.getAppLanguageCode())
                .orElseThrow(() -> new IllegalStateException("Teaching language not found: " + user.getAppLanguageCode()));
        Language learningLanguage = languageRepository.findById(request.getLanguage())
                .orElseThrow(() -> new IllegalArgumentException("Language not supported: " + request.getLanguage()));

        // 1. Save user's message
        RecentMessage userMessage = new RecentMessage();
        userMessage.setUser(user);
        userMessage.setTeachingLanguage(teachingLanguage);
        userMessage.setLearningLanguage(learningLanguage);
        userMessage.setRole("user");
        userMessage.setContent(request.getMessage());
        recentMessageRepository.save(userMessage);

        // 2. Call LLM for reply
        LLMService.LLMResponse llmResponse = llmService.generateReply(request, userId);

        // 2b. Record token usage against user's daily budget
        llmService.recordTokenUsage(userId, llmResponse.getTokenUsage());

        // 3. Strip notes block from user-facing reply
        String rawReply = llmResponse.getReply();
        String cleanReply = PromptTemplates.stripNotesBlock(rawReply);

        // 4. Save assistant's reply (clean version, without notes JSON)
        RecentMessage assistantMessage = new RecentMessage();
        assistantMessage.setUser(user);
        assistantMessage.setTeachingLanguage(teachingLanguage);
        assistantMessage.setLearningLanguage(learningLanguage);
        assistantMessage.setRole("assistant");
        assistantMessage.setContent(cleanReply);
        assistantMessage.setModelUsed(llmResponse.getModelUsed());
        assistantMessage.setTokenCount(llmResponse.getTokenUsage().getTotalTokens());
        recentMessageRepository.save(assistantMessage);

        // 5. Extract and persist notes from LLM response
        List<NoteDto> extractedNotes = llmService.extractNotes(rawReply, request.getLanguage());
        List<NoteDto> savedNotes = extractedNotes.isEmpty()
                ? List.of()
                : noteService.createNotes(extractedNotes, userId);

        log.info("Chat exchange saved: userId={}, language={}, model={}, notes={}",
                userId, request.getLanguage(), llmResponse.getModelUsed(), savedNotes.size());

        // 6. Build response
        List<NotePreviewDto> notePreviews = savedNotes.stream().map(n -> {
            NotePreviewDto preview = new NotePreviewDto();
            preview.setId(n.getId());
            preview.setType(n.getType());
            preview.setTitle(n.getTitle());
            preview.setIntervalDays(1);
            return preview;
        }).toList();

        ChatMessageResponse response = new ChatMessageResponse();
        response.setReply(cleanReply);
        response.setTokenUsage(llmResponse.getTokenUsage());
        response.setModelUsed(llmResponse.getModelUsed());
        response.setCreatedNotes(notePreviews);
        return response;
    }

    @Override
    public ChatHistoryDto getHistory(String userId, String language, int limit) {
        // TODO: Implement history retrieval (Week 4)
        throw new UnsupportedOperationException("TODO: Implement history retrieval");
    }

    @Override
    public boolean shouldTriggerSummarization(String userId, String language) {
        // TODO: Implement summarization trigger logic (Week 4)
        return false;
    }

    @Override
    public void triggerSummarization(String userId, String language) {
        // TODO: Implement summarization (Week 4)
        throw new UnsupportedOperationException("TODO: Implement summarization");
    }
}
