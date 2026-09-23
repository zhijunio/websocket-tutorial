package com.example.websocket;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class HeartbeatWebSocketHandler extends TextWebSocketHandler {

    private static final Duration IDLE_TIMEOUT = Duration.ofSeconds(15);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, Instant> lastActivity = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.put(session.getId(), session);
        lastActivity.put(session.getId(), Instant.now());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        lastActivity.put(session.getId(), Instant.now());
        try {
            ClientMessage clientMessage = objectMapper.readValue(message.getPayload(), ClientMessage.class);
            if ("ping".equals(clientMessage.type())) {
                send(session, ServerMessage.pong(clientMessage.id()));
            } else {
                send(session, ServerMessage.error("unsupported_type", "type must be ping"));
            }
        } catch (JsonProcessingException exception) {
            send(session, ServerMessage.error("invalid_json", "message must be valid JSON"));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        remove(session);
    }

    @Scheduled(fixedDelay = 5_000)
    void closeIdleSessions() {
        Instant deadline = Instant.now().minus(IDLE_TIMEOUT);
        lastActivity.forEach((id, activity) -> {
            if (activity.isBefore(deadline)) {
                WebSocketSession session = sessions.get(id);
                if (session != null) {
                    try {
                        session.close(CloseStatus.GOING_AWAY);
                    } catch (IOException ignored) {
                        // The connection is already unusable; cleanup still matters.
                    }
                    remove(session);
                }
            }
        });
    }

    int activeSessionCount() {
        return sessions.size();
    }

    private void send(WebSocketSession session, ServerMessage message) {
        try {
            String payload = objectMapper.writeValueAsString(message);
            synchronized (session) {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(payload));
                }
            }
        } catch (IOException exception) {
            remove(session);
        }
    }

    private void remove(WebSocketSession session) {
        sessions.remove(session.getId(), session);
        lastActivity.remove(session.getId());
    }

    record ClientMessage(String type, String id) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record ServerMessage(String type, String id, String code, String message) {

        static ServerMessage pong(String id) {
            return new ServerMessage("pong", id, null, null);
        }

        static ServerMessage error(String code, String message) {
            return new ServerMessage("error", null, code, message);
        }
    }
}
