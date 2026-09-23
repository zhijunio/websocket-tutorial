package com.example.websocket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class NotificationControllerTests {

    @Test
    void serializesNotificationTextAsValidJson() throws Exception {
        ConnectionRegistry registry = mock(ConnectionRegistry.class);
        InstanceIdentity identity = mock(InstanceIdentity.class);
        ObjectMapper objectMapper = new ObjectMapper();
        when(identity.value()).thenReturn("app-a");
        NotificationController controller = new NotificationController(registry, identity, objectMapper);

        controller.publish("alice", new NotificationController.PublishRequest("line\nquote\"slash\\"));

        ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
        verify(registry).sendToLocal(anyString(), payload.capture());
        JsonNode json = objectMapper.readTree(payload.getValue());
        assertEquals("line\nquote\"slash\\", json.get("text").asText());
    }
}
