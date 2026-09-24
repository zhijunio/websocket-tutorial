package com.example.websocket;

import java.time.Instant;
import java.util.UUID;

public record NotificationEvent(UUID id, String tenantId, String eventType, String recipientType,
                                String recipientId, String payload, long sequence, Instant createdAt) {
}
