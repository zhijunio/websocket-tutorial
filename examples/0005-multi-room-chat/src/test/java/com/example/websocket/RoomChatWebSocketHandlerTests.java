package com.example.websocket;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

class RoomChatWebSocketHandlerTests {

    private RoomChatWebSocketHandler handler;
    private WebSocketSession alice;
    private WebSocketSession bob;

    @BeforeEach
    void setUp() {
        handler = new RoomChatWebSocketHandler();
        alice = session("alice");
        bob = session("bob");
    }

    @Test
    void broadcastsOnlyToMembersOfTheSameRoom() throws Exception {
        join(alice, "red");
        join(bob, "blue");
        clearInvocations(alice, bob);

        send(alice, "{\"type\":\"chat\",\"room\":\"red\",\"text\":\"hello\"}");

        verify(alice).sendMessage(argThat((TextMessage message) -> message.getPayload().contains("hello")));
        verify(bob, never()).sendMessage(org.mockito.ArgumentMatchers.any(TextMessage.class));
    }

    @Test
    void removesAllRoomMembershipsWhenConnectionCloses() throws Exception {
        join(alice, "red");
        join(alice, "blue");

        handler.afterConnectionClosed(alice, org.springframework.web.socket.CloseStatus.NORMAL);

        org.junit.jupiter.api.Assertions.assertEquals(0, handler.roomMemberCount("red"));
        org.junit.jupiter.api.Assertions.assertEquals(0, handler.roomMemberCount("blue"));
    }

    private void join(WebSocketSession session, String room) throws Exception {
        send(session, "{\"type\":\"join\",\"room\":\"" + room + "\"}");
    }

    private void send(WebSocketSession session, String json) throws Exception {
        handler.handleMessage(session, new TextMessage(json));
    }

    private WebSocketSession session(String id) {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn(id);
        when(session.isOpen()).thenReturn(true);
        return session;
    }
}
