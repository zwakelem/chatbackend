// java
package com.example.chatbackend.service;

import com.example.chatbackend.data.DataStore;
import com.example.chatbackend.model.Message;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final List<WebSocketSession> webSocketSessions = new CopyOnWriteArrayList<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final UserService userService;
    private final DataStore dataStore;

    private final BlockingQueue<Incoming> messageQueue = new LinkedBlockingQueue<>();
    private final ExecutorService processor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "chat-message-processor");
        t.setDaemon(true);
        return t;
    });

    // start background processor
    {
        processor.submit(this::processLoop);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("afterConnectionEstablished::: {}", session.getId());
        webSocketSessions.add(session);
    }

    @Override
    public void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) {
        messageQueue.offer(new Incoming(session, message.getPayload()));
    }

    private void processLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Incoming incoming = messageQueue.take();
                try {
                    Message msg = objectMapper.readValue(incoming.payload(), Message.class);
                    Message messageWithTimestamp = new Message(msg.sender(), LocalDateTime.now(), msg.message());

                    if (userExists(messageWithTimestamp.sender())) {
                        sendTextMessage(messageWithTimestamp);
                        try {
                            dataStore.addMessage(messageWithTimestamp, messageWithTimestamp.sender());
                        } catch (Exception dsEx) {
                            log.error("Failed to store message for {}: {}", messageWithTimestamp.sender(), dsEx.getMessage(), dsEx);
                        }
                        log.info("handleTextMessage (async):: messages={}", dataStore.getUserMessages(messageWithTimestamp.sender()));
                    } else {
                        sendErrorResponse(incoming.session(), messageWithTimestamp);
                    }
                } catch (Exception e) {
                    log.error("processLoop::: Failed to parse/handle payload: {}", incoming.payload(), e);
                    try {
                        incoming.session().sendMessage(new TextMessage("{\"error\":\"invalid message payload\"}"));
                    } catch (IOException ioEx) {
                        log.warn("Failed to notify client about parsing error: {}", ioEx.getMessage());
                    }
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static void sendErrorResponse(@NonNull WebSocketSession session, Message messageWithTimestamp) {
        log.warn("sendErrorResponse::: this user={} does not exist, message is ignored!", messageWithTimestamp.sender());
        try {
            session.sendMessage(new TextMessage("{\"error\":\"this user does not exist, create user first!!\"}"));
        } catch (Exception sendEx) {
            log.warn("sendErrorResponse::: Failed to send error message to client: {}", sendEx.getMessage());
        }
    }

    private void sendTextMessage(Message messageWithTimestamp) throws IOException {
        String out = objectMapper.writeValueAsString(messageWithTimestamp);
        TextMessage outMessage = new TextMessage(out);
        for (WebSocketSession webSocketSession : webSocketSessions) {
            webSocketSession.sendMessage(outMessage);
        }
    }

    private boolean userExists(String sender) {
        return userService.exists(sender);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, @NonNull CloseStatus status) throws Exception {
        log.info("afterConnectionClosed::: {}", session.getId());
        webSocketSessions.remove(session);
    }

    @PreDestroy
    public void shutdown() {
        processor.shutdownNow();
        try {
            if (!processor.awaitTermination(2, TimeUnit.SECONDS)) {
                processor.shutdownNow();
            }
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    private static record Incoming(WebSocketSession session, String payload) {}
}
