package com.example.websocket;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class NotificationRateLimiter {
    private final StringRedisTemplate redis;
    private final AppProperties properties;

    public NotificationRateLimiter(StringRedisTemplate redis, AppProperties properties) {
        this.redis = redis;
        this.properties = properties;
    }

    public boolean allow(String tenantId, String userId) {
        long bucket = System.currentTimeMillis() / Duration.ofMinutes(1).toMillis();
        String key = "ws:notification-rate:" + tenantId + ":" + userId + ":" + bucket;
        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1) redis.expire(key, Duration.ofMinutes(2));
        return count != null && count <= properties.getMaxNotificationRate();
    }
}
