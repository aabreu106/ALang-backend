package com.alang.service.impl;

import com.alang.config.LLMProperties;
import com.alang.dto.chat.ChatMessageRequest;
import com.alang.dto.chat.TokenUsageDto;
import com.alang.dto.note.NoteDto;
import com.alang.entity.ChatSession;
import com.alang.entity.ConversationSummary;
import com.alang.entity.Language;
import com.alang.entity.RecentMessage;
import com.alang.entity.User;
import com.alang.entity.UserTier;
import com.alang.exception.LLMProviderException;
import com.alang.exception.RateLimitExceededException;
import com.alang.exception.UnauthorizedException;
import com.alang.exception.UserNotFoundException;
import com.alang.repository.ChatSessionRepository;
import com.alang.repository.ConversationSummaryRepository;
import com.alang.repository.LanguageRepository;
import com.alang.repository.RecentMessageRepository;
import com.alang.repository.UserRepository;
import com.alang.entity.NoteType;
import com.alang.service.LLMService;
import com.alang.service.PromptTemplates;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDate;
import com.alang.dto.note.NoteTagDto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private final ChatSessionRepository chatSessionRepository;

    private final ObjectMapper objectMapper;

    private static final int MAX_CONTEXT_SUMMARIES = 3;
    private static final int MAX_CONTEXT_MESSAGES = 10;
    private static final Set<String> VALID_NOTE_TYPES = Set.of("vocab", "grammar", "exception", "other");
    private static final int MAX_TITLE_LENGTH = 60;

    // The message array sent to the LLM looks like: [system prompt] → [summary context] → [recent msg 1] → [recent msg 2] → ... → [new user message]
    @Override
    public LLMResponse generateReply(ChatMessageRequest request, String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // Resolve session to get language context (language is no longer in the request body)
        ChatSession session = chatSessionRepository.findByIdAndUser(request.getSessionId(), user)
                .orElseThrow(() -> new UnauthorizedException("Session not found or access denied"));

        String model = selectModelForUser(user);

        Language appLanguage = languageRepository.findById(user.getAppLanguageCode())
                .orElseThrow(() -> new IllegalStateException("App language not found: " + user.getAppLanguageCode()));
        Language targetLanguage = session.getLearningLanguage();

        String systemPrompt = promptTemplates.buildChatSystemPrompt(
                appLanguage.getName(), targetLanguage.getName());

        List<Map<String, String>> messages = new ArrayList<>();

        // Include session-scoped conversation context if requested
        if (request.getIncludeContext()) {
            messages.addAll(buildConversationContext(session));
        }

        messages.add(Map.of("role", "user", "content", request.getMessage()));

        // Estimate token usage and check budget before calling LLM
        int estimatedTokens = countTokens(systemPrompt, model);
        for (Map<String, String> msg : messages) {
            estimatedTokens += countTokens(msg.get("content"), model);
        }
        if (!checkTokenBudget(userId, estimatedTokens)) {
            long remaining = Math.max(0, getDailyLimit(user) - user.getTotalDailyTokensUsed());
            throw new RateLimitExceededException(
                    "Not enough tokens remaining for this request. Estimated cost: "
                            + estimatedTokens + " tokens. Please try again tomorrow.",
                    remaining);
        }

        LLMApiResponse apiResponse = callLLMApi(model, systemPrompt, messages);

        TokenUsageDto tokenUsage = apiResponse.tokenUsage();
        log.info("LLM call completed: model={}, tokens={}", model, tokenUsage.getTotalTokens());

        return new LLMResponse(apiResponse.content(), model, tokenUsage);
    }

    @Override
    public NoteDto generateNoteFromConversation(
            List<Map<String, String>> sessionMessages,
            String topicFocus,
            NoteDto existingNote,
            Language learningLanguage,
            Language appLanguage,
            String userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        String model = selectModelForUser(user);

        String systemPrompt = promptTemplates.buildNoteCreationSystemPrompt(
                appLanguage.getName(), learningLanguage.getName());

        String userPrompt;
        if (existingNote != null) {
            String existingNoteJson;
            try {
                existingNoteJson = objectMapper.writeValueAsString(existingNote);
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize existing note for update prompt, proceeding without it");
                existingNoteJson = "{}";
            }
            userPrompt = promptTemplates.buildNoteUpdateUserPrompt(sessionMessages, existingNoteJson, topicFocus);
        } else {
            userPrompt = promptTemplates.buildNoteCreationUserPrompt(sessionMessages, topicFocus);
        }

        // Token budget check
        int estimatedTokens = countTokens(systemPrompt, model) + countTokens(userPrompt, model);
        if (!checkTokenBudget(userId, estimatedTokens)) {
            long remaining = Math.max(0, getDailyLimit(user) - user.getTotalDailyTokensUsed());
            throw new RateLimitExceededException(
                    "Not enough tokens remaining for note creation. Please try again tomorrow.", remaining);
        }

        // Single user message containing the full conversation transcript
        List<Map<String, String>> messages = List.of(Map.of("role", "user", "content", userPrompt));

        LLMApiResponse apiResponse = callLLMApi(model, systemPrompt, messages);
        recordTokenUsage(userId, apiResponse.tokenUsage());

        NoteDto note = parseNoteCreationResponse(apiResponse.content(), learningLanguage.getCode());
        note.setLearningLanguage(learningLanguage.getCode());
        note.setTeachingLanguage(appLanguage.getCode());

        log.info("Generated note from conversation: userId={}, language={}, topic={}, model={}",
                userId, learningLanguage.getCode(), topicFocus, model);

        return note;
    }

    private NoteDto parseNoteCreationResponse(String json, String language) {
        try {
            JsonNode root = objectMapper.readTree(json.trim());
            NoteDto note = parseNoteNode(root, language);
            if (note == null) {
                throw new LLMProviderException("LLM returned an invalid note structure for language=" + language);
            }
            return note;
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse note creation JSON for language={}: {}", language, e.getMessage());
            throw new LLMProviderException("LLM returned unparseable JSON for note creation", e);
        }
    }

    private static final Set<String> VALID_TAG_CATEGORIES = Set.of("topic", "formality", "difficulty", "function");

    // Validate and parse a single note node from the extracted JSON. Returns null if the note is invalid and should be skipped.
    private NoteDto parseNoteNode(JsonNode node, String language) {
        String type = node.path("type").asText("").trim().toLowerCase();
        String title = node.path("title").asText("").trim();
        String summary = node.path("summary").asText("").trim();
        String content = node.path("content").asText("").trim();

        if (!VALID_NOTE_TYPES.contains(type)) {
            log.warn("Invalid note type '{}', skipping note with title '{}'", type, title);
            return null;
        }

        if (title.isEmpty()) {
            log.warn("Note missing title, skipping");
            return null;
        }

        if (title.length() > MAX_TITLE_LENGTH) {
            title = title.substring(0, MAX_TITLE_LENGTH);
        }

        NoteDto note = new NoteDto();
        note.setType(NoteType.valueOf(type));
        note.setLearningLanguage(language);
        note.setTitle(title);
        note.setSummary(summary.isEmpty() ? null : summary);
        note.setNoteContent(content.isEmpty() ? null : content);

        // Parse structured content (type-specific fields)
        JsonNode structuredNode = node.path("structured");
        if (structuredNode.isObject()) {
            Map<String, Object> structured = objectMapper.convertValue(structuredNode, objectMapper.getTypeFactory()
                    .constructMapType(HashMap.class, String.class, Object.class));
            note.setStructuredContent(structured);
        }

        // Parse tags
        JsonNode tagsNode = node.path("tags");
        if (tagsNode.isArray() && !tagsNode.isEmpty()) {
            List<NoteTagDto> tags = new ArrayList<>();
            for (JsonNode tagNode : tagsNode) {
                String category = tagNode.path("category").asText("").trim().toLowerCase();
                String value = tagNode.path("value").asText("").trim().toLowerCase();
                if (VALID_TAG_CATEGORIES.contains(category) && !value.isEmpty()) {
                    tags.add(new NoteTagDto(category, value));
                }
            }
            note.setTags(tags);
        }

        return note;
    }

    @Override
    public ConversationSummary generateSummary(List<RecentMessage> recentMessages, String userId, String language) {
        // TODO: Implement conversation summarization (Week 4)
        throw new UnsupportedOperationException("TODO: Implement conversation summarization");
    }

    @Override
    public int countTokens(String text, String model) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        // TODO: Replace with a real tokenizer library (e.g., jtokkit) that uses the model param
        // to select the correct encoding. Current approximation: 1 token ≈ 4 characters.
        return (int) Math.ceil(text.length() / 4.0);
    }

    @Override
    public boolean checkTokenBudget(String userId, int estimatedTokens) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        // TODO: Implement daily reset specific to user timezone
        // Reset daily token count if the last reset was before today
        LocalDate today = LocalDate.now();
        if (user.getLastTokenResetDate() == null
                || user.getLastTokenResetDate().toLocalDate().isBefore(today)) {
            user.setTotalDailyTokensUsed(0L);
            user.setLastTokenResetDate(today.atStartOfDay());
            userRepository.save(user);
        }

        return (user.getTotalDailyTokensUsed() + estimatedTokens) <= getDailyLimit(user);
    }

    @Override
    public void recordTokenUsage(String userId, TokenUsageDto tokenUsage) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        long current = user.getTotalDailyTokensUsed() != null ? user.getTotalDailyTokensUsed() : 0L;
        user.setTotalDailyTokensUsed(current + tokenUsage.getTotalTokens());
        userRepository.save(user);

        log.info("Recorded token usage: userId={}, tokens={}, totalUsed={}",
                userId, tokenUsage.getTotalTokens(), user.getTotalDailyTokensUsed());
    }

    @Override
    public String selectModel(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        return selectModelForUser(user);
    }

    private String selectModelForUser(User user) {
        LLMProperties.Models models = llmProperties.getModels();

        if (user.getTier() == UserTier.pro) {
            return models.getStandard();
        }

        return models.getCheap();
    }

    private int getDailyLimit(User user) {
        return (user.getTier() == UserTier.pro)
                ? llmProperties.getTokenLimits().getProTierDaily()
                : llmProperties.getTokenLimits().getFreeTierDaily();
    }

    // ---- Private helpers ----

    /**
     * Build session-scoped conversation context from summaries and recent messages.
     * Messages are filtered to the current session only — not user+language globally.
     *
     * NOTE (Week 4): When session-scoped ConversationSummary is implemented, the summary
     * query here should also be session-scoped. For now, summaries are loaded by
     * user+language as a placeholder.
     */
    private List<Map<String, String>> buildConversationContext(ChatSession session) {
        List<Map<String, String>> contextMessages = new ArrayList<>();

        // Load recent summaries (placeholder: user+language scoped until Week 4 adds session_id to summaries)
        List<ConversationSummary> summaries = conversationSummaryRepository
                .findByUserAndLearningLanguageOrderByCreatedAtDesc(
                        session.getUser(), session.getLearningLanguage(),
                        PageRequest.of(0, MAX_CONTEXT_SUMMARIES));

        if (!summaries.isEmpty()) {
            StringBuilder summaryBlock = new StringBuilder("Previous conversation context:\n");
            // Reverse so oldest summary comes first (chronological order)
            for (int i = summaries.size() - 1; i >= 0; i--) {
                summaryBlock.append("- ").append(summaries.get(i).getSummaryText()).append("\n");
            }
            contextMessages.add(Map.of("role", "system", "content", summaryBlock.toString()));
        }

        // Load recent messages scoped to this session (not user+language)
        List<RecentMessage> recentMessages = recentMessageRepository
                .findBySessionOrderByCreatedAtAsc(session, PageRequest.of(0, MAX_CONTEXT_MESSAGES));

        for (RecentMessage msg : recentMessages) {
            contextMessages.add(Map.of("role", msg.getRole().name(), "content", msg.getContent()));
        }

        return contextMessages;
    }

    /**
     * Legacy overload kept for Week 4 summarization scheduler.
     * New code should use {@link #buildConversationContext(ChatSession)}.
     */
    @SuppressWarnings("unused") // Reserved for Week 4 summarization
    private List<Map<String, String>> buildConversationContext(User user, String learningLanguageCode) {
        Language learningLanguage = languageRepository.findById(learningLanguageCode).orElse(null);
        if (learningLanguage == null) {
            log.warn("Language not found: {}, skipping context", learningLanguageCode);
            return List.of();
        }

        List<Map<String, String>> contextMessages = new ArrayList<>();

        List<ConversationSummary> summaries = conversationSummaryRepository
                .findByUserAndLearningLanguageOrderByCreatedAtDesc(
                        user, learningLanguage, PageRequest.of(0, MAX_CONTEXT_SUMMARIES));

        if (!summaries.isEmpty()) {
            StringBuilder summaryBlock = new StringBuilder("Previous conversation context:\n");
            for (int i = summaries.size() - 1; i >= 0; i--) {
                summaryBlock.append("- ").append(summaries.get(i).getSummaryText()).append("\n");
            }
            contextMessages.add(Map.of("role", "system", "content", summaryBlock.toString()));
        }

        List<RecentMessage> recentMessages = recentMessageRepository
                .findByUserAndLearningLanguageOrderByCreatedAtAsc(
                        user, learningLanguage, PageRequest.of(0, MAX_CONTEXT_MESSAGES));

        for (RecentMessage msg : recentMessages) {
            contextMessages.add(Map.of("role", msg.getRole().name(), "content", msg.getContent()));
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
