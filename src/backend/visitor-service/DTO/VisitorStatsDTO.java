package DTO;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record VisitorStatsDTO(
        long activeVisitors,
        long activeListeners,
        int activeWindowSeconds,
        Instant measuredAt
) {
    public record History(
            String period,
            LocalDate from,
            LocalDate to,
            String timezone,
            long totalVisits,
            List<HistoryRow> rows
    ) {
    }

    public record HistoryRow(
            String label,
            long visits
    ) {
    }
}