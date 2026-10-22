package com.sgu.audioguide.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AudioDTO(
        Long id,
        Long contentId,
        String fileName,
        String fileUrl,
        Long fileSizeBytes,
        Double durationSeconds,
        int versionNumber,
        Boolean isVisible
) {

    @JsonProperty("audioUrl")
    public String audioUrl() {
        return fileUrl;
    }

    @JsonProperty("visible")
    public Boolean visible() {
        return isVisible;
    }
}