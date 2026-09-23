package com.example.websocket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

class HeartbeatWebSocketHandlerTests {

    private HeartbeatWebSocketHandler handler;
    private WebSocketSession session;

    @BeforeEach
    void setUp() {
        handler = new HeartbeatWebSocketHandler();
        session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("session-1");
        when(session.isOpen()).thenReturn(true);
    }

    @Test
    void repliesToApplicationPing() throws Exception {
        handler.afterConnectionEstablished(session);
        handler.handleMessage(session, new TextMessage("{\"type\":\"ping\",\"id\":\"p1\"}"));

        verify(session).sendMessage(argThat((TextMessage message) ->
                message.getPayload().contains("\"type\":\"pong\"")
                        && message.getPayload().contains("\"id\":\"p1\"")));
    }

    @Test
    void removesSessionAfterClose() throws Exception {
        handler.afterConnectionEstablished(session);
        handler.afterConnectionClosed(session, org.springframework.web.socket.CloseStatus.NORMAL);

        assertEquals(0, handler.activeSessionCount());
    }
}
