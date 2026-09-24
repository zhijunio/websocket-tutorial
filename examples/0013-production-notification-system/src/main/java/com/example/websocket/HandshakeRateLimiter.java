package com.example.websocket;

import java.time.Duration;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class HandshakeRateLimiter {
    private static final String PREFIX = "ws:handshake-rate:";
    private final StringRedisTemplate redis;
    private final AppProperties properties;

    public HandshakeRateLimiter(StringRedisTemplate redis, AppProperties properties) {
        this.redis = redis;
        this.properties = properties;
    }

    public boolean allow(HttpServletRequest request) {
        // Spring's framework forward-header strategy has already replaced the
        // remote address from the proxy's trusted forwarding headers. Nginx is
        // the only exposed entry point in the Compose topology.
        String address = request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
        long bucket = System.currentTimeMillis() / Duration.ofMinutes(1).toMillis();
        String key = PREFIX + address + ":" + bucket;
        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1) {
            redis.expire(key, Duration.ofMinutes(2));
        }
        return count != null && count <= properties.getMaxHandshakesPerMinute();
    }
}
