package DTO;

import java.time.LocalDate;
import java.util.List;

public record ListeningStatsDTO(
        LocalDate from,
        LocalDate to,
        String timezone,
        long totalListens,
        long uniqueSessions,
        List<Row> rows
) {

    public record Row(
            LocalDate date,
            long poiId,
            String poiName,
            String languageCode,
            String languageName,
            long listens,
            long uniqueSessions
    ) {
    }
}