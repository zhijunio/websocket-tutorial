package com.example.websocket;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

@Component
public class DeviceConnectionRegistry {
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public void register(String deviceId, WebSocketSession session) {
        sessions.put(deviceId, session);
    }

    public void unregister(String deviceId, WebSocketSession session) {
        sessions.remove(deviceId, session);
    }

    public boolean sendToDevice(String deviceId, String payload) {
        WebSocketSession session = sessions.get(deviceId);
        if (session == null || !session.isOpen()) {
            return false;
        }
        try {
            synchronized (session) {
                session.sendMessage(new TextMessage(payload));
            }
            return true;
        } catch (IOException exception) {
            sessions.remove(deviceId, session);
            return false;
        }
    }
}
