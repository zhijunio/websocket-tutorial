package com.example.websocket;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

@Service
public class NotificationService {

    private final AtomicLong sequence = new AtomicLong();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, WebSocketSession> onlineUsers = new ConcurrentHashMap<>();
    private final Map<String, List<Notification>> unread = new ConcurrentHashMap<>();

    public void connect(String userId, WebSocketSession session) {
        onlineUsers.put(userId, session);
    }

    public void disconnect(String userId, WebSocketSession session) {
        onlineUsers.remove(userId, session);
    }

    public Notification publish(String userId, String text) {
        Notification notification = new Notification(
                Long.toString(sequence.incrementAndGet()), userId,
                text == null ? "" : text, Instant.now());
        WebSocketSession session = onlineUsers.get(userId);
        if (session != null && session.isOpen()) {
            try {
                synchronized (session) {
                    session.sendMessage(new TextMessage(notification.toJson(objectMapper)));
                }
                return notification;
            } catch (Exception exception) {
                onlineUsers.remove(userId, session);
            }
        }
        unread.computeIfAbsent(userId, ignored -> new CopyOnWriteArrayList<>()).add(notification);
        return notification;
    }

    public List<Notification> unread(String userId) {
        return List.copyOf(unread.getOrDefault(userId, List.of()));
    }

    public boolean acknowledge(String userId, String notificationId) {
        List<Notification> notifications = unread.get(userId);
        if (notifications == null) {
            return false;
        }
        boolean removed = notifications.removeIf(notification -> notification.id().equals(notificationId));
        if (notifications.isEmpty()) {
            unread.remove(userId, notifications);
        }
        return removed;
    }

    int unreadCount(String userId) {
        return unread.getOrDefault(userId, List.of()).size();
    }

    record Notification(String id, String userId, String text, Instant createdAt) {

        String toJson(ObjectMapper objectMapper) throws JsonProcessingException {
            return objectMapper.writeValueAsString(Map.of(
                    "type", "notification",
                    "id", id,
                    "text", text,
                    "createdAt", createdAt.toString()));
        }
    }
}
