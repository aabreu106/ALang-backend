package com.alang.service.impl;

import com.alang.config.LLMProperties;
import com.alang.dto.chat.ChatMessageRequest;
import com.alang.dto.chat.TokenUsageDto;
import com.alang.dto.note.NoteDto;
import com.alang.entity.*;
import com.alang.exception.LLMProviderException;
import com.alang.exception.RateLimitExceededException;
import com.alang.exception.UserNotFoundException;
import com.alang.repository.ConversationSummaryRepository;
import com.alang.repository.LanguageRepository;
import com.alang.repository.RecentMessageRepository;
import com.alang.repository.UserRepository;
import com.alang.service.LLMService;
import com.alang.service.PromptTemplates;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LLMServiceImplTest {

    @Mock
    private WebClient llmWebClient;

    @Mock
    private LLMProperties llmProperties;

    @Mock
    private PromptTemplates promptTemplates;

    @Mock
    private UserRepository userRepository;

    @Mock
    private LanguageRepository languageRepository;

    @Mock
    private RecentMessageRepository recentMessageRepository;

    @Mock
    private ConversationSummaryRepository conversationSummaryRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private LLMServiceImpl llmService;

    private User freeUser;
    private User proUser;

    @BeforeEach
    void setUp() {
        freeUser = new User();
        freeUser.setId("free-user");
        freeUser.setTier(UserTier.free);
        freeUser.setAppLanguageCode("en");
        freeUser.setTotalDailyTokensUsed(0L);
        freeUser.setLastTokenResetDate(LocalDateTime.now());

        proUser = new User();
        proUser.setId("pro-user");
        proUser.setTier(UserTier.pro);
        proUser.setAppLanguageCode("en");
        proUser.setTotalDailyTokensUsed(0L);
        proUser.setLastTokenResetDate(LocalDateTime.now());
    }

    private LLMProperties.Models createModels() {
        LLMProperties.Models models = new LLMProperties.Models();
        models.setCheap("gpt-3.5-turbo");
        models.setStandard("gpt-4-turbo");
        models.setPremium("gpt-4");
        return models;
    }

    private LLMProperties.TokenLimits createTokenLimits() {
        LLMProperties.TokenLimits limits = new LLMProperties.TokenLimits();
        limits.setFreeTierDaily(3500);
        limits.setProTierDaily(35000);
        limits.setPerRequestMax(3500);
        return limits;
    }

    // --- extractNotes ---

    @Nested
    class ExtractNotes {

        private final ObjectMapper realMapper = new ObjectMapper();

        @Test
        void returnsEmptyListWhenNoDelimiter() {
            List<NoteDto> result = llmService.extractNotes("Just a plain reply", "ja");

            assertThat(result).isEmpty();
        }

        @Test
        void returnsEmptyListWhenEmptyNotesArray() throws Exception {
            String raw = "Explanation\n---NOTES_JSON---\n{\"notes\":[]}";

            // Use real ObjectMapper for JSON parsing tests
            LLMServiceImpl serviceWithRealMapper = new LLMServiceImpl(
                    llmWebClient, llmProperties, promptTemplates,
                    userRepository, languageRepository, recentMessageRepository,
                    conversationSummaryRepository, realMapper);

            List<NoteDto> result = serviceWithRealMapper.extractNotes(raw, "ja");

            assertThat(result).isEmpty();
        }

        @Test
        void extractsValidNotes() {
            String json = """
                    {"notes":[
                      {"type":"vocab","title":"水","summary":"Water","content":"水 means water in Japanese"}
                    ]}""";
            String raw = "Explanation\n---NOTES_JSON---\n" + json;

            LLMServiceImpl serviceWithRealMapper = new LLMServiceImpl(
                    llmWebClient, llmProperties, promptTemplates,
                    userRepository, languageRepository, recentMessageRepository,
                    conversationSummaryRepository, realMapper);

            List<NoteDto> result = serviceWithRealMapper.extractNotes(raw, "ja");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getType()).isEqualTo(NoteType.vocab);
            assertThat(result.get(0).getTitle()).isEqualTo("水");
            assertThat(result.get(0).getSummary()).isEqualTo("Water");
            assertThat(result.get(0).getLearningLanguage()).isEqualTo("ja");
        }

        @Test
        void extractsMultipleNotes() {
            String json = """
                    {"notes":[
                      {"type":"vocab","title":"水","summary":"Water","content":"Details"},
                      {"type":"grammar","title":"て-form","summary":"Connecting form","content":"Details"}
                    ]}""";
            String raw = "Reply\n---NOTES_JSON---\n" + json;

            LLMServiceImpl serviceWithRealMapper = new LLMServiceImpl(
                    llmWebClient, llmProperties, promptTemplates,
                    userRepository, languageRepository, recentMessageRepository,
                    conversationSummaryRepository, realMapper);

            List<NoteDto> result = serviceWithRealMapper.extractNotes(raw, "ja");

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getType()).isEqualTo(NoteType.vocab);
            assertThat(result.get(1).getType()).isEqualTo(NoteType.grammar);
        }

        @Test
        void skipsNotesWithInvalidType() {
            String json = """
                    {"notes":[
                      {"type":"invalid_type","title":"Test","summary":"S","content":"C"},
                      {"type":"vocab","title":"Valid","summary":"S","content":"C"}
                    ]}""";
            String raw = "Reply\n---NOTES_JSON---\n" + json;

            LLMServiceImpl serviceWithRealMapper = new LLMServiceImpl(
                    llmWebClient, llmProperties, promptTemplates,
                    userRepository, languageRepository, recentMessageRepository,
                    conversationSummaryRepository, realMapper);

            List<NoteDto> result = serviceWithRealMapper.extractNotes(raw, "ja");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTitle()).isEqualTo("Valid");
        }

        @Test
        void skipsNotesWithEmptyTitle() {
            String json = """
                    {"notes":[
                      {"type":"vocab","title":"","summary":"S","content":"C"}
                    ]}""";
            String raw = "Reply\n---NOTES_JSON---\n" + json;

            LLMServiceImpl serviceWithRealMapper = new LLMServiceImpl(
                    llmWebClient, llmProperties, promptTemplates,
                    userRepository, languageRepository, recentMessageRepository,
                    conversationSummaryRepository, realMapper);

            List<NoteDto> result = serviceWithRealMapper.extractNotes(raw, "ja");

            assertThat(result).isEmpty();
        }

        @Test
        void truncatesTitleLongerThan60Chars() {
            String longTitle = "A".repeat(80);
            String json = String.format(
                    "{\"notes\":[{\"type\":\"vocab\",\"title\":\"%s\",\"summary\":\"S\",\"content\":\"C\"}]}", longTitle);
            String raw = "Reply\n---NOTES_JSON---\n" + json;

            LLMServiceImpl serviceWithRealMapper = new LLMServiceImpl(
                    llmWebClient, llmProperties, promptTemplates,
                    userRepository, languageRepository, recentMessageRepository,
                    conversationSummaryRepository, realMapper);

            List<NoteDto> result = serviceWithRealMapper.extractNotes(raw, "ja");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTitle()).hasSize(60);
        }

        @Test
        void returnsEmptyListForInvalidJson() {
            String raw = "Reply\n---NOTES_JSON---\n{invalid json!!!}";

            LLMServiceImpl serviceWithRealMapper = new LLMServiceImpl(
                    llmWebClient, llmProperties, promptTemplates,
                    userRepository, languageRepository, recentMessageRepository,
                    conversationSummaryRepository, realMapper);

            List<NoteDto> result = serviceWithRealMapper.extractNotes(raw, "ja");

            assertThat(result).isEmpty();
        }

        @Test
        void setsNullForEmptySummaryAndContent() {
            String json = """
                    {"notes":[
                      {"type":"vocab","title":"水","summary":"","content":""}
                    ]}""";
            String raw = "Reply\n---NOTES_JSON---\n" + json;

            LLMServiceImpl serviceWithRealMapper = new LLMServiceImpl(
                    llmWebClient, llmProperties, promptTemplates,
                    userRepository, languageRepository, recentMessageRepository,
                    conversationSummaryRepository, realMapper);

            List<NoteDto> result = serviceWithRealMapper.extractNotes(raw, "ja");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getSummary()).isNull();
            assertThat(result.get(0).getNoteContent()).isNull();
        }
    }

    // --- countTokens ---

    @Test
    void countTokens_returnsApproximateCount() {
        assertThat(llmService.countTokens("Hello world!", "gpt-3.5-turbo")).isEqualTo(3);
    }

    @Test
    void countTokens_returnsZeroForNull() {
        assertThat(llmService.countTokens(null, "gpt-3.5-turbo")).isZero();
    }

    @Test
    void countTokens_returnsZeroForEmpty() {
        assertThat(llmService.countTokens("", "gpt-3.5-turbo")).isZero();
    }

    @Test
    void countTokens_roundsUp() {
        // 5 chars / 4 = 1.25 → ceiling = 2
        assertThat(llmService.countTokens("Hello", "gpt-3.5-turbo")).isEqualTo(2);
    }

    // --- checkTokenBudget ---

    @Test
    void checkTokenBudget_returnsTrueWhenUnderLimit() {
        freeUser.setTotalDailyTokensUsed(1000L);
        when(userRepository.findById("free-user")).thenReturn(Optional.of(freeUser));
        when(llmProperties.getTokenLimits()).thenReturn(createTokenLimits());

        boolean result = llmService.checkTokenBudget("free-user", 500);

        assertThat(result).isTrue();
    }

    @Test
    void checkTokenBudget_returnsFalseWhenOverLimit() {
        freeUser.setTotalDailyTokensUsed(3000L);
        when(userRepository.findById("free-user")).thenReturn(Optional.of(freeUser));
        when(llmProperties.getTokenLimits()).thenReturn(createTokenLimits());

        boolean result = llmService.checkTokenBudget("free-user", 1000);

        assertThat(result).isFalse();
    }

    @Test
    void checkTokenBudget_resetsCountWhenNewDay() {
        freeUser.setTotalDailyTokensUsed(5000L);
        freeUser.setLastTokenResetDate(LocalDateTime.now().minusDays(1));
        when(userRepository.findById("free-user")).thenReturn(Optional.of(freeUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(llmProperties.getTokenLimits()).thenReturn(createTokenLimits());

        boolean result = llmService.checkTokenBudget("free-user", 100);

        assertThat(result).isTrue();
        assertThat(freeUser.getTotalDailyTokensUsed()).isZero();
    }

    @Test
    void checkTokenBudget_throwsWhenUserNotFound() {
        when(userRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> llmService.checkTokenBudget("missing", 100))
                .isInstanceOf(UserNotFoundException.class);
    }

    // --- recordTokenUsage ---

    @Test
    void recordTokenUsage_incrementsUserTotal() {
        freeUser.setTotalDailyTokensUsed(1000L);
        when(userRepository.findById("free-user")).thenReturn(Optional.of(freeUser));

        TokenUsageDto usage = new TokenUsageDto(50, 100, 150, null);
        llmService.recordTokenUsage("free-user", usage);

        assertThat(freeUser.getTotalDailyTokensUsed()).isEqualTo(1150L);
        verify(userRepository).save(freeUser);
    }

    @Test
    void recordTokenUsage_handlesNullExistingUsage() {
        freeUser.setTotalDailyTokensUsed(null);
        when(userRepository.findById("free-user")).thenReturn(Optional.of(freeUser));

        TokenUsageDto usage = new TokenUsageDto(10, 20, 30, null);
        llmService.recordTokenUsage("free-user", usage);

        assertThat(freeUser.getTotalDailyTokensUsed()).isEqualTo(30L);
    }

    // --- selectModel ---

    @Test
    void selectModel_freeUserCasualChat_returnsCheap() {
        ChatMessageRequest request = new ChatMessageRequest();
        request.setIntent("casual_chat");
        request.setDepth("normal");

        when(userRepository.findById("free-user")).thenReturn(Optional.of(freeUser));
        when(llmProperties.getModels()).thenReturn(createModels());

        String model = llmService.selectModel(request, "free-user");

        assertThat(model).isEqualTo("gpt-3.5-turbo");
    }

    @Test
    void selectModel_freeUserDetailedGrammar_returnsStandard() {
        ChatMessageRequest request = new ChatMessageRequest();
        request.setIntent("grammar_explanation");
        request.setDepth("detailed");

        when(userRepository.findById("free-user")).thenReturn(Optional.of(freeUser));
        when(llmProperties.getModels()).thenReturn(createModels());

        String model = llmService.selectModel(request, "free-user");

        assertThat(model).isEqualTo("gpt-4-turbo");
    }

    @Test
    void selectModel_proUserCasualChat_returnsStandard() {
        ChatMessageRequest request = new ChatMessageRequest();
        request.setIntent("casual_chat");
        request.setDepth("normal");

        when(userRepository.findById("pro-user")).thenReturn(Optional.of(proUser));
        when(llmProperties.getModels()).thenReturn(createModels());

        String model = llmService.selectModel(request, "pro-user");

        assertThat(model).isEqualTo("gpt-4-turbo");
    }

    @Test
    void selectModel_proUserGrammar_returnsPremium() {
        ChatMessageRequest request = new ChatMessageRequest();
        request.setIntent("grammar_explanation");
        request.setDepth("normal");

        when(userRepository.findById("pro-user")).thenReturn(Optional.of(proUser));
        when(llmProperties.getModels()).thenReturn(createModels());

        String model = llmService.selectModel(request, "pro-user");

        assertThat(model).isEqualTo("gpt-4");
    }

    @Test
    void selectModel_proUserDetailed_returnsPremium() {
        ChatMessageRequest request = new ChatMessageRequest();
        request.setIntent("casual_chat");
        request.setDepth("detailed");

        when(userRepository.findById("pro-user")).thenReturn(Optional.of(proUser));
        when(llmProperties.getModels()).thenReturn(createModels());

        String model = llmService.selectModel(request, "pro-user");

        assertThat(model).isEqualTo("gpt-4");
    }

    @Test
    void selectModel_freeUserNonDetailedGrammar_returnsCheap() {
        ChatMessageRequest request = new ChatMessageRequest();
        request.setIntent("grammar_explanation");
        request.setDepth("normal");

        when(userRepository.findById("free-user")).thenReturn(Optional.of(freeUser));
        when(llmProperties.getModels()).thenReturn(createModels());

        String model = llmService.selectModel(request, "free-user");

        // Free tier: educational but not detailed → cheap
        assertThat(model).isEqualTo("gpt-3.5-turbo");
    }

    @Test
    void selectModel_handlesNullIntentAndDepth() {
        ChatMessageRequest request = new ChatMessageRequest();
        // intent and depth are null

        when(userRepository.findById("free-user")).thenReturn(Optional.of(freeUser));
        when(llmProperties.getModels()).thenReturn(createModels());

        String model = llmService.selectModel(request, "free-user");

        assertThat(model).isEqualTo("gpt-3.5-turbo");
    }

    @Test
    void selectModel_freeUserDetailedVocabulary_returnsStandard() {
        ChatMessageRequest request = new ChatMessageRequest();
        request.setIntent("vocabulary");
        request.setDepth("detailed");

        when(userRepository.findById("free-user")).thenReturn(Optional.of(freeUser));
        when(llmProperties.getModels()).thenReturn(createModels());

        String model = llmService.selectModel(request, "free-user");

        assertThat(model).isEqualTo("gpt-4-turbo");
    }

    @Test
    void selectModel_freeUserDetailedCorrectionRequest_returnsStandard() {
        ChatMessageRequest request = new ChatMessageRequest();
        request.setIntent("correction_request");
        request.setDepth("detailed");

        when(userRepository.findById("free-user")).thenReturn(Optional.of(freeUser));
        when(llmProperties.getModels()).thenReturn(createModels());

        String model = llmService.selectModel(request, "free-user");

        assertThat(model).isEqualTo("gpt-4-turbo");
    }

    @Test
    void selectModel_proUserCorrectionRequest_returnsPremium() {
        ChatMessageRequest request = new ChatMessageRequest();
        request.setIntent("correction_request");
        request.setDepth("normal");

        when(userRepository.findById("pro-user")).thenReturn(Optional.of(proUser));
        when(llmProperties.getModels()).thenReturn(createModels());

        String model = llmService.selectModel(request, "pro-user");

        assertThat(model).isEqualTo("gpt-4");
    }

    @Test
    void selectModel_proUserVocabulary_returnsPremium() {
        ChatMessageRequest request = new ChatMessageRequest();
        request.setIntent("vocabulary");
        request.setDepth("brief");

        when(userRepository.findById("pro-user")).thenReturn(Optional.of(proUser));
        when(llmProperties.getModels()).thenReturn(createModels());

        String model = llmService.selectModel(request, "pro-user");

        assertThat(model).isEqualTo("gpt-4");
    }

    @Test
    void selectModel_freeUserDetailedNonEducational_returnsCheap() {
        ChatMessageRequest request = new ChatMessageRequest();
        request.setIntent("casual_chat");
        request.setDepth("detailed");

        when(userRepository.findById("free-user")).thenReturn(Optional.of(freeUser));
        when(llmProperties.getModels()).thenReturn(createModels());

        String model = llmService.selectModel(request, "free-user");

        // detailed but not educational → still cheap for free tier
        assertThat(model).isEqualTo("gpt-3.5-turbo");
    }

    @Test
    void selectModel_throwsWhenUserNotFound() {
        ChatMessageRequest request = new ChatMessageRequest();
        when(userRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> llmService.selectModel(request, "missing"))
                .isInstanceOf(UserNotFoundException.class);
    }

    // --- checkTokenBudget (additional coverage) ---

    @Test
    void checkTokenBudget_resetsCountWhenLastResetDateIsNull() {
        freeUser.setTotalDailyTokensUsed(9999L);
        freeUser.setLastTokenResetDate(null);
        when(userRepository.findById("free-user")).thenReturn(Optional.of(freeUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(llmProperties.getTokenLimits()).thenReturn(createTokenLimits());

        boolean result = llmService.checkTokenBudget("free-user", 100);

        assertThat(result).isTrue();
        assertThat(freeUser.getTotalDailyTokensUsed()).isZero();
        verify(userRepository).save(freeUser);
    }

    @Test
    void checkTokenBudget_returnsTrueExactlyAtLimit() {
        freeUser.setTotalDailyTokensUsed(3000L);
        when(userRepository.findById("free-user")).thenReturn(Optional.of(freeUser));
        when(llmProperties.getTokenLimits()).thenReturn(createTokenLimits());

        // 3000 + 500 = 3500 == limit → should pass
        boolean result = llmService.checkTokenBudget("free-user", 500);

        assertThat(result).isTrue();
    }

    @Test
    void checkTokenBudget_returnsFalseOneOverLimit() {
        freeUser.setTotalDailyTokensUsed(3000L);
        when(userRepository.findById("free-user")).thenReturn(Optional.of(freeUser));
        when(llmProperties.getTokenLimits()).thenReturn(createTokenLimits());

        // 3000 + 501 = 3501 > 3500 limit → should fail
        boolean result = llmService.checkTokenBudget("free-user", 501);

        assertThat(result).isFalse();
    }

    @Test
    void checkTokenBudget_proUserHasHigherLimit() {
        proUser.setTotalDailyTokensUsed(5000L);
        when(userRepository.findById("pro-user")).thenReturn(Optional.of(proUser));
        when(llmProperties.getTokenLimits()).thenReturn(createTokenLimits());

        // 5000 + 1000 = 6000, well under pro limit of 35000
        boolean result = llmService.checkTokenBudget("pro-user", 1000);

        assertThat(result).isTrue();
    }

    // --- recordTokenUsage (additional coverage) ---

    @Test
    void recordTokenUsage_throwsWhenUserNotFound() {
        when(userRepository.findById("missing")).thenReturn(Optional.empty());

        TokenUsageDto usage = new TokenUsageDto(10, 20, 30, null);

        assertThatThrownBy(() -> llmService.recordTokenUsage("missing", usage))
                .isInstanceOf(UserNotFoundException.class);
    }

    // --- countTokens (additional coverage) ---

    @Test
    void countTokens_exactMultipleOfFour() {
        // 8 chars / 4 = exactly 2
        assertThat(llmService.countTokens("12345678", "gpt-3.5-turbo")).isEqualTo(2);
    }

    @Test
    void countTokens_singleCharacter() {
        // 1 char / 4 = 0.25 → ceiling = 1
        assertThat(llmService.countTokens("A", "gpt-3.5-turbo")).isEqualTo(1);
    }

    // --- extractNotes (additional coverage) ---

    @Nested
    class ExtractNotesAdditional {

        private final ObjectMapper realMapper = new ObjectMapper();

        private LLMServiceImpl createServiceWithRealMapper() {
            return new LLMServiceImpl(
                    llmWebClient, llmProperties, promptTemplates,
                    userRepository, languageRepository, recentMessageRepository,
                    conversationSummaryRepository, realMapper);
        }

        @Test
        void extractsExceptionType() {
            String json = """
                    {"notes":[
                      {"type":"exception","title":"Irregular verb: する","summary":"する is irregular","content":"Details"}
                    ]}""";
            String raw = "Reply\n---NOTES_JSON---\n" + json;

            List<NoteDto> result = createServiceWithRealMapper().extractNotes(raw, "ja");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getType()).isEqualTo(NoteType.exception);
        }

        @Test
        void extractsOtherType() {
            String json = """
                    {"notes":[
                      {"type":"other","title":"Cultural context","summary":"Politeness levels","content":"Details"}
                    ]}""";
            String raw = "Reply\n---NOTES_JSON---\n" + json;

            List<NoteDto> result = createServiceWithRealMapper().extractNotes(raw, "ja");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getType()).isEqualTo(NoteType.other);
        }

        @Test
        void handlesNoteMissingOptionalFields() {
            // Only type and title present, no summary or content keys
            String json = """
                    {"notes":[
                      {"type":"vocab","title":"水"}
                    ]}""";
            String raw = "Reply\n---NOTES_JSON---\n" + json;

            List<NoteDto> result = createServiceWithRealMapper().extractNotes(raw, "ja");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTitle()).isEqualTo("水");
            assertThat(result.get(0).getSummary()).isNull();
            assertThat(result.get(0).getNoteContent()).isNull();
        }

        @Test
        void handlesNotesMissingNotesKey() {
            String json = "{\"data\":[]}";
            String raw = "Reply\n---NOTES_JSON---\n" + json;

            List<NoteDto> result = createServiceWithRealMapper().extractNotes(raw, "ja");

            assertThat(result).isEmpty();
        }

        @Test
        void handlesTypeWithWhitespaceAndCasing() {
            String json = """
                    {"notes":[
                      {"type":"  VOCAB  ","title":"火","summary":"Fire","content":"C"}
                    ]}""";
            String raw = "Reply\n---NOTES_JSON---\n" + json;

            List<NoteDto> result = createServiceWithRealMapper().extractNotes(raw, "ja");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getType()).isEqualTo(NoteType.vocab);
        }

        @Test
        void titleExactly60CharsIsNotTruncated() {
            String title = "A".repeat(60);
            String json = String.format(
                    "{\"notes\":[{\"type\":\"vocab\",\"title\":\"%s\",\"summary\":\"S\",\"content\":\"C\"}]}", title);
            String raw = "Reply\n---NOTES_JSON---\n" + json;

            List<NoteDto> result = createServiceWithRealMapper().extractNotes(raw, "ja");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTitle()).hasSize(60);
        }

        @Test
        void setsCorrectLanguageOnExtractedNotes() {
            String json = """
                    {"notes":[
                      {"type":"vocab","title":"agua","summary":"Water","content":"C"}
                    ]}""";
            String raw = "Reply\n---NOTES_JSON---\n" + json;

            List<NoteDto> result = createServiceWithRealMapper().extractNotes(raw, "es");

            assertThat(result.get(0).getLearningLanguage()).isEqualTo("es");
        }
    }

    // --- generateReply, callLLMApi, parseApiResponse, buildConversationContext ---

    @Nested
    class GenerateReply {

        private Language english;
        private Language japanese;

        @BeforeEach
        void setUpLanguages() {
            english = new Language();
            english.setCode("en");
            english.setName("English");
            english.setNativeName("English");

            japanese = new Language();
            japanese.setCode("ja");
            japanese.setName("Japanese");
            japanese.setNativeName("日本語");
        }

        @SuppressWarnings("unchecked")
        private void mockWebClientSuccess(Map<String, Object> apiResponse) {
            WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
            WebClient.RequestBodySpec requestBodySpec = mock(WebClient.RequestBodySpec.class);
            WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
            WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
            Mono<Map> mono = mock(Mono.class);

            when(llmWebClient.post()).thenReturn(requestBodyUriSpec);
            when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
            when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.bodyToMono(Map.class)).thenReturn(mono);
            when(mono.block()).thenReturn(apiResponse);
        }

        @SuppressWarnings("unchecked")
        private void mockWebClientThrows(Exception exception) {
            WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
            WebClient.RequestBodySpec requestBodySpec = mock(WebClient.RequestBodySpec.class);
            WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
            WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
            Mono<Map> mono = mock(Mono.class);

            when(llmWebClient.post()).thenReturn(requestBodyUriSpec);
            when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
            when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.bodyToMono(Map.class)).thenReturn(mono);
            when(mono.block()).thenThrow(exception);
        }

        private Map<String, Object> buildApiResponse(String content, int prompt, int completion, int total) {
            Map<String, Object> usage = Map.of(
                    "prompt_tokens", prompt,
                    "completion_tokens", completion,
                    "total_tokens", total
            );
            Map<String, Object> message = Map.of("content", content);
            Map<String, Object> choice = Map.of("message", message);
            // Use HashMap since Map.of doesn't allow conditional keys easily
            HashMap<String, Object> response = new HashMap<>();
            response.put("choices", List.of(choice));
            response.put("usage", usage);
            return response;
        }

        private ChatMessageRequest buildRequest(String msg) {
            ChatMessageRequest request = new ChatMessageRequest();
            request.setLanguage("ja");
            request.setMessage(msg);
            request.setIntent("casual_chat");
            request.setDepth("normal");
            request.setIncludeContext(false);
            return request;
        }

        private void mockCommonDependencies() {
            LLMProperties.Models models = createModels();
            LLMProperties.TokenLimits limits = createTokenLimits();

            when(userRepository.findById("free-user")).thenReturn(Optional.of(freeUser));
            when(languageRepository.findById("en")).thenReturn(Optional.of(english));
            when(languageRepository.findById("ja")).thenReturn(Optional.of(japanese));
            when(promptTemplates.buildChatSystemPrompt("English", "Japanese")).thenReturn("System prompt");
            when(llmProperties.getModels()).thenReturn(models);
            when(llmProperties.getTokenLimits()).thenReturn(limits);
            // save for budget reset
            lenient().when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        }

        // --- generateReply success ---

        @Test
        void generateReply_returnsLLMResponseOnSuccess() {
            mockCommonDependencies();
            mockWebClientSuccess(buildApiResponse("Hello!", 50, 100, 150));

            ChatMessageRequest request = buildRequest("Hi");
            LLMService.LLMResponse response = llmService.generateReply(request, "free-user");

            assertThat(response.getReply()).isEqualTo("Hello!");
            assertThat(response.getModelUsed()).isEqualTo("gpt-3.5-turbo");
            assertThat(response.getTokenUsage().getTotalTokens()).isEqualTo(150);
            assertThat(response.getTokenUsage().getPromptTokens()).isEqualTo(50);
            assertThat(response.getTokenUsage().getCompletionTokens()).isEqualTo(100);
        }

        @Test
        void generateReply_throwsWhenUserNotFound() {
            when(userRepository.findById("missing")).thenReturn(Optional.empty());

            ChatMessageRequest request = buildRequest("Hi");

            assertThatThrownBy(() -> llmService.generateReply(request, "missing"))
                    .isInstanceOf(UserNotFoundException.class);
        }

        @Test
        void generateReply_throwsWhenAppLanguageNotFound() {
            when(userRepository.findById("free-user")).thenReturn(Optional.of(freeUser));
            when(llmProperties.getModels()).thenReturn(createModels());
            when(languageRepository.findById("en")).thenReturn(Optional.empty());

            ChatMessageRequest request = buildRequest("Hi");

            assertThatThrownBy(() -> llmService.generateReply(request, "free-user"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("App language not found");
        }

        @Test
        void generateReply_throwsWhenTargetLanguageNotSupported() {
            when(userRepository.findById("free-user")).thenReturn(Optional.of(freeUser));
            when(llmProperties.getModels()).thenReturn(createModels());
            when(languageRepository.findById("en")).thenReturn(Optional.of(english));
            when(languageRepository.findById("xx")).thenReturn(Optional.empty());

            ChatMessageRequest request = buildRequest("Hi");
            request.setLanguage("xx");

            assertThatThrownBy(() -> llmService.generateReply(request, "free-user"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Language not supported");
        }

        @Test
        void generateReply_throwsRateLimitWhenBudgetExceeded() {
            freeUser.setTotalDailyTokensUsed(3500L); // already at limit

            when(userRepository.findById("free-user")).thenReturn(Optional.of(freeUser));
            when(languageRepository.findById("en")).thenReturn(Optional.of(english));
            when(languageRepository.findById("ja")).thenReturn(Optional.of(japanese));
            when(promptTemplates.buildChatSystemPrompt("English", "Japanese")).thenReturn("System prompt");
            when(llmProperties.getModels()).thenReturn(createModels());
            when(llmProperties.getTokenLimits()).thenReturn(createTokenLimits());

            ChatMessageRequest request = buildRequest("Hi");

            assertThatThrownBy(() -> llmService.generateReply(request, "free-user"))
                    .isInstanceOf(RateLimitExceededException.class)
                    .hasMessageContaining("Daily token limit exceeded");
        }

        // --- parseApiResponse (tested indirectly through generateReply) ---

        @Test
        void generateReply_handlesResponseWithoutUsage() {
            mockCommonDependencies();

            // Response without "usage" key
            Map<String, Object> message = Map.of("content", "Reply text");
            Map<String, Object> choice = Map.of("message", message);
            Map<String, Object> apiResponse = Map.of("choices", List.of(choice));

            mockWebClientSuccess(apiResponse);

            ChatMessageRequest request = buildRequest("Hi");
            LLMService.LLMResponse response = llmService.generateReply(request, "free-user");

            assertThat(response.getReply()).isEqualTo("Reply text");
            assertThat(response.getTokenUsage().getTotalTokens()).isZero();
            assertThat(response.getTokenUsage().getPromptTokens()).isZero();
        }

        @Test
        void generateReply_throwsOnNullApiResponse() {
            mockCommonDependencies();
            mockWebClientSuccess(null);

            ChatMessageRequest request = buildRequest("Hi");

            assertThatThrownBy(() -> llmService.generateReply(request, "free-user"))
                    .isInstanceOf(LLMProviderException.class)
                    .hasMessageContaining("Failed to call LLM API after 3 attempts")
                    .cause().hasMessageContaining("missing 'choices'");
        }

        @Test
        void generateReply_throwsOnMissingChoices() {
            mockCommonDependencies();
            mockWebClientSuccess(Map.of("id", "123")); // no "choices" key

            ChatMessageRequest request = buildRequest("Hi");

            assertThatThrownBy(() -> llmService.generateReply(request, "free-user"))
                    .isInstanceOf(LLMProviderException.class)
                    .hasMessageContaining("Failed to call LLM API after 3 attempts")
                    .cause().hasMessageContaining("missing 'choices'");
        }

        @Test
        void generateReply_throwsOnEmptyChoices() {
            mockCommonDependencies();
            mockWebClientSuccess(Map.of("choices", List.of()));

            ChatMessageRequest request = buildRequest("Hi");

            assertThatThrownBy(() -> llmService.generateReply(request, "free-user"))
                    .isInstanceOf(LLMProviderException.class)
                    .hasMessageContaining("Failed to call LLM API after 3 attempts")
                    .cause().hasMessageContaining("empty 'choices'");
        }

        // --- callLLMApi retry and error handling ---

        @Test
        void generateReply_throwsOnNonRetryableHttpError() {
            mockCommonDependencies();

            WebClientResponseException clientError = WebClientResponseException.create(
                    400, "Bad Request", null, "error body".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);

            mockWebClientThrows(clientError);

            ChatMessageRequest request = buildRequest("Hi");

            assertThatThrownBy(() -> llmService.generateReply(request, "free-user"))
                    .isInstanceOf(LLMProviderException.class)
                    .hasMessageContaining("LLM API returned 400");
        }

        @Test
        void generateReply_throwsAfterMaxRetriesOnServerError() {
            mockCommonDependencies();

            WebClientResponseException serverError = WebClientResponseException.create(
                    500, "Internal Server Error", null, "error".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);

            // Mock the full chain so each post() returns fresh mocks that throw
            WebClient.RequestBodyUriSpec uriSpec = mock(WebClient.RequestBodyUriSpec.class);
            WebClient.RequestBodySpec bodySpec = mock(WebClient.RequestBodySpec.class);
            @SuppressWarnings("unchecked")
            WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
            WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);
            @SuppressWarnings("unchecked")
            Mono<Map> mono = mock(Mono.class);

            when(llmWebClient.post()).thenReturn(uriSpec);
            when(uriSpec.uri(anyString())).thenReturn(bodySpec);
            when(bodySpec.bodyValue(any())).thenReturn(headersSpec);
            when(headersSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.bodyToMono(Map.class)).thenReturn(mono);
            when(mono.block()).thenThrow(serverError);

            ChatMessageRequest request = buildRequest("Hi");

            assertThatThrownBy(() -> llmService.generateReply(request, "free-user"))
                    .isInstanceOf(LLMProviderException.class)
                    .hasMessageContaining("after 3 attempts");
        }

        @Test
        void generateReply_throwsAfterMaxRetriesOn429() {
            mockCommonDependencies();

            WebClientResponseException rateLimitError = WebClientResponseException.create(
                    429, "Too Many Requests", null, "rate limited".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);

            mockWebClientThrows(rateLimitError);

            ChatMessageRequest request = buildRequest("Hi");

            assertThatThrownBy(() -> llmService.generateReply(request, "free-user"))
                    .isInstanceOf(LLMProviderException.class)
                    .hasMessageContaining("after 3 attempts");
        }

        @Test
        void generateReply_throwsAfterMaxRetriesOnGenericException() {
            mockCommonDependencies();
            mockWebClientThrows(new RuntimeException("Connection reset"));

            ChatMessageRequest request = buildRequest("Hi");

            assertThatThrownBy(() -> llmService.generateReply(request, "free-user"))
                    .isInstanceOf(LLMProviderException.class)
                    .hasMessageContaining("Failed to call LLM API after 3 attempts");
        }

        // --- buildConversationContext (tested indirectly through generateReply) ---

        @Test
        void generateReply_includesConversationContext() {
            mockCommonDependencies();
            mockWebClientSuccess(buildApiResponse("Reply with context", 80, 120, 200));

            // Set up summaries
            ConversationSummary summary = new ConversationSummary();
            summary.setSummaryText("User learned about particles.");
            when(conversationSummaryRepository.findByUserAndLearningLanguageOrderByCreatedAtDesc(
                    eq(freeUser), eq(japanese), any())).thenReturn(List.of(summary));

            // Set up recent messages
            RecentMessage msg1 = new RecentMessage();
            msg1.setRole("user");
            msg1.setContent("What is は?");
            RecentMessage msg2 = new RecentMessage();
            msg2.setRole("assistant");
            msg2.setContent("は is a topic marker.");
            when(recentMessageRepository.findByUserAndLearningLanguageOrderByCreatedAtAsc(
                    eq(freeUser), eq(japanese), any())).thenReturn(List.of(msg1, msg2));

            ChatMessageRequest request = buildRequest("Tell me more");
            request.setIncludeContext(true);

            LLMService.LLMResponse response = llmService.generateReply(request, "free-user");

            assertThat(response.getReply()).isEqualTo("Reply with context");
            // Verify context repos were queried
            verify(conversationSummaryRepository).findByUserAndLearningLanguageOrderByCreatedAtDesc(
                    eq(freeUser), eq(japanese), any());
            verify(recentMessageRepository).findByUserAndLearningLanguageOrderByCreatedAtAsc(
                    eq(freeUser), eq(japanese), any());
        }

        @Test
        void generateReply_skipsContextWhenIncludeContextFalse() {
            mockCommonDependencies();
            mockWebClientSuccess(buildApiResponse("Reply", 50, 100, 150));

            ChatMessageRequest request = buildRequest("Hi");
            request.setIncludeContext(false);

            llmService.generateReply(request, "free-user");

            verifyNoInteractions(conversationSummaryRepository);
            // recentMessageRepository is not called for context (only called if includeContext=true)
            verify(recentMessageRepository, never()).findByUserAndLearningLanguageOrderByCreatedAtAsc(
                    any(), any(), any());
        }

        @Test
        void generateReply_handlesEmptyContextGracefully() {
            mockCommonDependencies();
            mockWebClientSuccess(buildApiResponse("Reply", 50, 100, 150));

            when(conversationSummaryRepository.findByUserAndLearningLanguageOrderByCreatedAtDesc(
                    eq(freeUser), eq(japanese), any())).thenReturn(List.of());
            when(recentMessageRepository.findByUserAndLearningLanguageOrderByCreatedAtAsc(
                    eq(freeUser), eq(japanese), any())).thenReturn(List.of());

            ChatMessageRequest request = buildRequest("Hi");
            request.setIncludeContext(true);

            LLMService.LLMResponse response = llmService.generateReply(request, "free-user");

            assertThat(response.getReply()).isEqualTo("Reply");
        }

        @Test
        void generateReply_handlesUnknownContextLanguageGracefully() {
            // When includeContext=true but language lookup for context returns null
            freeUser.setTotalDailyTokensUsed(0L);

            when(userRepository.findById("free-user")).thenReturn(Optional.of(freeUser));
            when(languageRepository.findById("en")).thenReturn(Optional.of(english));
            // First call for target language (in generateReply) returns japanese
            // Second call for context building also needs "ja" but we need it to work for the main flow
            when(languageRepository.findById("ja")).thenReturn(Optional.of(japanese))
                    .thenReturn(Optional.empty()); // context building returns null
            when(promptTemplates.buildChatSystemPrompt("English", "Japanese")).thenReturn("System prompt");
            when(llmProperties.getModels()).thenReturn(createModels());
            when(llmProperties.getTokenLimits()).thenReturn(createTokenLimits());
            lenient().when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            mockWebClientSuccess(buildApiResponse("Reply", 50, 100, 150));

            ChatMessageRequest request = buildRequest("Hi");
            request.setIncludeContext(true);

            // Should not throw - just skips context
            LLMService.LLMResponse response = llmService.generateReply(request, "free-user");
            assertThat(response.getReply()).isEqualTo("Reply");
        }

        @Test
        void generateReply_usesCorrectModelBasedOnRequest() {
            // Set up pro user mocks directly (not mockCommonDependencies which stubs free-user)
            proUser.setTotalDailyTokensUsed(0L);
            proUser.setLastTokenResetDate(LocalDateTime.now());
            proUser.setAppLanguageCode("en");

            when(userRepository.findById("pro-user")).thenReturn(Optional.of(proUser));
            when(languageRepository.findById("en")).thenReturn(Optional.of(english));
            when(languageRepository.findById("ja")).thenReturn(Optional.of(japanese));
            when(promptTemplates.buildChatSystemPrompt("English", "Japanese")).thenReturn("System prompt");
            when(llmProperties.getModels()).thenReturn(createModels());
            when(llmProperties.getTokenLimits()).thenReturn(createTokenLimits());
            lenient().when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            mockWebClientSuccess(buildApiResponse("Detailed grammar explanation", 100, 200, 300));

            ChatMessageRequest request = buildRequest("Explain て-form in detail");
            request.setIntent("grammar_explanation");
            request.setDepth("detailed");

            LLMService.LLMResponse response = llmService.generateReply(request, "pro-user");

            assertThat(response.getModelUsed()).isEqualTo("gpt-4");
        }
    }

    // --- generateSummary (TODO) ---

    @Test
    void generateSummary_throwsUnsupportedOperation() {
        assertThatThrownBy(() -> llmService.generateSummary(List.of(), "user-1", "ja"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
