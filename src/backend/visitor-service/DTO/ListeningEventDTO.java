package DTO;

public record ListeningEventDTO(
        String eventId,
        Long audioFileId,
        String triggerSource
) {
}