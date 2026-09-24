package com.example.websocket;

import java.util.List;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

/**
 * Redis-backed connection leases. Admission and all quota checks happen in one
 * Lua invocation, so concurrent application instances cannot oversubscribe a
 * user, tenant, IP, or instance.
 */
@Service
public class ConnectionAdmissionService {
    private static final String PREFIX = "ws:connections:";
    private static final DefaultRedisScript<Long> ACQUIRE = new DefaultRedisScript<>("""
            local now = tonumber(ARGV[2])
            local member = ARGV[1]
            for i, key in ipairs(KEYS) do
              redis.call('ZREMRANGEBYSCORE', key, '-inf', now)
              if redis.call('ZCARD', key) >= tonumber(ARGV[i + 3]) then return 0 end
            end
            local expiry = tonumber(ARGV[3])
            for _, key in ipairs(KEYS) do redis.call('ZADD', key, expiry, member) end
            return 1
            """, Long.class);
    private static final DefaultRedisScript<Long> RELEASE = new DefaultRedisScript<>("""
            for _, key in ipairs(KEYS) do redis.call('ZREM', key, ARGV[1]) end
            return 1
            """, Long.class);
    private static final DefaultRedisScript<Long> REFRESH = new DefaultRedisScript<>("""
            local expiry = tonumber(ARGV[2])
            for _, key in ipairs(KEYS) do
              if redis.call('ZSCORE', key, ARGV[1]) then redis.call('ZADD', key, expiry, ARGV[1]) end
            end
            return 1
            """, Long.class);

    private final StringRedisTemplate redis;
    private final AppProperties properties;

    public ConnectionAdmissionService(StringRedisTemplate redis, AppProperties properties) {
        this.redis = redis;
        this.properties = properties;
    }

    public boolean tryAcquire(String tenantId, String userId, String clientIp, String connectionId) {
        long now = System.currentTimeMillis();
        long expiry = now + properties.getPresenceTtl().toMillis();
        List<String> keys = keys(tenantId, userId, clientIp);
        Long result = redis.execute(ACQUIRE, keys, connectionId, Long.toString(now), Long.toString(expiry),
                Integer.toString(properties.getMaxConnectionsPerUser()),
                Integer.toString(properties.getMaxConnectionsPerTenant()),
                Integer.toString(properties.getMaxConnectionsPerInstance()),
                Integer.toString(properties.getMaxConnectionsPerIp()));
        return Long.valueOf(1L).equals(result);
    }

    public void release(String tenantId, String userId, String clientIp, String connectionId) {
        redis.execute(RELEASE, keys(tenantId, userId, clientIp), connectionId);
    }

    public void refresh(String tenantId, String userId, String clientIp, String connectionId) {
        redis.execute(REFRESH, keys(tenantId, userId, clientIp), connectionId,
                Long.toString(System.currentTimeMillis() + properties.getPresenceTtl().toMillis()));
    }

    private List<String> keys(String tenantId, String userId, String clientIp) {
        String safeIp = clientIp == null || clientIp.isBlank() ? "unknown" : clientIp;
        return List.of(PREFIX + "user:" + tenantId + ":" + userId,
                PREFIX + "tenant:" + tenantId,
                PREFIX + "instance:" + properties.getInstanceId(),
                PREFIX + "ip:" + safeIp);
    }
}
