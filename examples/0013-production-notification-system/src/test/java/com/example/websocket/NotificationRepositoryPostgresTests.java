package com.example.websocket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class NotificationRepositoryPostgresTests {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("websocket")
            .withUsername("websocket")
            .withPassword("test-password");

    private NotificationRepository repository;
    private JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        org.springframework.jdbc.datasource.DriverManagerDataSource dataSource = new org.springframework.jdbc.datasource.DriverManagerDataSource();
        dataSource.setUrl(POSTGRES.getJdbcUrl());
        dataSource.setUsername(POSTGRES.getUsername());
        dataSource.setPassword(POSTGRES.getPassword());
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("TRUNCATE notification_delivery, notification_outbox, notification_event");
        repository = new NotificationRepository(jdbc);
    }

    @Test
    void eventAndOutboxAreCommittedTogetherAndCanBeClaimed() {
        NotificationEvent event = repository.create("tenant-a", "ALARM_CREATED", "USER", "alice", "{\"text\":\"alarm\"}");

        assertNotNull(event);
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM notification_outbox WHERE event_id = ?", Integer.class, event.id()));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM notification_delivery WHERE event_id = ?", Integer.class, event.id()));

        List<OutboxRecord> claimed = repository.claimOutbox(10);

        assertEquals(1, claimed.size());
        assertEquals("PROCESSING", jdbc.queryForObject("SELECT status FROM notification_outbox WHERE id = ?", String.class, claimed.getFirst().id()));
    }

    @Test
    void ackAndDeliveryAreIdempotent() {
        NotificationEvent event = repository.create("tenant-a", "PAYMENT_UPDATED", "USER", "alice", "{\"state\":\"paid\"}");

        repository.markDelivered(event.id(), "tenant-a", "alice");
        repository.markDelivered(event.id(), "tenant-a", "alice");
        repository.acknowledge(event.id(), "tenant-a", "alice");
        repository.acknowledge(event.id(), "tenant-a", "alice");

        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM notification_delivery WHERE event_id = ? AND delivered_at IS NOT NULL AND acked_at IS NOT NULL", Integer.class, event.id()));
        assertEquals("ACKED", jdbc.queryForObject("SELECT status FROM notification_delivery WHERE event_id = ?", String.class, event.id()));
    }

    @Test
    void sameIdempotencyKeyReturnsTheOriginalEventWithoutDuplicatingOutbox() {
        NotificationEvent first = repository.create("tenant-a", "request-123", "ALARM_CREATED", "USER", "alice", "{\"text\":\"same\"}");
        NotificationEvent retry = repository.create("tenant-a", "request-123", "ALARM_CREATED", "USER", "alice", "{\"text\":\"same\"}");

        assertEquals(first.id(), retry.id());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM notification_event WHERE tenant_id = ?", Integer.class, "tenant-a"));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM notification_outbox WHERE event_id = ?", Integer.class, first.id()));
    }

    @Test
    void sameIdempotencyKeyWithDifferentRequestIsRejected() {
        repository.create("tenant-a", "request-123", "ALARM_CREATED", "USER", "alice", "{\"text\":\"same\"}");

        assertThrows(IdempotencyConflictException.class,
                () -> repository.create("tenant-a", "request-123", "ALARM_CREATED", "USER", "alice", "{\"text\":\"changed\"}"));
    }

    @Test
    void outboxMovesToDeadAfterConfiguredAttempts() {
        NotificationEvent event = repository.create("tenant-a", "ALARM_CREATED", "USER", "alice", "{\"text\":\"retry\"}");
        OutboxRecord record = repository.claimOutbox(1).getFirst();

        repository.markFailed(record.id(), "redis unavailable", 1);

        assertEquals("DEAD", jdbc.queryForObject("SELECT status FROM notification_outbox WHERE event_id = ?", String.class, event.id()));
    }
}
