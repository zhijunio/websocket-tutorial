package com.example.websocket;

import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class NotificationController {
    private final ConnectionRegistry registry;
    private final InstanceIdentity identity;
    private final ObjectMapper objectMapper;

    public NotificationController(ConnectionRegistry registry, InstanceIdentity identity,
                                  ObjectMapper objectMapper) {
        this.registry = registry;
        this.identity = identity;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/users/{userId}/notifications")
    public Map<String, Object> publish(@PathVariable String userId,
                                       @RequestBody PublishRequest request) {
        String text = request.text() == null ? "" : request.text();
        String payload;
        try {
            payload = objectMapper.writeValueAsString(Map.of(
                    "type", "notification",
                    "text", text));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("notification payload could not be serialized", exception);
        }
        boolean delivered = registry.sendToLocal(userId, payload);
        return Map.of("instance", identity.value(), "userId", userId, "deliveredLocally", delivered);
    }

    @GetMapping("/instance")
    public Map<String, String> instance() {
        return Map.of("instance", identity.value());
    }

    record PublishRequest(String text) {
    }
}
