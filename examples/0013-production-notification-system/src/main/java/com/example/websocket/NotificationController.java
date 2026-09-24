package com.example.websocket;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;

@RestController
public class NotificationController {
    private final NotificationRepository notifications;
    private final AppProperties properties;
    private final ObjectMapper mapper;
    private final NotificationRateLimiter rateLimiter;

    public NotificationController(NotificationRepository notifications, AppProperties properties, ObjectMapper mapper,
                                   NotificationRateLimiter rateLimiter) {
        this.notifications = notifications;
        this.properties = properties;
        this.mapper = mapper;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/api/notifications")
    public NotificationEvent create(Principal principal, @RequestBody CreateNotification request,
                                    @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        if (!rateLimiter.allow(properties.getTenantId(), principal.getName())) {
            throw new ResponseStatusException(TOO_MANY_REQUESTS, "notification rate limit exceeded");
        }
        if (request.eventType() == null || request.eventType().isBlank()
                || request.recipientType() == null || request.recipientType().isBlank()
                || request.recipientId() == null || request.recipientId().isBlank() || request.payload() == null
                || idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > 200) {
            throw new IllegalArgumentException("recipientId, payload and Idempotency-Key are required");
        }
        try {
            mapper.readTree(request.payload());
        } catch (Exception exception) {
            throw new IllegalArgumentException("payload must be valid JSON", exception);
        }
        return notifications.create(properties.getTenantId(), idempotencyKey, request.eventType(), request.recipientType(), request.recipientId(), request.payload());
    }

    @GetMapping("/api/notifications")
    public List<NotificationEvent> replay(Principal principal, @RequestParam(defaultValue = "0") long after,
                                          @RequestParam(defaultValue = "100") int limit) {
        return notifications.after(properties.getTenantId(), principal.getName(), Math.max(after, 0), Math.min(limit, 100));
    }

    @PostMapping("/api/notifications/{eventId}/ack")
    public Map<String, Object> acknowledge(Principal principal, @PathVariable UUID eventId) {
        if (notifications.acknowledge(eventId, properties.getTenantId(), principal.getName()) == 0) {
            throw new ResponseStatusException(NOT_FOUND, "notification not found");
        }
        return Map.of("eventId", eventId, "acknowledged", true);
    }

    record CreateNotification(String eventType, String recipientType, String recipientId, String payload) {
    }
}
