package com.alang.service.impl;

import com.alang.dto.chat.ChatMessageRequest;
import com.alang.dto.chat.ChatMessageResponse;
import com.alang.dto.chat.CloseSessionRequest;
import com.alang.dto.chat.CreateSessionRequest;
import com.alang.dto.chat.NoteFromSessionRequest;
import com.alang.dto.chat.SessionDetailResponse;
import com.alang.dto.chat.SessionResponse;
import com.alang.dto.chat.UpdateSessionTitleRequest;
import com.alang.dto.chat.TokenUsageDto;
import com.alang.dto.note.NoteDto;
import com.alang.dto.note.UpdateNoteRequest;
import com.alang.entity.ChatSession;
import com.alang.entity.Language;
import com.alang.entity.NoteType;
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
import com.alang.service.LLMService;
import com.alang.service.NoteService;
import com.alang.service.PromptTemplates;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private ChatServiceImpl chatService;

    private User testUser;
    private Language english;
    private Language japanese;
    private ChatSession activeSession;

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

        activeSession = new ChatSession();
        activeSession.setId("session-1");
        activeSession.setUser(testUser);
        activeSession.setStatus(SessionStatus.active);
        activeSession.setTeachingLanguage(english);
        activeSession.setLearningLanguage(japanese);
    }

    private LLMService.LLMResponse makeLLMResponse(String reply) {
        TokenUsageDto usage = new TokenUsageDto(10, 20, 30, null);
        return new LLMService.LLMResponse(reply, "gpt-3.5-turbo", usage);
    }

    // ---- createSession ----

    @Nested
    class CreateSession {

        @Test
        void createSession_successReturnsSessionResponse() {
            CreateSessionRequest request = new CreateSessionRequest();
            request.setLanguage("ja");
            request.setTitle("My Japanese session");

            when(userRepository.findById("user-1")).thenReturn(Optional.of(testUser));
            when(languageRepository.findById("en")).thenReturn(Optional.of(english));
            when(languageRepository.findById("ja")).thenReturn(Optional.of(japanese));
            when(chatSessionRepository.save(any(ChatSession.class))).thenAnswer(inv -> {
                ChatSession s = inv.getArgument(0);
                s.setId("new-session-id");
                return s;
            });

            SessionResponse response = chatService.createSession(request, "user-1");

            assertThat(response.getId()).isEqualTo("new-session-id");
            assertThat(response.getLearningLanguage()).isEqualTo("ja");
            assertThat(response.getTeachingLanguage()).isEqualTo("en");
            assertThat(response.getStatus()).isEqualTo("active");
            assertThat(response.getTitle()).isEqualTo("My Japanese session");
        }

        @Test
        void createSession_throwsWhenUserNotFound() {
            CreateSessionRequest request = new CreateSessionRequest();
            request.setLanguage("ja");

            when(userRepository.findById("missing")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> chatService.createSession(request, "missing"))
                    .isInstanceOf(UserNotFoundException.class);
        }

        @Test
        void createSession_throwsWhenAppLanguageNotFound() {
            CreateSessionRequest request = new CreateSessionRequest();
            request.setLanguage("ja");

            when(userRepository.findById("user-1")).thenReturn(Optional.of(testUser));
            when(languageRepository.findById("en")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> chatService.createSession(request, "user-1"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("App language not found");
        }

        @Test
        void createSession_throwsWhenLearningLanguageNotSupported() {
            CreateSessionRequest request = new CreateSessionRequest();
            request.setLanguage("xx"); // unsupported

            when(userRepository.findById("user-1")).thenReturn(Optional.of(testUser));
            when(languageRepository.findById("en")).thenReturn(Optional.of(english));
            when(languageRepository.findById("xx")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> chatService.createSession(request, "user-1"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Language not supported");
        }
    }

    // ---- getActiveSessions ----

    @Nested
    class GetActiveSessions {

        @Test
        void getActiveSessions_returnsSessionsWithMessages() {
            RecentMessage msg = new RecentMessage();
            msg.setRole(RoleType.user);
            msg.setContent("What is は?");

            when(userRepository.findById("user-1")).thenReturn(Optional.of(testUser));
            when(chatSessionRepository.findByUserAndStatusOrderByCreatedAtDesc(testUser, SessionStatus.active))
                    .thenReturn(List.of(activeSession));
            when(recentMessageRepository.findBySessionOrderByCreatedAtAsc(activeSession))
                    .thenReturn(List.of(msg));

            List<SessionDetailResponse> responses = chatService.getActiveSessions("user-1");

            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).getId()).isEqualTo("session-1");
            assertThat(responses.get(0).getStatus()).isEqualTo("active");
            assertThat(responses.get(0).getLearningLanguage()).isEqualTo("ja");
            assertThat(responses.get(0).getMessages()).hasSize(1);
            assertThat(responses.get(0).getMessages().get(0).getContent()).isEqualTo("What is は?");
        }

        @Test
        void getActiveSessions_returnsEmptyList_whenNoActiveSessions() {
            when(userRepository.findById("user-1")).thenReturn(Optional.of(testUser));
            when(chatSessionRepository.findByUserAndStatusOrderByCreatedAtDesc(testUser, SessionStatus.active))
                    .thenReturn(List.of());

            List<SessionDetailResponse> responses = chatService.getActiveSessions("user-1");

            assertThat(responses).isEmpty();
        }

        @Test
        void getActiveSessions_throwsWhenUserNotFound() {
            when(userRepository.findById("missing")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> chatService.getActiveSessions("missing"))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    // ---- sendMessage ----

    @Nested
    class SendMessage {

        @Test
        void sendMessage_throwsWhenUserNotFound() {
            ChatMessageRequest request = new ChatMessageRequest();
            request.setMessage("Hello");
            request.setSessionId("session-1");

            when(userRepository.findById("missing")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> chatService.sendMessage(request, "missing"))
                    .isInstanceOf(UserNotFoundException.class);
        }

        @Test
        void sendMessage_throwsWhenSessionNotFoundOrAccessDenied() {
            ChatMessageRequest request = new ChatMessageRequest();
            request.setMessage("Hello");
            request.setSessionId("session-1");

            when(userRepository.findById("user-1")).thenReturn(Optional.of(testUser));
            when(chatSessionRepository.findByIdAndUser("session-1", testUser))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> chatService.sendMessage(request, "user-1"))
                    .isInstanceOf(UnauthorizedException.class);
        }

        @Test
        void sendMessage_throwsWhenSessionClosed() {
            ChatMessageRequest request = new ChatMessageRequest();
            request.setMessage("Hello");
            request.setSessionId("session-1");

            ChatSession closedSession = new ChatSession();
            closedSession.setId("session-1");
            closedSession.setStatus(SessionStatus.closed);
            closedSession.setTeachingLanguage(english);
            closedSession.setLearningLanguage(japanese);

            when(userRepository.findById("user-1")).thenReturn(Optional.of(testUser));
            when(chatSessionRepository.findByIdAndUser("session-1", testUser))
                    .thenReturn(Optional.of(closedSession));

            assertThatThrownBy(() -> chatService.sendMessage(request, "user-1"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("closed session");
        }

        @Test
        void sendMessage_savesMessagesAndReturnsResponse() {
            ChatMessageRequest request = new ChatMessageRequest();
            request.setMessage("What is は?");
            request.setSessionId("session-1");

            when(userRepository.findById("user-1")).thenReturn(Optional.of(testUser));
            when(chatSessionRepository.findByIdAndUser("session-1", testUser))
                    .thenReturn(Optional.of(activeSession));
            when(recentMessageRepository.save(any(RecentMessage.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(llmService.generateReply(request, "user-1"))
                    .thenReturn(makeLLMResponse("は is the topic marker."));

            ChatMessageResponse response = chatService.sendMessage(request, "user-1");

            assertThat(response.getReply()).isEqualTo("は is the topic marker.");
            assertThat(response.getModelUsed()).isEqualTo("gpt-3.5-turbo");
            assertThat(response.getTokenUsage().getTotalTokens()).isEqualTo(30);
            verify(recentMessageRepository, times(2)).save(any(RecentMessage.class));
            verify(llmService).recordTokenUsage(eq("user-1"), any(TokenUsageDto.class));
        }

        @Test
        void sendMessage_stripsTopicsBlockFromReply() {
            ChatMessageRequest request = new ChatMessageRequest();
            request.setMessage("Teach me particles");
            request.setSessionId("session-1");

            String rawReply = "Particles are used to mark grammar roles." +
                    PromptTemplates.TOPICS_DELIMITER +
                    "[\"は vs が\", \"に vs で\", \"を particle\"]";

            when(userRepository.findById("user-1")).thenReturn(Optional.of(testUser));
            when(chatSessionRepository.findByIdAndUser("session-1", testUser))
                    .thenReturn(Optional.of(activeSession));
            when(recentMessageRepository.save(any(RecentMessage.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(llmService.generateReply(request, "user-1"))
                    .thenReturn(makeLLMResponse(rawReply));

            ChatMessageResponse response = chatService.sendMessage(request, "user-1");

            assertThat(response.getReply()).isEqualTo("Particles are used to mark grammar roles.");
            assertThat(response.getSuggestedTopics()).containsExactly("は vs が", "に vs で", "を particle");
        }

        @Test
        void sendMessage_returnsNullSuggestedTopicsWhenNonePresent() {
            ChatMessageRequest request = new ChatMessageRequest();
            request.setMessage("What is は?");
            request.setSessionId("session-1");

            when(userRepository.findById("user-1")).thenReturn(Optional.of(testUser));
            when(chatSessionRepository.findByIdAndUser("session-1", testUser))
                    .thenReturn(Optional.of(activeSession));
            when(recentMessageRepository.save(any(RecentMessage.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(llmService.generateReply(request, "user-1"))
                    .thenReturn(makeLLMResponse("は is the topic marker."));

            ChatMessageResponse response = chatService.sendMessage(request, "user-1");

            assertThat(response.getSuggestedTopics()).isNull();
        }
    }

    // ---- createNoteFromSession ----

    @Nested
    class CreateNoteFromSession {

        @Test
        void createNoteFromSession_throwsWhenUserNotFound() {
            when(userRepository.findById("missing")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> chatService.createNoteFromSession(
                    "session-1", new NoteFromSessionRequest(), "missing"))
                    .isInstanceOf(UserNotFoundException.class);
        }

        @Test
        void createNoteFromSession_throwsWhenSessionNotFound() {
            when(userRepository.findById("user-1")).thenReturn(Optional.of(testUser));
            when(chatSessionRepository.findByIdAndUser("session-1", testUser))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> chatService.createNoteFromSession(
                    "session-1", new NoteFromSessionRequest(), "user-1"))
                    .isInstanceOf(UnauthorizedException.class);
        }

        @Test
        void createNoteFromSession_throwsWhenSessionEmpty() {
            when(userRepository.findById("user-1")).thenReturn(Optional.of(testUser));
            when(chatSessionRepository.findByIdAndUser("session-1", testUser))
                    .thenReturn(Optional.of(activeSession));
            when(recentMessageRepository.findBySessionOrderByCreatedAtAsc(activeSession))
                    .thenReturn(List.of());

            assertThatThrownBy(() -> chatService.createNoteFromSession(
                    "session-1", new NoteFromSessionRequest(), "user-1"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("empty session");
        }

        @Test
        void createNoteFromSession_successCreatesAndReturnsNote() {
            RecentMessage msg = new RecentMessage();
            msg.setRole(RoleType.user);
            msg.setContent("What is は?");

            NoteDto generatedNote = new NoteDto();
            generatedNote.setId("note-1");
            generatedNote.setTitle("は");
            generatedNote.setType(NoteType.vocab);

            NoteFromSessionRequest request = new NoteFromSessionRequest();
            request.setTopicFocus("topic marker");

            when(userRepository.findById("user-1")).thenReturn(Optional.of(testUser));
            when(chatSessionRepository.findByIdAndUser("session-1", testUser))
                    .thenReturn(Optional.of(activeSession));
            when(recentMessageRepository.findBySessionOrderByCreatedAtAsc(activeSession))
                    .thenReturn(List.of(msg));
            when(llmService.generateNoteFromConversation(any(), eq("topic marker"), eq(null),
                    eq(japanese), eq(english), eq("user-1")))
                    .thenReturn(generatedNote);
            when(noteService.createNote(generatedNote, "user-1")).thenReturn(generatedNote);

            NoteDto result = chatService.createNoteFromSession("session-1", request, "user-1");

            assertThat(result.getId()).isEqualTo("note-1");
            assertThat(result.getTitle()).isEqualTo("は");
            verify(noteService).createNote(generatedNote, "user-1");
            // Session should be marked as having a note created
            assertThat(activeSession.getNoteCreated()).isTrue();
            verify(chatSessionRepository).save(activeSession);
        }
    }

    // ---- updateNoteFromSession ----

    @Nested
    class UpdateNoteFromSession {

        @Test
        void updateNoteFromSession_throwsWhenUserNotFound() {
            when(userRepository.findById("missing")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> chatService.updateNoteFromSession(
                    "session-1", "note-1", new NoteFromSessionRequest(), "missing"))
                    .isInstanceOf(UserNotFoundException.class);
        }

        @Test
        void updateNoteFromSession_throwsWhenSessionNotFound() {
            when(userRepository.findById("user-1")).thenReturn(Optional.of(testUser));
            when(chatSessionRepository.findByIdAndUser("session-1", testUser))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> chatService.updateNoteFromSession(
                    "session-1", "note-1", new NoteFromSessionRequest(), "user-1"))
                    .isInstanceOf(UnauthorizedException.class);
        }

        @Test
        void updateNoteFromSession_throwsWhenSessionEmpty() {
            NoteDto existingNote = new NoteDto();
            existingNote.setId("note-1");

            when(userRepository.findById("user-1")).thenReturn(Optional.of(testUser));
            when(chatSessionRepository.findByIdAndUser("session-1", testUser))
                    .thenReturn(Optional.of(activeSession));
            when(noteService.getNote("note-1", "user-1")).thenReturn(existingNote);
            when(recentMessageRepository.findBySessionOrderByCreatedAtAsc(activeSession))
                    .thenReturn(List.of());

            assertThatThrownBy(() -> chatService.updateNoteFromSession(
                    "session-1", "note-1", new NoteFromSessionRequest(), "user-1"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("empty session");
        }

        @Test
        void updateNoteFromSession_callsUpdateNoteWithMarkAsUserEditedFalse() {
            RecentMessage msg = new RecentMessage();
            msg.setRole(RoleType.user);
            msg.setContent("What about が?");

            NoteDto existingNote = new NoteDto();
            existingNote.setId("note-1");
            existingNote.setTitle("は");

            NoteDto updatedNote = new NoteDto();
            updatedNote.setId("note-1");
            updatedNote.setTitle("は vs が");

            when(userRepository.findById("user-1")).thenReturn(Optional.of(testUser));
            when(chatSessionRepository.findByIdAndUser("session-1", testUser))
                    .thenReturn(Optional.of(activeSession));
            when(noteService.getNote("note-1", "user-1")).thenReturn(existingNote);
            when(recentMessageRepository.findBySessionOrderByCreatedAtAsc(activeSession))
                    .thenReturn(List.of(msg));
            when(llmService.generateNoteFromConversation(any(), any(), eq(existingNote),
                    eq(japanese), eq(english), eq("user-1")))
                    .thenReturn(updatedNote);
            when(noteService.updateNote(eq("note-1"), any(UpdateNoteRequest.class), eq("user-1"), eq(false)))
                    .thenReturn(updatedNote);

            NoteDto result = chatService.updateNoteFromSession(
                    "session-1", "note-1", new NoteFromSessionRequest(), "user-1");

            assertThat(result.getTitle()).isEqualTo("は vs が");
            // Key assertion: LLM-driven update must pass markAsUserEdited=false
            verify(noteService).updateNote(eq("note-1"), any(UpdateNoteRequest.class), eq("user-1"), eq(false));
        }
    }

    // ---- closeSession ----

    @Nested
    class CloseSession {

        @Test
        void closeSession_throwsWhenUserNotFound() {
            CloseSessionRequest request = new CloseSessionRequest();
            when(userRepository.findById("missing")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> chatService.closeSession("session-1", request, "missing"))
                    .isInstanceOf(UserNotFoundException.class);
        }

        @Test
        void closeSession_throwsWhenSessionNotFound() {
            // force=false → enters early-return branch → findByIdAndUser → empty → throws
            CloseSessionRequest request = new CloseSessionRequest();
            when(userRepository.findById("user-1")).thenReturn(Optional.of(testUser));
            when(chatSessionRepository.existsByIdAndUserAndNoteCreatedTrue("session-1", testUser))
                    .thenReturn(false);
            when(chatSessionRepository.findByIdAndUser("session-1", testUser))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> chatService.closeSession("session-1", request, "user-1"))
                    .isInstanceOf(UnauthorizedException.class);
        }

        @Test
        void closeSession_returnsEarlyWithNoteCreatedFalse_whenNoteNotCreatedAndNotForce() {
            CloseSessionRequest request = new CloseSessionRequest(); // force=false
            when(userRepository.findById("user-1")).thenReturn(Optional.of(testUser));
            when(chatSessionRepository.existsByIdAndUserAndNoteCreatedTrue("session-1", testUser))
                    .thenReturn(false);
            when(chatSessionRepository.findByIdAndUser("session-1", testUser))
                    .thenReturn(Optional.of(activeSession));
            when(recentMessageRepository.countBySession(activeSession)).thenReturn(3L);

            SessionResponse response = chatService.closeSession("session-1", request, "user-1");

            // Session should NOT be closed
            assertThat(response.isNoteCreated()).isFalse();
            assertThat(response.getStatus()).isEqualTo("active");
            verify(chatSessionRepository, never()).save(any());
        }

        @Test
        void closeSession_throwsWhenAlreadyClosed() {
            // Use force=true to bypass the note check and reach the "already closed" guard
            CloseSessionRequest request = new CloseSessionRequest();
            request.setForce(true);

            ChatSession closedSession = new ChatSession();
            closedSession.setId("session-1");
            closedSession.setStatus(SessionStatus.closed);
            closedSession.setTeachingLanguage(english);
            closedSession.setLearningLanguage(japanese);

            when(userRepository.findById("user-1")).thenReturn(Optional.of(testUser));
            when(chatSessionRepository.findByIdAndUser("session-1", testUser))
                    .thenReturn(Optional.of(closedSession));

            assertThatThrownBy(() -> chatService.closeSession("session-1", request, "user-1"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already closed");
        }

        @Test
        void closeSession_closesSessionWhenNoteAlreadyCreated() {
            CloseSessionRequest request = new CloseSessionRequest(); // force=false
            activeSession.setNoteCreated(true);

            when(userRepository.findById("user-1")).thenReturn(Optional.of(testUser));
            when(chatSessionRepository.existsByIdAndUserAndNoteCreatedTrue("session-1", testUser))
                    .thenReturn(true);
            when(chatSessionRepository.findByIdAndUser("session-1", testUser))
                    .thenReturn(Optional.of(activeSession));
            when(chatSessionRepository.save(activeSession)).thenReturn(activeSession);
            when(recentMessageRepository.countBySession(activeSession)).thenReturn(7L);

            SessionResponse response = chatService.closeSession("session-1", request, "user-1");

            assertThat(response.getStatus()).isEqualTo("closed");
            assertThat(response.getMessageCount()).isEqualTo(7);
            assertThat(response.isNoteCreated()).isTrue();
            assertThat(activeSession.getClosedAt()).isNotNull();
        }

        @Test
        void closeSession_closesSessionWhenForceTrueRegardlessOfNote() {
            CloseSessionRequest request = new CloseSessionRequest();
            request.setForce(true);

            when(userRepository.findById("user-1")).thenReturn(Optional.of(testUser));
            when(chatSessionRepository.findByIdAndUser("session-1", testUser))
                    .thenReturn(Optional.of(activeSession));
            when(chatSessionRepository.save(activeSession)).thenReturn(activeSession);
            when(recentMessageRepository.countBySession(activeSession)).thenReturn(7L);

            SessionResponse response = chatService.closeSession("session-1", request, "user-1");

            assertThat(response.getStatus()).isEqualTo("closed");
            // note-created check must be skipped entirely when force=true
            verify(chatSessionRepository, never()).existsByIdAndUserAndNoteCreatedTrue(any(), any());
        }
    }

    // ---- updateSessionTitle ----

    @Nested
    class UpdateSessionTitle {

        @Test
        void updateSessionTitle_throwsWhenUserNotFound() {
            UpdateSessionTitleRequest request = new UpdateSessionTitleRequest();
            request.setTitle("New Title");

            when(userRepository.findById("missing")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> chatService.updateSessionTitle("session-1", request, "missing"))
                    .isInstanceOf(UserNotFoundException.class);
        }

        @Test
        void updateSessionTitle_throwsWhenSessionNotFoundOrAccessDenied() {
            UpdateSessionTitleRequest request = new UpdateSessionTitleRequest();
            request.setTitle("New Title");

            when(userRepository.findById("user-1")).thenReturn(Optional.of(testUser));
            when(chatSessionRepository.findByIdAndUser("session-1", testUser))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> chatService.updateSessionTitle("session-1", request, "user-1"))
                    .isInstanceOf(UnauthorizedException.class);
        }

        @Test
        void updateSessionTitle_updatesAndReturnsTitleInResponse() {
            UpdateSessionTitleRequest request = new UpdateSessionTitleRequest();
            request.setTitle("Japanese Particles");

            when(userRepository.findById("user-1")).thenReturn(Optional.of(testUser));
            when(chatSessionRepository.findByIdAndUser("session-1", testUser))
                    .thenReturn(Optional.of(activeSession));
            when(chatSessionRepository.save(activeSession)).thenAnswer(inv -> {
                ChatSession s = inv.getArgument(0);
                s.setTitle("Japanese Particles");
                return s;
            });
            when(recentMessageRepository.countBySession(activeSession)).thenReturn(4L);

            SessionResponse response = chatService.updateSessionTitle("session-1", request, "user-1");

            assertThat(response.getTitle()).isEqualTo("Japanese Particles");
            assertThat(response.getId()).isEqualTo("session-1");
            assertThat(response.getMessageCount()).isEqualTo(4);
            assertThat(activeSession.getTitle()).isEqualTo("Japanese Particles");
            verify(chatSessionRepository).save(activeSession);
        }

        @Test
        void updateSessionTitle_worksOnClosedSession() {
            ChatSession closedSession = new ChatSession();
            closedSession.setId("session-1");
            closedSession.setUser(testUser);
            closedSession.setStatus(SessionStatus.closed);
            closedSession.setTeachingLanguage(english);
            closedSession.setLearningLanguage(japanese);

            UpdateSessionTitleRequest request = new UpdateSessionTitleRequest();
            request.setTitle("Finished Session");

            when(userRepository.findById("user-1")).thenReturn(Optional.of(testUser));
            when(chatSessionRepository.findByIdAndUser("session-1", testUser))
                    .thenReturn(Optional.of(closedSession));
            when(chatSessionRepository.save(closedSession)).thenReturn(closedSession);
            when(recentMessageRepository.countBySession(closedSession)).thenReturn(10L);

            SessionResponse response = chatService.updateSessionTitle("session-1", request, "user-1");

            assertThat(response.getStatus()).isEqualTo("closed");
            assertThat(closedSession.getTitle()).isEqualTo("Finished Session");
        }
    }

    // ---- Stubs (Week 4 TODO) ----

    @Test
    void getHistory_throwsUnsupportedOperation() {
        assertThatThrownBy(() -> chatService.getHistory("user-1", "ja", 20))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void shouldTriggerSummarization_returnsFalse() {
        assertThat(chatService.shouldTriggerSummarization("session-1")).isFalse();
    }

    @Test
    void triggerSummarization_throwsUnsupportedOperation() {
        assertThatThrownBy(() -> chatService.triggerSummarization("session-1"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
