package com.example.websocket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;

class ConnectionRegistryTests {
    @Test
    void unregisterDoesNotRemoveAReplacementSession() {
        ConnectionRegistry registry = new ConnectionRegistry();
        WebSocketSession oldSession = mock(WebSocketSession.class);
        WebSocketSession newSession = mock(WebSocketSession.class);
        registry.register("alice", oldSession);
        registry.register("alice", newSession);
        registry.unregister("alice", oldSession);
        assertEquals(1, registry.size());
    }

    @Test
    void closedSessionCannotReceiveLocalMessage() {
        ConnectionRegistry registry = new ConnectionRegistry();
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(false);
        registry.register("alice", session);
        assertFalse(registry.sendToLocal("alice", "payload"));
    }
}
