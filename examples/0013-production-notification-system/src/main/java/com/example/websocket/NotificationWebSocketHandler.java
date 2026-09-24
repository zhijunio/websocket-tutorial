package com.example.websocket;

import java.security.Principal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import jakarta.annotation.PreDestroy;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Gauge;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PongMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class NotificationWebSocketHandler extends TextWebSocketHandler {
    private final Map<String, Connection> connections = new ConcurrentHashMap<>();
    private final ObjectMapper mapper;
    private final PresenceService presence;
    private final ConnectionAdmissionService admission;
    private final NotificationRepository notifications;
    private final AppProperties properties;
    private final Counter opened;
    private final Counter closed;
    private final Counter queueRejected;
    private final Counter received;
    private final Counter backendFailures;

    public NotificationWebSocketHandler(ObjectMapper mapper, PresenceService presence,
                                        NotificationRepository notifications, AppProperties properties,
                                        ConnectionAdmissionService admission, MeterRegistry meters) {
        this.mapper = mapper;
        this.presence = presence;
        this.notifications = notifications;
        this.admission = admission;
        this.properties = properties;
        this.opened = meters.counter("websocket.connections.opened", "instance", properties.getInstanceId());
        this.closed = meters.counter("websocket.connections.closed", "instance", properties.getInstanceId());
        this.queueRejected = meters.counter("websocket.outbound.queue.rejected", "instance", properties.getInstanceId());
        this.received = meters.counter("websocket.messages.received", "instance", properties.getInstanceId());
        this.backendFailures = meters.counter("websocket.backend.failures", "instance", properties.getInstanceId());
        Gauge.builder("websocket.connections.active", connections, Map::size)
                .tag("instance", properties.getInstanceId()).register(meters);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Principal principal = (Principal) session.getAttributes().get(TicketHandshakeInterceptor.PRINCIPAL);
        if (principal == null) {
            close(session, CloseStatus.POLICY_VIOLATION);
            return;
        }
        String tenantId = String.valueOf(session.getAttributes().getOrDefault("tenantId", properties.getTenantId()));
        String clientType = String.valueOf(session.getAttributes().getOrDefault("clientType", "browser"));
        String clientIp = String.valueOf(session.getAttributes().getOrDefault("clientIp", "unknown"));
        try {
            if (!admission.tryAcquire(tenantId, principal.getName(), clientIp, session.getId())) {
                close(session, CloseStatus.SERVICE_OVERLOAD);
                return;
            }
        } catch (RuntimeException exception) {
            close(session, CloseStatus.SERVICE_OVERLOAD);
            return;
        }
        Connection connection = new Connection(principal.getName(), tenantId, clientType, session,
                clientIp,
                new QueuedConnection(session, properties.getOutboundQueueCapacity(), () -> remove(session.getId())));
        if (connections.putIfAbsent(session.getId(), connection) != null) {
            admission.release(tenantId, principal.getName(), clientIp, session.getId());
            close(session, CloseStatus.SERVICE_OVERLOAD);
            return;
        }
        opened.increment();
        try {
            presence.connect(tenantId, principal.getName(), clientType, session.getId());
        } catch (RuntimeException exception) {
            remove(session.getId());
            close(session, CloseStatus.SERVICE_OVERLOAD);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        Connection connection = connections.get(session.getId());
        if (connection == null) {
            close(session, CloseStatus.POLICY_VIOLATION);
            return;
        }
        if (message.getPayloadLength() > properties.getMaxMessageBytes()) {
            close(session, CloseStatus.TOO_BIG_TO_PROCESS);
            return;
        }
        try {
            received.increment();
            JsonNode node = mapper.readTree(message.getPayload());
            String type = node.path("type").asText();
            if (type.isBlank()) {
                throw new IllegalArgumentException("message type is required");
            }
            switch (type) {
                case "ping" -> {
                    presence.heartbeat(connection.tenantId(), connection.userId(), session.getId());
                    admission.refresh(connection.tenantId(), connection.userId(), connection.clientIp(), session.getId());
                    connection.send(json("pong", node.path("messageId").asText(UUID.randomUUID().toString()), null));
                }
                case "ack" -> {
                    UUID eventId = UUID.fromString(node.path("eventId").asText());
                    notifications.acknowledge(eventId, connection.tenantId(), connection.userId());
                }
                case "resume" -> replay(connection, node.path("after").asLong(0));
                case "close" -> close(session, CloseStatus.NORMAL);
                default -> connection.send(json("error", UUID.randomUUID().toString(), "unsupported_message_type"));
            }
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            connection.send(json("error", UUID.randomUUID().toString(), "invalid_message"));
        } catch (Exception exception) {
            backendFailures.increment();
            connection.send(json("error", UUID.randomUUID().toString(), "backend_unavailable"));
        }
    }

    @Override
    protected void handlePongMessage(WebSocketSession session, PongMessage message) {
        Connection connection = connections.get(session.getId());
        if (connection != null) {
            connection.pong();
            presence.heartbeat(connection.tenantId(), connection.userId(), session.getId());
            admission.refresh(connection.tenantId(), connection.userId(), connection.clientIp(), session.getId());
        }
    }

    @org.springframework.scheduling.annotation.Scheduled(fixedDelayString = "${app.heartbeat-interval:15s}")
    public void heartbeat() {
        long now = System.currentTimeMillis();
        for (Connection connection : connections.values()) {
            if (now - connection.lastPongAt() > properties.getPresenceTtl().toMillis()) {
                close(connection.session(), CloseStatus.SESSION_NOT_RELIABLE);
            } else if (!connection.queued().offerPing()) {
                close(connection.session(), CloseStatus.SESSION_NOT_RELIABLE);
            }
        }
    }

    @PreDestroy
    public void shutdown() {
        String message = json("server_shutdown", UUID.randomUUID().toString(), Map.of("instanceId", properties.getInstanceId()));
        for (Connection connection : connections.values()) {
            if (!connection.queued().closeAfter(message)) {
                close(connection.session(), CloseStatus.GOING_AWAY);
            }
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        remove(session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        remove(session.getId());
    }

    public boolean send(String tenantId, String userId, String payload) {
        boolean delivered = false;
        for (Connection connection : connections.values()) {
            if (connection.tenantId().equals(tenantId) && connection.userId().equals(userId)) {
                if (connection.send(payload)) {
                    delivered = true;
                } else {
                    queueRejected.increment();
                    remove(connection.session().getId());
                }
            }
        }
        return delivered;
    }

    public int connectionCount() {
        return connections.size();
    }

    private void replay(Connection connection, long after) {
        notifications.after(connection.tenantId(), connection.userId(), after, 100)
                .forEach(event -> connection.send(notificationJson(event)));
    }

    private String notificationJson(NotificationEvent event) {
        try {
            Map<String, Object> value = Map.of("type", "notification", "messageId", UUID.randomUUID().toString(),
                    "timestamp", Instant.now().toString(), "eventId", event.id(), "sequence", event.sequence(),
                    "eventType", event.eventType(), "payload", mapper.readTree(event.payload()));
            return mapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("websocket notification could not be serialized", exception);
        }
    }

    private String json(String type, String messageId, Object payload) {
        try {
            Map<String, Object> value = Map.of("type", type, "messageId", messageId,
                    "timestamp", Instant.now().toString(), "payload", payload == null ? Map.of() : payload);
            return mapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("websocket message could not be serialized", exception);
        }
    }

    private void remove(String connectionId) {
        Connection connection = connections.remove(connectionId);
        if (connection != null) {
            closed.increment();
            try {
                presence.disconnect(connection.tenantId(), connection.userId(), connectionId);
            } catch (RuntimeException ignored) {
                // Redis leases have TTLs; local resources must still be closed.
            }
            try {
                admission.release(connection.tenantId(), connection.userId(), connection.clientIp(), connectionId);
            } catch (RuntimeException ignored) {
                // The Redis admission lease will expire if Redis is unavailable.
            }
            connection.queued().close();
        }
    }

    private void close(WebSocketSession session, CloseStatus status) {
        try {
            session.close(status);
        } catch (Exception ignored) {
            // The close callback performs registry cleanup.
        }
    }

    static final class Connection {
        private final String userId;
        private final String tenantId;
        private final String clientType;
        private final WebSocketSession session;
        private final String clientIp;
        private final QueuedConnection queued;
        private final AtomicLong lastPong = new AtomicLong(System.currentTimeMillis());

        Connection(String userId, String tenantId, String clientType, WebSocketSession session, String clientIp,
                   QueuedConnection queued) {
            this.userId = userId;
            this.tenantId = tenantId;
            this.clientType = clientType;
            this.session = session;
            this.clientIp = clientIp;
            this.queued = queued;
        }

        String userId() { return userId; }
        String tenantId() { return tenantId; }
        String clientType() { return clientType; }
        WebSocketSession session() { return session; }
        QueuedConnection queued() { return queued; }
        String clientIp() { return clientIp; }
        long lastPongAt() { return lastPong.get(); }
        void pong() { lastPong.set(System.currentTimeMillis()); }
        boolean send(String payload) { return queued.offer(payload); }
    }
}
