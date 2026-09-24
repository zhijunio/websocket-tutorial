package com.example.websocket;

import java.time.Instant;

public interface TicketStore {
    void put(String value, StoredTicket ticket, long ttlSeconds);

    StoredTicket consume(String value);
}

record StoredTicket(String subject, String tenantId, String clientType, String audience,
                    String jti, Instant expiresAt) {
}
