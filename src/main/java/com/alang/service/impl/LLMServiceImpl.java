package com.alang.service.impl;

import com.alang.config.LLMProperties;
import com.alang.dto.chat.ChatMessageRequest;
import com.alang.dto.chat.TokenUsageDto;
import com.alang.dto.note.NoteDto;
import com.alang.entity.ConversationSummary;
import com.alang.entity.Language;
import com.alang.entity.RecentMessage;
import com.alang.entity.User;
import com.alang.entity.UserTier;
import com.alang.exception.LLMProviderException;
import com.alang.exception.UserNotFoundException;
import com.alang.repository.ConversationSummaryRepository;
import com.alang.repository.LanguageRepository;
import com.alang.repository.RecentMessageRepository;
import com.alang.repository.UserRepository;
import com.alang.service.LLMService;
import com.alang.service.PromptTemplates;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class LLMServiceImpl implements LLMService {

    private final WebClient llmWebClient;
    private final LLMProperties llmProperties;
    private final PromptTemplates promptTemplates;
    private final UserRepository userRepository;
    private final LanguageRepository languageRepository;
    private final RecentMessageRepository recentMessageRepository;
    private final ConversationSummaryRepository conversationSummaryRepository;

    private static final int MAX_CONTEXT_SUMMARIES = 3;
    private static final int MAX_CONTEXT_MESSAGES = 10;

    // The message array sent to the LLM looks like: [system prompt] → [summary context] → [recent msg 1] → [recent msg 2] → ... → [new user message]
    @Override
    public LLMResponse generateReply(ChatMessageRequest request, String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        String model = selectModelForUser(request, user);
        String systemPrompt = promptTemplates.buildChatSystemPrompt(
                user.getAppLanguageCode(), request.getLanguage());

        List<Map<String, String>> messages = new ArrayList<>();

        // Include conversation context if requested (summaries + recent messages)
        if (request.getIncludeContext()) {
            messages.addAll(buildConversationContext(user, request.getLanguage()));
        }

        messages.add(Map.of("role", "user", "content", request.getMessage()));

        LLMApiResponse apiResponse = callLLMApi(model, systemPrompt, messages);

        TokenUsageDto tokenUsage = apiResponse.tokenUsage();
        log.info("LLM call completed: model={}, tokens={}", model, tokenUsage.getTotalTokens());

        return new LLMResponse(apiResponse.content(), model, tokenUsage);
    }

    @Override
    public List<NoteDto> extractNotes(String llmResponse, String language) {
        // TODO: Implement note extraction (Week 3)
        throw new UnsupportedOperationException("TODO: Implement note extraction");
    }

    @Override
    public ConversationSummary generateSummary(List<RecentMessage> recentMessages, String userId, String language) {
        // TODO: Implement conversation summarization (Week 4)
        throw new UnsupportedOperationException("TODO: Implement conversation summarization");
    }

    @Override
    public int countTokens(String text, String model) {
        // TODO: Implement token counting (Week 2, Task 3)
        throw new UnsupportedOperationException("TODO: Implement token counting");
    }

    @Override
    public boolean checkTokenBudget(String userId, int estimatedTokens) {
        // TODO: Implement token budget checking (Week 2, Task 3)
        throw new UnsupportedOperationException("TODO: Implement token budget checking");
    }

    @Override
    public void recordTokenUsage(String userId, TokenUsageDto tokenUsage) {
        // TODO: Implement token usage recording (Week 2, Task 3)
        throw new UnsupportedOperationException("TODO: Implement token usage recording");
    }

    @Override
    public String selectModel(ChatMessageRequest request, String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        return selectModelForUser(request, user);
    }

    private String selectModelForUser(ChatMessageRequest request, User user) {
        String intent = request.getIntent() != null ? request.getIntent() : "";
        String depth = request.getDepth() != null ? request.getDepth() : "normal";
        boolean isEducational = intent.equals("grammar_explanation")
                || intent.equals("correction_request")
                || intent.equals("vocabulary");

        LLMProperties.Models models = llmProperties.getModels();

        if (user.getTier() == UserTier.pro) {
            if (depth.equals("detailed") || isEducational) {
                return models.getPremium();
            }
            return models.getStandard();
        }

        // Free tier: standard only for detailed educational requests, cheap otherwise
        if (depth.equals("detailed") && isEducational) {
            return models.getStandard();
        }
        return models.getCheap();
    }

    // ---- Private helpers ----

    /**
     * Build conversation context from summaries and recent messages.
     * Returns a list of message maps ready to be sent to the LLM.
     */
    private List<Map<String, String>> buildConversationContext(User user, String learningLanguageCode) {
        Language learningLanguage = languageRepository.findById(learningLanguageCode).orElse(null);
        if (learningLanguage == null) {
            log.warn("Language not found: {}, skipping context", learningLanguageCode);
            return List.of();
        }

        List<Map<String, String>> contextMessages = new ArrayList<>();

        // Load recent summaries (newest first) and prepend as a system-level context block
        List<ConversationSummary> summaries = conversationSummaryRepository
                .findByUserAndLearningLanguageOrderByCreatedAtDesc(
                        user, learningLanguage, PageRequest.of(0, MAX_CONTEXT_SUMMARIES));

        if (!summaries.isEmpty()) {
            StringBuilder summaryBlock = new StringBuilder("Previous conversation context:\n");
            // Reverse so oldest summary comes first (chronological order)
            for (int i = summaries.size() - 1; i >= 0; i--) {
                summaryBlock.append("- ").append(summaries.get(i).getSummaryText()).append("\n");
            }
            contextMessages.add(Map.of("role", "system", "content", summaryBlock.toString()));
        }

        // Load recent messages (oldest first for chronological order)
        List<RecentMessage> recentMessages = recentMessageRepository
                .findByUserAndLearningLanguageOrderByCreatedAtAsc(
                        user, learningLanguage, PageRequest.of(0, MAX_CONTEXT_MESSAGES));

        for (RecentMessage msg : recentMessages) {
            contextMessages.add(Map.of("role", msg.getRole(), "content", msg.getContent()));
        }

        return contextMessages;
    }

    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_BACKOFF_MS = 1000;

    /**
     * Call the LLM API using the chat completions format (compatible with Ollama, OpenAI, etc.).
     * Retries on transient errors (429, 5xx) with exponential backoff.
     */
    private LLMApiResponse callLLMApi(String model, String systemPrompt, List<Map<String, String>> messages) {
        List<Map<String, String>> fullMessages = new ArrayList<>();
        fullMessages.add(Map.of("role", "system", "content", systemPrompt));
        fullMessages.addAll(messages);

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", fullMessages,
                "max_tokens", llmProperties.getTokenLimits().getPerRequestMax()
        );

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                Map<String, Object> response = llmWebClient.post()
                        .uri("/chat/completions")
                        .bodyValue(requestBody)
                        .retrieve()
                        .bodyToMono(Map.class)
                        .block();

                return parseApiResponse(response);
            } catch (WebClientResponseException e) {
                int status = e.getStatusCode().value();

                if (!isRetryable(status)) {
                    log.error("LLM API non-retryable error: status={}, body={}", status, e.getResponseBodyAsString());
                    throw new LLMProviderException("LLM API returned " + status, e);
                }

                if (attempt == MAX_RETRIES) {
                    log.error("LLM API failed after {} attempts: status={}", MAX_RETRIES, status);
                    throw new LLMProviderException("LLM API returned " + status + " after " + MAX_RETRIES + " attempts", e);
                }

                long backoff = INITIAL_BACKOFF_MS * (1L << (attempt - 1));
                log.warn("LLM API retryable error: status={}, attempt={}/{}, retrying in {}ms", status, attempt, MAX_RETRIES, backoff);
                sleep(backoff);
            } catch (Exception e) {
                if (attempt == MAX_RETRIES) {
                    log.error("LLM API call failed after {} attempts", MAX_RETRIES, e);
                    throw new LLMProviderException("Failed to call LLM API after " + MAX_RETRIES + " attempts", e);
                }

                long backoff = INITIAL_BACKOFF_MS * (1L << (attempt - 1));
                log.warn("LLM API call error, attempt={}/{}, retrying in {}ms: {}", attempt, MAX_RETRIES, backoff, e.getMessage());
                sleep(backoff);
            }
        }

        // Unreachable, but the compiler needs it
        throw new LLMProviderException("LLM API call failed unexpectedly");
    }

    private boolean isRetryable(int httpStatus) {
        return httpStatus == 429 || httpStatus >= 500;
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LLMProviderException("LLM API call interrupted", e);
        }
    }

    /**
     * Parse the chat completions response.
     */
    @SuppressWarnings("unchecked")
    private LLMApiResponse parseApiResponse(Map<String, Object> response) {
        if (response == null || !response.containsKey("choices")) {
            throw new LLMProviderException("Invalid LLM response: missing 'choices'");
        }

        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        if (choices.isEmpty()) {
            throw new LLMProviderException("Invalid LLM response: empty 'choices'");
        }

        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        String content = (String) message.get("content");

        TokenUsageDto tokenUsage = new TokenUsageDto();
        if (response.containsKey("usage")) {
            Map<String, Object> usage = (Map<String, Object>) response.get("usage");
            tokenUsage.setPromptTokens(toInt(usage.get("prompt_tokens")));
            tokenUsage.setCompletionTokens(toInt(usage.get("completion_tokens")));
            tokenUsage.setTotalTokens(toInt(usage.get("total_tokens")));
        }

        return new LLMApiResponse(content, tokenUsage);
    }

    private int toInt(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return 0;
    }

    private record LLMApiResponse(String content, TokenUsageDto tokenUsage) {}
}
