// src/main/java/com/example/chatbackend/service/ChatMessageProcessor.java
package com.example.chatbackend.service;

import com.example.chatbackend.data.DataStore;
import com.example.chatbackend.model.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMessageProcessor {

    private final UserService userService;
    private final DataStore dataStore;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Async
    public CompletableFuture<Void> process(String payload, WebSocketSession incomingSession, List<WebSocketSession> sessions) {
        try {
            Message msg = objectMapper.readValue(payload, Message.class);
            Message messageWithTimestamp = new Message(msg.sender(), LocalDateTime.now(), msg.message());

            if (userService.exists(messageWithTimestamp.sender())) {
                broadcast(messageWithTimestamp, sessions);
                try {
                    dataStore.addMessage(messageWithTimestamp, messageWithTimestamp.sender());
                } catch (Exception dsEx) {
                    log.error("Failed to store message for {}: {}", messageWithTimestamp.sender(), dsEx.getMessage(), dsEx);
                }
                log.info("process::: messages={}", dataStore.getUserMessages(messageWithTimestamp.sender()));
            } else {
                sendErrorResponse(incomingSession);
            }
        } catch (Exception e) {
            log.error("process::: Failed to parse/handle payload: {}", payload, e);
            try {
                incomingSession.sendMessage(new TextMessage("{\"error\":\"invalid message payload\"}"));
            } catch (IOException ioEx) {
                log.warn("Failed to notify client about parsing error: {}", ioEx.getMessage());
            }
        }
        return CompletableFuture.completedFuture(null);
    }

    private void broadcast(Message message, List<WebSocketSession> sessions) {
        try {
            String out = objectMapper.writeValueAsString(message);
            TextMessage outMessage = new TextMessage(out);
            for (WebSocketSession webSocketSession : sessions) {
                try {
                    webSocketSession.sendMessage(outMessage);
                } catch (IOException e) {
                    log.warn("Failed to send message to session {}: {}", webSocketSession.getId(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("broadcast::: serialization failed: {}", e.getMessage(), e);
        }
    }

    private void sendErrorResponse(WebSocketSession session) {
        try {
            session.sendMessage(new TextMessage("{\"error\":\"this user does not exist, create user first!!\"}"));
        } catch (IOException e) {
            log.warn("sendErrorResponse::: Failed to send error message to client: {}", e.getMessage());
        }
    }
}
