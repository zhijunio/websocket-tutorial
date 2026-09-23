package com.example.websocket;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class RoomChatWebSocketHandler extends TextWebSocketHandler {

    private static final Pattern ROOM_NAME = Pattern.compile("[A-Za-z0-9_-]{1,32}");
    private static final int MAX_TEXT_LENGTH = 256;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, Set<WebSocketSession>> rooms = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> sessionRooms = new ConcurrentHashMap<>();

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            ClientCommand command = objectMapper.readValue(message.getPayload(), ClientCommand.class);
            if (command.type() == null) {
                sendError(session, "missing_type", "type is required");
                return;
            }
            switch (command.type()) {
                case "join" -> join(session, command.room());
                case "leave" -> leave(session, command.room());
                case "chat" -> chat(session, command.room(), command.text());
                default -> sendError(session, "unsupported_type", "type must be join, leave or chat");
            }
        } catch (JsonProcessingException exception) {
            sendError(session, "invalid_json", "message must be valid JSON");
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        removeSession(session);
    }

    int roomMemberCount(String room) {
        return rooms.getOrDefault(room, Set.of()).size();
    }

    private void join(WebSocketSession session, String room) {
        if (!validRoom(room)) {
            sendError(session, "invalid_room", "room must match [A-Za-z0-9_-]{1,32}");
            return;
        }

        Set<WebSocketSession> members = rooms.computeIfAbsent(room, ignored -> ConcurrentHashMap.newKeySet());
        if (!members.add(session)) {
            sendError(session, "already_joined", "session is already in this room");
            return;
        }
        sessionRooms.computeIfAbsent(session.getId(), ignored -> ConcurrentHashMap.newKeySet()).add(room);

        send(session, ServerEvent.joined(room, members.size()));
        broadcast(room, ServerEvent.memberJoined(room, session.getId(), members.size()), session);
    }

    private void leave(WebSocketSession session, String room) {
        Set<WebSocketSession> members = rooms.get(room);
        if (members == null || !members.remove(session)) {
            sendError(session, "not_joined", "session is not in this room");
            return;
        }
        removeSessionRoom(session, room);
        removeEmptyRoom(room, members);

        send(session, ServerEvent.left(room, members.size()));
        broadcast(room, ServerEvent.memberLeft(room, session.getId(), members.size()), session);
    }

    private void chat(WebSocketSession session, String room, String text) {
        Set<WebSocketSession> members = rooms.get(room);
        if (members == null || !members.contains(session)) {
            sendError(session, "not_joined", "join this room before sending messages");
            return;
        }
        if (text == null || text.isBlank() || text.length() > MAX_TEXT_LENGTH) {
            sendError(session, "invalid_text", "text must contain 1 to 256 characters");
            return;
        }
        broadcast(room, ServerEvent.message(room, session.getId(), text), null);
    }

    private void removeSession(WebSocketSession session) {
        Set<String> joinedRooms = sessionRooms.remove(session.getId());
        if (joinedRooms == null) {
            return;
        }
        for (String room : new ArrayList<>(joinedRooms)) {
            Set<WebSocketSession> members = rooms.get(room);
            if (members != null && members.remove(session)) {
                removeEmptyRoom(room, members);
                broadcast(room, ServerEvent.memberLeft(room, session.getId(), members.size()), null);
            }
        }
    }

    private void removeSessionRoom(WebSocketSession session, String room) {
        Set<String> joinedRooms = sessionRooms.get(session.getId());
        if (joinedRooms != null) {
            joinedRooms.remove(room);
            if (joinedRooms.isEmpty()) {
                sessionRooms.remove(session.getId(), joinedRooms);
            }
        }
    }

    private void removeEmptyRoom(String room, Set<WebSocketSession> members) {
        if (members.isEmpty()) {
            rooms.remove(room, members);
        }
    }

    private void broadcast(String room, ServerEvent event, WebSocketSession excluded) {
        Set<WebSocketSession> members = rooms.get(room);
        if (members == null) {
            return;
        }
        for (WebSocketSession member : new ArrayList<>(members)) {
            if (member != excluded) {
                send(member, event);
            }
        }
    }

    private void sendError(WebSocketSession session, String code, String message) {
        send(session, ServerEvent.error(code, message));
    }

    private void send(WebSocketSession session, ServerEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            synchronized (session) {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(payload));
                }
            }
        } catch (IOException exception) {
            removeSession(session);
        }
    }

    private boolean validRoom(String room) {
        return room != null && ROOM_NAME.matcher(room).matches();
    }

    record ClientCommand(String type, String room, String text) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record ServerEvent(String type, String room, String from, String text,
                       Integer members, String code, String message) {

        static ServerEvent joined(String room, int members) {
            return new ServerEvent("joined", room, null, null, members, null, null);
        }

        static ServerEvent left(String room, int members) {
            return new ServerEvent("left", room, null, null, members, null, null);
        }

        static ServerEvent memberJoined(String room, String from, int members) {
            return new ServerEvent("member_joined", room, from, null, members, null, null);
        }

        static ServerEvent memberLeft(String room, String from, int members) {
            return new ServerEvent("member_left", room, from, null, members, null, null);
        }

        static ServerEvent message(String room, String from, String text) {
            return new ServerEvent("message", room, from, text, null, null, null);
        }

        static ServerEvent error(String code, String message) {
            return new ServerEvent("error", null, null, null, null, code, message);
        }
    }
}
