package com.example.websocket;

import java.util.Optional;

public final class TopicMapper {
    private TopicMapper() {
    }

    public static String eventTopic(String deviceId) {
        return "devices/" + requireDeviceId(deviceId) + "/events";
    }

    public static String commandTopic(String deviceId) {
        return "devices/" + requireDeviceId(deviceId) + "/commands";
    }

    public static Optional<String> deviceIdFromEventTopic(String topic) {
        String[] parts = topic == null ? new String[0] : topic.split("/", -1);
        if (parts.length == 3 && parts[0].equals("devices") && parts[2].equals("events")
                && !parts[1].isBlank()) {
            return Optional.of(parts[1]);
        }
        return Optional.empty();
    }

    private static String requireDeviceId(String deviceId) {
        if (deviceId == null || !deviceId.matches("[A-Za-z0-9_-]{1,32}")) {
            throw new IllegalArgumentException("deviceId must match [A-Za-z0-9_-]{1,32}");
        }
        return deviceId;
    }
}
