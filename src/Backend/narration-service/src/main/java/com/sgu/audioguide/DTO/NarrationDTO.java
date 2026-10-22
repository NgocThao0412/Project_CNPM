package com.sgu.audioguide.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;

public record NarrationDTO(
        Long id,
        Long poiId,
        Long languageId,
        String languageCode,
        String languageName,
        String title,
        String content,
        String audioUrl,
        Boolean isVisible
) {

    @JsonProperty("visible")
    public Boolean visible() {
        return isVisible;
    }
}