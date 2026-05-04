package com.example.chatbackend.service;

import com.example.chatbackend.data.DataStore;
import com.example.chatbackend.model.Message;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatWebSocketHandlerTest {

    @InjectMocks
    private ChatWebSocketHandler chatWebSocketHandler;

    @Mock
    private UserService userService;

    @Mock
    private DataStore dataStore;

    @Mock
    private WebSocketSession session1;

    @Mock
    private WebSocketSession session2;

    @Captor
    private ArgumentCaptor<TextMessage> textMessageCaptor;

    @Test
    @DisplayName("test handleTextMessage(), user exists, message broadcast and stored")
    void testHandleTextMessage1() throws Exception {
        String payload = "{\"sender\":\"alice\", \"message\":\"hello\"}";
        when(userService.exists("alice")).thenReturn(true);

        chatWebSocketHandler.afterConnectionEstablished(session1);
        chatWebSocketHandler.afterConnectionEstablished(session2);

        chatWebSocketHandler.handleTextMessage(session1, new TextMessage(payload));

        // allow brief time for async processing
        verify(session1, timeout(500)).sendMessage(textMessageCaptor.capture());
        verify(session2, timeout(500)).sendMessage(any(TextMessage.class));

        TextMessage sent = textMessageCaptor.getValue();
        assertThat(sent.getPayload()).contains("alice");
        assertThat(sent.getPayload()).contains("hello");
        verify(dataStore, timeout(500)).addMessage(any(Message.class), eq("alice"));
    }

    @Test
    @DisplayName("test handleTextMessage(), user does not exist, error sent to user, message not stored")
    void testHandleTextMessage2() throws Exception {
        String payload = "{\"sender\":\"bob\", \"message\":\"hi\"}";
        when(userService.exists("bob")).thenReturn(false);
        chatWebSocketHandler.afterConnectionEstablished(session1);

        chatWebSocketHandler.handleTextMessage(session1, new TextMessage(payload));

        verify(session1, timeout(500)).sendMessage(textMessageCaptor.capture());
        TextMessage sent = textMessageCaptor.getValue();
        assertThat(sent.getPayload()).contains("error");
        assertThat(sent.getPayload()).contains("create user first");
        verify(dataStore, never()).addMessage(any(Message.class), anyString());
    }

}
