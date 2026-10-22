package com.sgu.audioguide.DTO;

public record AdminNarrationWriteDTO(
        Long poiId,
        Long languageId,
        String title,
        String content,
        Boolean visible
) {
}