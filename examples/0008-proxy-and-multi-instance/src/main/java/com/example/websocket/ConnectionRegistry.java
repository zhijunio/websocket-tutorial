package com.example.websocket;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

@Component
public class ConnectionRegistry {
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public void register(String userId, WebSocketSession session) {
        sessions.put(userId, session);
    }

    public void unregister(String userId, WebSocketSession session) {
        sessions.remove(userId, session);
    }

    public boolean sendToLocal(String userId, String payload) {
        WebSocketSession session = sessions.get(userId);
        if (session == null || !session.isOpen()) {
            return false;
        }
        try {
            synchronized (session) {
                session.sendMessage(new TextMessage(payload));
            }
            return true;
        } catch (IOException exception) {
            sessions.remove(userId, session);
            return false;
        }
    }

    int size() {
        return sessions.size();
    }
}
