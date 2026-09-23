package com.example.websocket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TopicMapperTests {
    @Test
    void mapsDeviceTopicsInBothDirections() {
        assertEquals("devices/sensor-1/events", TopicMapper.eventTopic("sensor-1"));
        assertEquals("devices/sensor-1/commands", TopicMapper.commandTopic("sensor-1"));
        assertEquals("sensor-1", TopicMapper.deviceIdFromEventTopic("devices/sensor-1/events").orElseThrow());
    }

    @Test
    void rejectsTopicsOutsideTheEventShape() {
        assertTrue(TopicMapper.deviceIdFromEventTopic("devices/sensor-1/commands").isEmpty());
        assertTrue(TopicMapper.deviceIdFromEventTopic("devices//events").isEmpty());
    }
}
