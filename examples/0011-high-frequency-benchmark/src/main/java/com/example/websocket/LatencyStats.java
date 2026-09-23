package com.example.websocket;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class LatencyStats {
    private LatencyStats() {
    }

    public static long percentileMillis(List<Long> nanos, double percentile) {
        if (nanos.isEmpty()) {
            return 0;
        }
        List<Long> sorted = new ArrayList<>(nanos);
        Collections.sort(sorted);
        int index = (int) Math.ceil(percentile * sorted.size()) - 1;
        index = Math.max(0, Math.min(index, sorted.size() - 1));
        return sorted.get(index) / 1_000_000;
    }
}
