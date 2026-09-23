package com.example.websocket;

import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

@Component
public class NotificationWebSocketHandler extends AbstractWebSocketHandler {

    private final NotificationService notifications;

    public NotificationWebSocketHandler(NotificationService notifications) {
        this.notifications = notifications;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String userId = userId(session);
        notifications.connect(userId, session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        notifications.disconnect(userId(session), session);
    }

    private String userId(WebSocketSession session) {
        Object value = session.getAttributes().get("userId");
        return value == null ? "anonymous" : value.toString();
    }
}
