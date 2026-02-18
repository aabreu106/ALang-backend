package com.alang.service.impl;

import com.alang.dto.chat.ChatMessageRequest;
import com.alang.dto.chat.ChatMessageResponse;
import com.alang.dto.chat.TokenUsageDto;
import com.alang.dto.note.NoteDto;
import com.alang.entity.Language;
import com.alang.entity.NoteType;
import com.alang.entity.User;
import com.alang.exception.UserNotFoundException;
import com.alang.repository.LanguageRepository;
import com.alang.repository.RecentMessageRepository;
import com.alang.repository.UserRepository;
import com.alang.service.LLMService;
import com.alang.service.NoteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceImplTest {

    @Mock
    private LLMService llmService;

    @Mock
    private NoteService noteService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private LanguageRepository languageRepository;

    @Mock
    private RecentMessageRepository recentMessageRepository;

    @InjectMocks
    private ChatServiceImpl chatService;

    private User testUser;
    private Language english;
    private Language japanese;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId("user-1");
        testUser.setAppLanguageCode("en");

        english = new Language();
        english.setCode("en");
        english.setName("English");
        english.setNativeName("English");

        japanese = new Language();
        japanese.setCode("ja");
        japanese.setName("Japanese");
        japanese.setNativeName("日本語");
    }

    private ChatMessageRequest buildRequest(String message) {
        ChatMessageRequest request = new ChatMessageRequest();
        request.setLanguage("ja");
        request.setMessage(message);
        request.setIncludeContext(true);
        return request;
    }

    private LLMService.LLMResponse buildLLMResponse(String reply) {
        TokenUsageDto tokenUsage = new TokenUsageDto(100, 200, 300, null);
        return new LLMService.LLMResponse(reply, "gpt-3.5-turbo", tokenUsage);
    }

    // --- sendMessage ---

    @Test
    void sendMessage_returnsCleanReplyWithoutNotesBlock() {
        ChatMessageRequest request = buildRequest("What is て-form?");
        String rawReply = "The て-form is a connecting form.\n---NOTES_JSON---\n{\"notes\":[]}";
        LLMService.LLMResponse llmResponse = buildLLMResponse(rawReply);

        when(userRepository.findById("user-1")).thenReturn(Optional.of(testUser));
        when(languageRepository.findById("en")).thenReturn(Optional.of(english));
        when(languageRepository.findById("ja")).thenReturn(Optional.of(japanese));
        when(llmService.generateReply(request, "user-1")).thenReturn(llmResponse);
        when(llmService.extractNotes(rawReply, "ja")).thenReturn(List.of());

        ChatMessageResponse response = chatService.sendMessage(request, "user-1");

        assertThat(response.getReply()).isEqualTo("The て-form is a connecting form.");
        assertThat(response.getModelUsed()).isEqualTo("gpt-3.5-turbo");
        assertThat(response.getTokenUsage().getTotalTokens()).isEqualTo(300);
    }

    @Test
    void sendMessage_savesUserAndAssistantMessages() {
        ChatMessageRequest request = buildRequest("Hello");
        LLMService.LLMResponse llmResponse = buildLLMResponse("Hi there!");

        when(userRepository.findById("user-1")).thenReturn(Optional.of(testUser));
        when(languageRepository.findById("en")).thenReturn(Optional.of(english));
        when(languageRepository.findById("ja")).thenReturn(Optional.of(japanese));
        when(llmService.generateReply(request, "user-1")).thenReturn(llmResponse);
        when(llmService.extractNotes("Hi there!", "ja")).thenReturn(List.of());

        chatService.sendMessage(request, "user-1");

        // 2 saves: user message + assistant message
        verify(recentMessageRepository, times(2)).save(any());
    }

    @Test
    void sendMessage_extractsAndSavesNotes() {
        ChatMessageRequest request = buildRequest("What is 水?");
        String rawReply = "水 means water.\n---NOTES_JSON---\n{\"notes\":[{\"type\":\"vocab\",\"title\":\"水\"}]}";
        LLMService.LLMResponse llmResponse = buildLLMResponse(rawReply);

        NoteDto extractedNote = new NoteDto();
        extractedNote.setType(NoteType.vocab);
        extractedNote.setTitle("水");
        extractedNote.setLearningLanguage("ja");

        NoteDto savedNote = new NoteDto();
        savedNote.setId("note-1");
        savedNote.setType(NoteType.vocab);
        savedNote.setTitle("水");

        when(userRepository.findById("user-1")).thenReturn(Optional.of(testUser));
        when(languageRepository.findById("en")).thenReturn(Optional.of(english));
        when(languageRepository.findById("ja")).thenReturn(Optional.of(japanese));
        when(llmService.generateReply(request, "user-1")).thenReturn(llmResponse);
        when(llmService.extractNotes(rawReply, "ja")).thenReturn(List.of(extractedNote));
        when(noteService.createNotes(List.of(extractedNote), "user-1")).thenReturn(List.of(savedNote));

        ChatMessageResponse response = chatService.sendMessage(request, "user-1");

        assertThat(response.getCreatedNotes()).hasSize(1);
        assertThat(response.getCreatedNotes().get(0).getId()).isEqualTo("note-1");
        assertThat(response.getCreatedNotes().get(0).getTitle()).isEqualTo("水");
    }

    @Test
    void sendMessage_recordsTokenUsage() {
        ChatMessageRequest request = buildRequest("Test");
        TokenUsageDto tokenUsage = new TokenUsageDto(50, 100, 150, null);
        LLMService.LLMResponse llmResponse = new LLMService.LLMResponse("Reply", "gpt-3.5-turbo", tokenUsage);

        when(userRepository.findById("user-1")).thenReturn(Optional.of(testUser));
        when(languageRepository.findById("en")).thenReturn(Optional.of(english));
        when(languageRepository.findById("ja")).thenReturn(Optional.of(japanese));
        when(llmService.generateReply(request, "user-1")).thenReturn(llmResponse);
        when(llmService.extractNotes("Reply", "ja")).thenReturn(List.of());

        chatService.sendMessage(request, "user-1");

        verify(llmService).recordTokenUsage("user-1", tokenUsage);
    }

    @Test
    void sendMessage_throwsWhenUserNotFound() {
        ChatMessageRequest request = buildRequest("Hello");
        when(userRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.sendMessage(request, "missing"))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void sendMessage_throwsWhenLanguageNotSupported() {
        ChatMessageRequest request = new ChatMessageRequest();
        request.setLanguage("xx");
        request.setMessage("Hello");
        request.setIncludeContext(true);

        when(userRepository.findById("user-1")).thenReturn(Optional.of(testUser));
        when(languageRepository.findById("en")).thenReturn(Optional.of(english));
        when(languageRepository.findById("xx")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.sendMessage(request, "user-1"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // --- getHistory (TODO) ---

    @Test
    void getHistory_throwsUnsupportedOperation() {
        assertThatThrownBy(() -> chatService.getHistory("user-1", "ja", 20))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    // --- shouldTriggerSummarization ---

    @Test
    void shouldTriggerSummarization_returnsFalse() {
        assertThat(chatService.shouldTriggerSummarization("user-1", "ja")).isFalse();
    }

    // --- triggerSummarization (TODO) ---

    @Test
    void triggerSummarization_throwsUnsupportedOperation() {
        assertThatThrownBy(() -> chatService.triggerSummarization("user-1", "ja"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
