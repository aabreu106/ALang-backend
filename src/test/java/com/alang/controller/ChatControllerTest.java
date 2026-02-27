package com.alang.controller;

import com.alang.dto.chat.ChatMessageRequest;
import com.alang.dto.chat.ChatMessageResponse;
import com.alang.dto.chat.CloseSessionRequest;
import com.alang.dto.chat.SessionDetailResponse;
import com.alang.dto.chat.SessionResponse;
import com.alang.service.ChatService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    @Mock
    private ChatService chatService;

    @InjectMocks
    private ChatController chatController;

    @Test
    void sendMessage_returnsOkWithResponse() {
        ChatMessageRequest request = new ChatMessageRequest();
        request.setMessage("What is the て-form?");
        request.setSessionId("session-1"); // pre-set so stub matches after controller injects it

        ChatMessageResponse chatResponse = new ChatMessageResponse();
        chatResponse.setReply("The て-form is...");
        chatResponse.setModelUsed("gpt-3.5-turbo");

        when(chatService.sendMessage(request, "user-1")).thenReturn(chatResponse);

        var response = chatController.sendMessage("session-1", request, "user-1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(chatResponse);
        verify(chatService).sendMessage(request, "user-1");
    }

    @Test
    void sendMessage_passesUserIdToService() {
        ChatMessageRequest request = new ChatMessageRequest();
        request.setMessage("How do I conjugate ser?");
        request.setSessionId("session-42");

        ChatMessageResponse chatResponse = new ChatMessageResponse();
        chatResponse.setReply("Ser conjugates as...");
        when(chatService.sendMessage(request, "user-42")).thenReturn(chatResponse);

        chatController.sendMessage("session-42", request, "user-42");

        verify(chatService).sendMessage(request, "user-42");
    }

    // ---- closeSession ----

    @Test
    void closeSession_returnsOkWithClosedSession() {
        SessionResponse sessionResponse = new SessionResponse();
        sessionResponse.setId("session-1");
        sessionResponse.setStatus("closed");
        sessionResponse.setNoteCreated(true);

        CloseSessionRequest request = new CloseSessionRequest();
        when(chatService.closeSession("session-1", request, "user-1")).thenReturn(sessionResponse);

        var response = chatController.closeSession("session-1", request, "user-1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getStatus()).isEqualTo("closed");
        assertThat(response.getBody().isNoteCreated()).isTrue();
        verify(chatService).closeSession("session-1", request, "user-1");
    }

    @Test
    void closeSession_returnsSessionWithNoteCreatedFalse_whenNoteNotYetCreated() {
        SessionResponse sessionResponse = new SessionResponse();
        sessionResponse.setId("session-1");
        sessionResponse.setStatus("active");
        sessionResponse.setNoteCreated(false);

        CloseSessionRequest request = new CloseSessionRequest(); // force=false
        when(chatService.closeSession("session-1", request, "user-1")).thenReturn(sessionResponse);

        var response = chatController.closeSession("session-1", request, "user-1");

        assertThat(response.getBody().isNoteCreated()).isFalse();
        assertThat(response.getBody().getStatus()).isEqualTo("active");
    }

    @Test
    void closeSession_usesDefaultRequestWhenBodyIsNull() {
        SessionResponse sessionResponse = new SessionResponse();
        sessionResponse.setStatus("closed");

        // null body → controller creates a default CloseSessionRequest (force=false)
        when(chatService.closeSession(eq("session-1"), any(CloseSessionRequest.class), eq("user-1")))
                .thenReturn(sessionResponse);

        var response = chatController.closeSession("session-1", null, "user-1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(chatService).closeSession(eq("session-1"), any(CloseSessionRequest.class), eq("user-1"));
    }

    @Test
    void closeSession_passesForceTrueToService() {
        CloseSessionRequest request = new CloseSessionRequest();
        request.setForce(true);

        SessionResponse sessionResponse = new SessionResponse();
        sessionResponse.setStatus("closed");
        when(chatService.closeSession("session-1", request, "user-1")).thenReturn(sessionResponse);

        chatController.closeSession("session-1", request, "user-1");

        verify(chatService).closeSession("session-1", request, "user-1");
    }

    // ---- getActiveSessions ----

    @Test
    void getActiveSessions_returnsOkWithList() {
        SessionDetailResponse session = new SessionDetailResponse();
        session.setId("session-1");
        session.setStatus("active");
        session.setMessages(List.of());

        when(chatService.getActiveSessions("user-1")).thenReturn(List.of(session));

        var response = chatController.getActiveSessions("user-1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getId()).isEqualTo("session-1");
        verify(chatService).getActiveSessions("user-1");
    }

    @Test
    void getActiveSessions_returnsEmptyList_whenNoActiveSessions() {
        when(chatService.getActiveSessions("user-1")).thenReturn(List.of());

        var response = chatController.getActiveSessions("user-1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }
}
