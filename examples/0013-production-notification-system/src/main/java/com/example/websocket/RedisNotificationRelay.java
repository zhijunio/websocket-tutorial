package com.example.websocket;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.PendingMessage;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.RedisStreamCommands;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RedisNotificationRelay {
    private static final Logger log = LoggerFactory.getLogger(RedisNotificationRelay.class);
    static final String STREAM = "ws:notification:events";
    private final StringRedisTemplate redis;
    private final StreamOperations<String, String, String> streams;
    private final NotificationRepository notifications;
    private final NotificationWebSocketHandler handler;
    private final ObjectMapper mapper;
    private final String group;
    private final String consumer;
    private final AppProperties properties;
    private final AtomicBoolean groupReady = new AtomicBoolean();
    private final Counter groupFailures;

    public RedisNotificationRelay(StringRedisTemplate redis, NotificationRepository notifications,
                                  NotificationWebSocketHandler handler, AppProperties properties,
                                  MeterRegistry meters) {
        this.redis = redis;
        this.streams = redis.opsForStream();
        this.notifications = notifications;
        this.handler = handler;
        this.mapper = new ObjectMapper();
        this.properties = properties;
        this.group = "ws-instance-" + properties.getInstanceId();
        this.consumer = properties.getInstanceId();
        this.groupFailures = meters.counter("websocket.redis.stream.group.failures", "instance", properties.getInstanceId());
    }

    @Scheduled(fixedDelay = 500)
    public void publishOutbox() {
        for (OutboxRecord record : notifications.claimOutbox(50)) {
            try {
                streams.add(MapRecord.create(STREAM, Map.of("eventId", record.eventId().toString(), "tenantId", record.tenantId(),
                        "recipientId", record.recipientId(), "eventType", record.eventType(), "payload", record.payload(), "sequence", Long.toString(record.sequence()))),
                        RedisStreamCommands.XAddOptions.maxlen(properties.getStreamMaxLength()).approximateTrimming(true));
                notifications.markPublished(record.id());
            } catch (Exception exception) {
                notifications.markFailed(record.id(), exception.getMessage() == null ? "publish failed" : exception.getMessage(), properties.getMaxOutboxAttempts());
            }
        }
    }

    @Scheduled(fixedDelay = 200)
    public void consumeEvents() {
        if (!ensureGroup()) {
            return;
        }
        try {
            List<PendingMessage> pending = streams.pending(STREAM, group, Range.unbounded(), 50, Duration.ofSeconds(30)).stream().toList();
            if (!pending.isEmpty()) {
                process(streams.claim(STREAM, group, consumer, Duration.ofSeconds(30), pending.stream().map(PendingMessage::getId).toArray(org.springframework.data.redis.connection.stream.RecordId[]::new)));
            }
            List<MapRecord<String, String, String>> records = streams.read(Consumer.from(group, consumer),
                    StreamReadOptions.empty().count(50).block(Duration.ofMillis(100)),
                    StreamOffset.create(STREAM, ReadOffset.lastConsumed()));
            if (records == null) {
                return;
            }
            process(records);
        } catch (Exception exception) {
            groupReady.set(false);
            groupFailures.increment();
            log.warn("Redis notification stream consume failed; will retry", exception);
        }
    }

    private String payload(Map<String, String> values) {
        try {
            return mapper.writeValueAsString(Map.of("type", "notification", "messageId", UUID.randomUUID().toString(), "eventId", values.get("eventId"), "sequence", Long.parseLong(values.get("sequence")), "eventType", values.get("eventType"), "payload", mapper.readTree(values.get("payload"))));
        } catch (Exception exception) {
            throw new IllegalStateException("stream payload is invalid", exception);
        }
    }

    private void process(List<MapRecord<String, String, String>> records) {
        if (records == null) {
            return;
        }
        for (MapRecord<String, String, String> record : records) {
            Map<String, String> values = record.getValue();
            boolean delivered = handler.send(values.get("tenantId"), values.get("recipientId"), payload(values));
            if (delivered) {
                notifications.markDelivered(UUID.fromString(values.get("eventId")), values.get("tenantId"), values.get("recipientId"));
            }
            streams.acknowledge(STREAM, group, record.getId());
        }
    }

    private boolean ensureGroup() {
        if (groupReady.get()) {
            return true;
        }
        try {
            streams.createGroup(STREAM, ReadOffset.from("0-0"), group);
            groupReady.set(true);
            return true;
        } catch (Exception exception) {
            if (exception.getMessage() != null && exception.getMessage().contains("BUSYGROUP")) {
                groupReady.set(true);
                return true;
            }
            groupFailures.increment();
            log.warn("Redis notification stream group is not ready; will retry", exception);
            return false;
        }
    }
}
