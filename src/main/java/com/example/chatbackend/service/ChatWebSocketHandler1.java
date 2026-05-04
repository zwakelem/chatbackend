// src/main/java/com/example/chatbackend/service/ChatWebSocketHandler.java
package com.example.chatbackend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler1 extends TextWebSocketHandler {

    // thread\-safe list so it can be read concurrently by the async processor
    private final List<WebSocketSession> webSocketSessions = new CopyOnWriteArrayList<>();
    private final ChatMessageProcessor messageProcessor;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("afterConnectionEstablished::: {}", session.getId());
        webSocketSessions.add(session);
    }

    @Override
    public void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) {
        String payload = message.getPayload();
        // pass a snapshot of current sessions to the processor
        List<WebSocketSession> sessionsSnapshot = List.copyOf(webSocketSessions);
        messageProcessor.process(payload, session, sessionsSnapshot);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, @NonNull CloseStatus status) {
        log.info("afterConnectionClosed::: {}", session.getId());
        webSocketSessions.remove(session);
    }
}
