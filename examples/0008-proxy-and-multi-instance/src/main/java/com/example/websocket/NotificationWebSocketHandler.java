package com.example.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

@Component
public class NotificationWebSocketHandler extends AbstractWebSocketHandler {
    private final ConnectionRegistry registry;

    public NotificationWebSocketHandler(ConnectionRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        registry.register(userId(session), session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        registry.unregister(userId(session), session);
    }

    private String userId(WebSocketSession session) {
        Object value = session.getAttributes().get("userId");
        return value == null ? "anonymous" : value.toString();
    }
}
