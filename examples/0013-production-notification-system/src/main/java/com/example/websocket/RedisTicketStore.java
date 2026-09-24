package com.example.websocket;

import java.time.Instant;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Component
public class RedisTicketStore implements TicketStore {
    private static final String PREFIX = "ws:ticket:";
    private static final DefaultRedisScript<String> GET_AND_DELETE = new DefaultRedisScript<>(
            "local value = redis.call('GET', KEYS[1]); "
                    + "if value then redis.call('DEL', KEYS[1]); end; return value;", String.class);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public RedisTicketStore(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    @Override
    public void put(String value, StoredTicket ticket, long ttlSeconds) {
        try {
            redis.opsForValue().set(PREFIX + value, objectMapper.writeValueAsString(ticket), java.time.Duration.ofSeconds(ttlSeconds));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("ticket could not be serialized", exception);
        }
    }

    @Override
    public StoredTicket consume(String value) {
        String json = redis.execute(GET_AND_DELETE, java.util.List.of(PREFIX + value));
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, StoredTicket.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("ticket could not be deserialized", exception);
        }
    }
}
