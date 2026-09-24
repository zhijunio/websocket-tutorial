package com.example.websocket;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.FORBIDDEN;

@RestController
public class PresenceController {
    private final PresenceService presence;
    private final AppProperties properties;

    public PresenceController(PresenceService presence, AppProperties properties) {
        this.presence = presence;
        this.properties = properties;
    }

    @GetMapping("/api/presence/users")
    public Map<String, Object> users(@RequestParam(required = false) String tenantId,
                                     @RequestParam(required = false) String userId,
                                     @RequestParam(required = false) String clientType,
                                     @RequestParam(required = false) String instanceId,
                                     @RequestParam(defaultValue = "0") int offset,
                                     @RequestParam(defaultValue = "100") int limit) {
        List<PresenceRecord> records = presence.find(resolveTenant(tenantId), userId, clientType, instanceId, offset, limit);
        return Map.of("count", records.stream().map(PresenceRecord::userId).distinct().count(), "offset", Math.max(0, offset), "limit", Math.min(Math.max(1, limit), 500), "connections", records);
    }

    @GetMapping("/api/presence/users/{userId}")
    public Map<String, Object> user(@org.springframework.web.bind.annotation.PathVariable String userId) {
        List<PresenceRecord> records = presence.find(properties.getTenantId(), userId, null, null);
        return Map.of("userId", userId, "online", !records.isEmpty(), "connections", records);
    }

    @GetMapping("/api/presence/connections")
    public Map<String, Object> connections(@RequestParam(required = false) String instanceId) {
        List<PresenceRecord> records = presence.find(properties.getTenantId(), null, null, instanceId);
        return Map.of("count", records.size(), "connections", records);
    }

    private String resolveTenant(String requestedTenantId) {
        if (requestedTenantId != null && !requestedTenantId.isBlank()
                && !properties.getTenantId().equals(requestedTenantId)) {
            throw new ResponseStatusException(FORBIDDEN, "tenant access denied");
        }
        return properties.getTenantId();
    }
}
