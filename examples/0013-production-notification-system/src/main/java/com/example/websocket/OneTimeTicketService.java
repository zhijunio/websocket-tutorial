package com.example.websocket;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class OneTimeTicketService {
    private final TicketStore store;
    private final long ttlSeconds;
    private final SecureRandom random = new SecureRandom();

    @Autowired
    public OneTimeTicketService(TicketStore store, AppProperties properties) {
        this(store, properties.getTicketTtl().toSeconds());
    }

    OneTimeTicketService(TicketStore store, long ttlSeconds) {
        this.store = Objects.requireNonNull(store);
        this.ttlSeconds = ttlSeconds;
    }

    public IssuedTicket issue(String subject, String tenantId, String clientType) {
        String value = randomValue();
        Instant expiresAt = Instant.now().plusSeconds(ttlSeconds);
        store.put(value, new StoredTicket(subject, tenantId, clientType, "websocket", value, expiresAt), ttlSeconds);
        return new IssuedTicket(value, expiresAt);
    }

    public TicketClaims consume(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        StoredTicket ticket = store.consume(value);
        if (ticket == null || ticket.expiresAt().isBefore(Instant.now())) {
            return null;
        }
        return new TicketClaims(ticket.subject(), ticket.tenantId(), ticket.clientType(), ticket.audience(), ticket.jti(), ticket.expiresAt());
    }

    private String randomValue() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes) + ThreadLocalRandom.current().nextInt(10);
    }
}

record IssuedTicket(String value, Instant expiresAt) {
}

record TicketClaims(String subject, String tenantId, String clientType, String audience,
                    String jti, Instant expiresAt) {
}
