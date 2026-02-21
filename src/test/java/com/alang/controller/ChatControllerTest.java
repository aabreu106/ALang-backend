package com.alang.controller;

import com.alang.dto.chat.ChatHistoryDto;
import com.alang.dto.chat.ChatMessageRequest;
import com.alang.dto.chat.ChatMessageResponse;
import com.alang.service.ChatService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Collections;

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

    @Test
    void getHistory_returnsOkWithHistory() {
        ChatHistoryDto historyDto = new ChatHistoryDto();
        historyDto.setLanguage("ja");
        historyDto.setSummaries(Collections.emptyList());
        historyDto.setRecentExchanges(Collections.emptyList());

        when(chatService.getHistory("user-1", "ja", 20)).thenReturn(historyDto);

        var response = chatController.getHistory("ja", 20, "user-1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(historyDto);
        verify(chatService).getHistory("user-1", "ja", 20);
    }

    @Test
    void getHistory_passesCustomLimitToService() {
        ChatHistoryDto historyDto = new ChatHistoryDto();
        when(chatService.getHistory("user-1", "ko", 50)).thenReturn(historyDto);

        chatController.getHistory("ko", 50, "user-1");

        verify(chatService).getHistory("user-1", "ko", 50);
    }
}
