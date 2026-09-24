package com.example.websocket;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.Principal;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

class NotificationWebSocketHandlerTests {
    @Test
    void redisCleanupFailureDoesNotPreventSocketCleanup() throws Exception {
        PresenceService presence = mock(PresenceService.class);
        ConnectionAdmissionService admission = mock(ConnectionAdmissionService.class);
        NotificationRepository notifications = mock(NotificationRepository.class);
        AppProperties properties = new AppProperties();
        WebSocketSession session = mock(WebSocketSession.class);
        Principal principal = () -> "alice";

        when(session.getId()).thenReturn("connection-1");
        when(session.isOpen()).thenReturn(true);
        when(session.getAttributes()).thenReturn(Map.of(
                TicketHandshakeInterceptor.PRINCIPAL, principal,
                "tenantId", "tenant-a",
                "clientType", "browser",
                "clientIp", "192.0.2.1"));
        when(admission.tryAcquire("tenant-a", "alice", "192.0.2.1", "connection-1")).thenReturn(true);
        doThrow(new IllegalStateException("redis unavailable"))
                .when(presence).connect("tenant-a", "alice", "browser", "connection-1");
        doThrow(new IllegalStateException("redis unavailable"))
                .when(presence).disconnect("tenant-a", "alice", "connection-1");

        NotificationWebSocketHandler handler = new NotificationWebSocketHandler(
                new ObjectMapper(), presence, notifications, properties, admission, new SimpleMeterRegistry());

        assertDoesNotThrow(() -> handler.afterConnectionEstablished(session));
        verify(session).close(any(CloseStatus.class));
    }
}
