package com.sgu.audioguide.DTO;

import java.time.Instant;

public record VisitorStatsDTO(
        long activeVisitors,
        long activeListeners,
        int activeWindowSeconds,
        Instant measuredAt
) {
}