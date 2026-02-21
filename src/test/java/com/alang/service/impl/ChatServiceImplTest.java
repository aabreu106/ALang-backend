package com.alang.service.impl;

import com.alang.dto.chat.ChatMessageRequest;
import com.alang.entity.User;
import com.alang.exception.UserNotFoundException;
import com.alang.repository.ChatSessionRepository;
import com.alang.repository.LanguageRepository;
import com.alang.repository.RecentMessageRepository;
import com.alang.repository.UserRepository;
import com.alang.service.LLMService;
import com.alang.service.NoteService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    @Mock
    private ChatSessionRepository chatSessionRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ChatServiceImpl chatService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId("user-1");
        testUser.setAppLanguageCode("en");
    }

    // --- sendMessage ---

    @Test
    void sendMessage_throwsWhenUserNotFound() {
        ChatMessageRequest request = new ChatMessageRequest();
        request.setMessage("Hello");
        request.setSessionId("session-1");

        when(userRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.sendMessage(request, "missing"))
                .isInstanceOf(UserNotFoundException.class);
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
        assertThat(chatService.shouldTriggerSummarization("session-1")).isFalse();
    }

    // --- triggerSummarization (TODO) ---

    @Test
    void triggerSummarization_throwsUnsupportedOperation() {
        assertThatThrownBy(() -> chatService.triggerSummarization("session-1"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
