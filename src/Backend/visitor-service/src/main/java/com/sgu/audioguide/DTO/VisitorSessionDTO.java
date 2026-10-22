package com.sgu.audioguide.DTO;

public record VisitorSessionDTO(
        String token,
        int expiresInSeconds,
        int heartbeatIntervalSeconds,
        int activeWindowSeconds
) {
}