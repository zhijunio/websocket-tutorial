package com.example.websocket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

class NotificationServiceTests {

    private NotificationService notifications;
    private WebSocketSession session;

    @BeforeEach
    void setUp() {
        notifications = new NotificationService();
        session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(true);
    }

    @Test
    void storesNotificationWhenUserIsOffline() {
        NotificationService.Notification notification = notifications.publish("u1", "hello");

        assertEquals(notification, notifications.unread("u1").getFirst());
        assertEquals(1, notifications.unreadCount("u1"));
    }

    @Test
    void acknowledgesOnlyTheRequestedNotification() {
        NotificationService.Notification first = notifications.publish("u1", "one");
        notifications.publish("u1", "two");

        notifications.acknowledge("u1", first.id());

        assertEquals(1, notifications.unreadCount("u1"));
        assertEquals("two", notifications.unread("u1").getFirst().text());
    }

    @Test
    void serializesOnlineNotificationTextAsValidJson() throws Exception {
        notifications.connect("u1", session);

        notifications.publish("u1", "line\nquote\"slash\\");

        ArgumentCaptor<TextMessage> messages = ArgumentCaptor.forClass(TextMessage.class);
        verify(session).sendMessage(messages.capture());
        JsonNode json = new ObjectMapper().readTree(messages.getValue().getPayload());
        assertEquals("line\nquote\"slash\\", json.get("text").asText());
    }
}
