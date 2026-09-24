package com.example.websocket;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class InMemoryTicketStore implements TicketStore {
    private final Map<String, StoredTicket> values = new ConcurrentHashMap<>();

    @Override
    public void put(String value, StoredTicket ticket, long ttlSeconds) {
        values.put(value, ticket);
    }

    @Override
    public StoredTicket consume(String value) {
        StoredTicket ticket = values.remove(value);
        if (ticket == null || ticket.expiresAt().isBefore(Instant.now())) {
            return null;
        }
        return ticket;
    }
}
