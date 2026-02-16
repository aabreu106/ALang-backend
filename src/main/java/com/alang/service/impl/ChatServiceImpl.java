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
import com.alang.service.ChatService;
import com.alang.service.LLMService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final LLMService llmService;
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

        // 3. Save assistant's reply
        RecentMessage assistantMessage = new RecentMessage();
        assistantMessage.setUser(user);
        assistantMessage.setTeachingLanguage(teachingLanguage);
        assistantMessage.setLearningLanguage(learningLanguage);
        assistantMessage.setRole("assistant");
        assistantMessage.setContent(llmResponse.getReply());
        assistantMessage.setModelUsed(llmResponse.getModelUsed());
        assistantMessage.setTokenCount(llmResponse.getTokenUsage().getTotalTokens());
        recentMessageRepository.save(assistantMessage);

        log.info("Chat exchange saved: userId={}, language={}, model={}",
                userId, request.getLanguage(), llmResponse.getModelUsed());

        // 4. Build response
        ChatMessageResponse response = new ChatMessageResponse();
        response.setReply(llmResponse.getReply());
        response.setTokenUsage(llmResponse.getTokenUsage());
        response.setModelUsed(llmResponse.getModelUsed());
        response.setCreatedNotes(new ArrayList<>()); // TODO: Note extraction (Week 3)
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
