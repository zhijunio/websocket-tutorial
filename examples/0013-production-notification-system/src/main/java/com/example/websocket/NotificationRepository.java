package com.example.websocket;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class NotificationRepository {
    private static final RowMapper<NotificationEvent> EVENT = NotificationRepository::mapEvent;
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    public NotificationRepository(JdbcTemplate jdbc) {
        this(jdbc, new ObjectMapper());
    }

    @Autowired
    public NotificationRepository(JdbcTemplate jdbc, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    @Transactional
    public NotificationEvent create(String tenantId, String eventType, String recipientType,
                                    String recipientId, String payload) {
        return create(tenantId, UUID.randomUUID().toString(), eventType, recipientType, recipientId, payload);
    }

    @Transactional
    public NotificationEvent create(String tenantId, String idempotencyKey, String eventType, String recipientType,
                                    String recipientId, String payload) {
        UUID eventId = UUID.randomUUID();
        String requestHash = requestHash(eventType, recipientType, recipientId, payload);
        int inserted = jdbc.update("INSERT INTO notification_event(id, tenant_id, idempotency_key, request_hash, event_type, recipient_type, recipient_id, payload) VALUES (?, ?, ?, ?, ?, ?, ?, ?::jsonb) ON CONFLICT DO NOTHING",
                eventId, tenantId, idempotencyKey, requestHash, eventType, recipientType, recipientId, payload);
        if (inserted == 0) {
            ExistingRequest existing = jdbc.queryForObject("SELECT id, request_hash, event_type, recipient_type, recipient_id, payload::text FROM notification_event WHERE tenant_id = ? AND idempotency_key = ?",
                    (rs, row) -> new ExistingRequest(rs.getObject("id", UUID.class), rs.getString("request_hash"),
                            rs.getString("event_type"), rs.getString("recipient_type"), rs.getString("recipient_id"), rs.getString("payload")),
                    tenantId, idempotencyKey);
            String existingHash = existing.requestHash() == null
                    ? requestHash(existing.eventType(), existing.recipientType(), existing.recipientId(), existing.payload())
                    : existing.requestHash();
            if (!requestHash.equals(existingHash)) {
                throw new IdempotencyConflictException("Idempotency-Key was already used with a different request");
            }
            if (existing.requestHash() == null) {
                jdbc.update("UPDATE notification_event SET request_hash = ? WHERE id = ? AND request_hash IS NULL", existingHash, existing.id());
            }
        }
        UUID storedEventId = inserted == 1 ? eventId : jdbc.queryForObject("SELECT id FROM notification_event WHERE tenant_id = ? AND idempotency_key = ?",
                UUID.class, tenantId, idempotencyKey);
        NotificationEvent event = jdbc.queryForObject("SELECT id, tenant_id, event_type, recipient_type, recipient_id, payload::text, sequence, created_at FROM notification_event WHERE id = ?", EVENT, storedEventId);
        if (inserted == 1) {
            jdbc.update("INSERT INTO notification_outbox(id, event_id) VALUES (?, ?)", UUID.randomUUID(), eventId);
            jdbc.update("INSERT INTO notification_delivery(event_id, tenant_id, recipient_id) VALUES (?, ?, ?)", eventId, tenantId, recipientId);
        }
        return event;
    }

    public List<NotificationEvent> after(String tenantId, String recipientId, long sequence, int limit) {
        return jdbc.query("SELECT id, tenant_id, event_type, recipient_type, recipient_id, payload::text, sequence, created_at FROM notification_event WHERE tenant_id = ? AND recipient_id = ? AND sequence > ? ORDER BY sequence LIMIT ?",
                EVENT, tenantId, recipientId, sequence, Math.min(limit, 100));
    }

    public int acknowledge(UUID eventId, String tenantId, String recipientId) {
        return jdbc.update("UPDATE notification_delivery SET status = 'ACKED', acked_at = COALESCE(acked_at, CURRENT_TIMESTAMP) WHERE event_id = ? AND tenant_id = ? AND recipient_id = ?",
                eventId, tenantId, recipientId);
    }

    @Transactional
    public List<OutboxRecord> claimOutbox(int limit) {
        List<OutboxRecord> records = jdbc.query("SELECT o.id, o.event_id, e.tenant_id, e.recipient_id, e.event_type, e.payload::text, e.sequence FROM notification_outbox o JOIN notification_event e ON e.id = o.event_id WHERE ((o.status = 'PENDING' AND o.next_attempt_at <= CURRENT_TIMESTAMP) OR (o.status = 'PROCESSING' AND o.locked_until < CURRENT_TIMESTAMP)) ORDER BY o.created_at LIMIT ? FOR UPDATE SKIP LOCKED",
                (rs, row) -> new OutboxRecord(rs.getObject("id", UUID.class), rs.getObject("event_id", UUID.class), rs.getString("tenant_id"), rs.getString("recipient_id"), rs.getString("event_type"), rs.getString("payload"), rs.getLong("sequence")), limit);
        records.forEach(record -> jdbc.update("UPDATE notification_outbox SET status = 'PROCESSING', locked_until = CURRENT_TIMESTAMP + INTERVAL '30 seconds', attempts = attempts + 1 WHERE id = ?", record.id()));
        return records;
    }

    public void markPublished(UUID outboxId) {
        jdbc.update("UPDATE notification_outbox SET status = 'PUBLISHED', published_at = CURRENT_TIMESTAMP, locked_until = NULL WHERE id = ?", outboxId);
    }

    public void markDelivered(UUID eventId, String tenantId, String recipientId) {
        jdbc.update("UPDATE notification_delivery SET status = CASE WHEN status = 'ACKED' THEN 'ACKED' ELSE 'DELIVERED' END, delivered_at = COALESCE(delivered_at, CURRENT_TIMESTAMP) WHERE event_id = ? AND tenant_id = ? AND recipient_id = ?",
                eventId, tenantId, recipientId);
    }

    public void markFailed(UUID outboxId, String error, int maxAttempts) {
        jdbc.update("UPDATE notification_outbox SET status = CASE WHEN attempts >= ? THEN 'DEAD' ELSE 'PENDING' END, next_attempt_at = CURRENT_TIMESTAMP + (LEAST(attempts, 6) * INTERVAL '5 seconds'), locked_until = NULL, last_error = ? WHERE id = ?",
                maxAttempts, error.substring(0, Math.min(error.length(), 1000)), outboxId);
    }

    private String requestHash(String eventType, String recipientType, String recipientId, String payload) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String canonicalPayload = mapper.readTree(payload).toString();
            String request = String.join("\u0000", eventType, recipientType, recipientId, canonicalPayload);
            return java.util.HexFormat.of().formatHex(digest.digest(request.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalArgumentException("payload must be valid JSON", exception);
        }
    }

    private static NotificationEvent mapEvent(ResultSet rs, int row) throws SQLException {
        return new NotificationEvent(rs.getObject("id", UUID.class), rs.getString("tenant_id"), rs.getString("event_type"), rs.getString("recipient_type"), rs.getString("recipient_id"), rs.getString("payload"), rs.getLong("sequence"), rs.getTimestamp("created_at").toInstant());
    }
}

record OutboxRecord(UUID id, UUID eventId, String tenantId, String recipientId,
                    String eventType, String payload, long sequence) {
}

record ExistingRequest(UUID id, String requestHash, String eventType, String recipientType,
                       String recipientId, String payload) {
}

class IdempotencyConflictException extends RuntimeException {
    IdempotencyConflictException(String message) {
        super(message);
    }
}
