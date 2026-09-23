package com.example.websocket;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class LatencyStatsTests {
    @Test
    void calculatesNearestRankPercentiles() {
        List<Long> nanos = List.of(1_000_000L, 2_000_000L, 3_000_000L, 4_000_000L);

        assertEquals(2, LatencyStats.percentileMillis(nanos, 0.50));
        assertEquals(4, LatencyStats.percentileMillis(nanos, 0.95));
    }
}
