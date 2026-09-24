package com.example.websocket;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class PresenceService {
    private static final String PREFIX = "ws:presence:";
    private static final String INDEX_PREFIX = "ws:presence:index:";
    private final StringRedisTemplate redis;
    private final AppProperties properties;

    public PresenceService(StringRedisTemplate redis, AppProperties properties) {
        this.redis = redis;
        this.properties = properties;
    }

    public void connect(String tenantId, String userId, String clientType, String connectionId) {
        String key = key(tenantId, userId, connectionId);
        redis.opsForHash().putAll(key, Map.of("tenantId", tenantId, "userId", userId, "clientType", clientType,
                "connectionId", connectionId, "instanceId", properties.getInstanceId(),
                "connectedAt", Instant.now().toString(), "lastSeenAt", Instant.now().toString()));
        redis.expire(key, properties.getPresenceTtl());
        redis.opsForSet().add(indexKey(tenantId), key);
    }

    public void heartbeat(String tenantId, String userId, String connectionId) {
        String key = key(tenantId, userId, connectionId);
        if (Boolean.TRUE.equals(redis.hasKey(key))) {
            redis.opsForHash().put(key, "lastSeenAt", Instant.now().toString());
            redis.expire(key, properties.getPresenceTtl());
        }
    }

    public void disconnect(String tenantId, String userId, String connectionId) {
        String key = key(tenantId, userId, connectionId);
        redis.delete(key);
        redis.opsForSet().remove(indexKey(tenantId), key);
    }

    public List<PresenceRecord> find(String tenantId, String userId, String clientType, String instanceId) {
        return find(tenantId, userId, clientType, instanceId, 0, 500);
    }

    public List<PresenceRecord> find(String tenantId, String userId, String clientType, String instanceId,
                                     int offset, int limit) {
        List<PresenceRecord> result = new ArrayList<>();
        String index = indexKey(tenantId);
        ScanOptions options = ScanOptions.scanOptions().count(100).build();
        try (Cursor<String> cursor = redis.opsForSet().scan(index, options)) {
            int skipped = 0;
            while (cursor.hasNext()) {
                String key = cursor.next();
                Map<Object, Object> values = redis.opsForHash().entries(key);
                if (values.isEmpty()) {
                    redis.opsForSet().remove(index, key);
                    continue;
                }
                if (!matches(values, tenantId, userId, clientType, instanceId)) {
                    continue;
                }
                if (skipped++ < Math.max(offset, 0)) {
                    continue;
                }
                result.add(new PresenceRecord(string(values, "tenantId"), string(values, "userId"), string(values, "clientType"),
                        string(values, "connectionId"), string(values, "instanceId"), string(values, "connectedAt"), string(values, "lastSeenAt")));
                if (result.size() >= Math.max(1, Math.min(limit, 500))) {
                    break;
                }
            }
        } catch (Exception exception) {
            throw new IllegalStateException("presence scan failed", exception);
        }
        return result;
    }

    private boolean matches(Map<Object, Object> values, String tenantId, String userId, String clientType, String instanceId) {
        return blankOrEqual(tenantId, values, "tenantId") && blankOrEqual(userId, values, "userId")
                && blankOrEqual(clientType, values, "clientType") && blankOrEqual(instanceId, values, "instanceId");
    }

    private boolean blankOrEqual(String expected, Map<Object, Object> values, String field) {
        return expected == null || expected.isBlank() || expected.equals(string(values, field));
    }

    private String string(Map<Object, Object> values, String key) {
        Object value = values.get(key);
        return value == null ? "" : value.toString();
    }

    private String key(String tenantId, String userId, String connectionId) {
        return PREFIX + tenantId + ":" + userId + ":" + connectionId;
    }

    private String indexKey(String tenantId) {
        return INDEX_PREFIX + tenantId;
    }
}

record PresenceRecord(String tenantId, String userId, String clientType, String connectionId,
                      String instanceId, String connectedAt, String lastSeenAt) {
}
