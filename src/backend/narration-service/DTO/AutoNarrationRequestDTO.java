package DTO;

public record AutoNarrationRequestDTO(
        Long poiId,
        String title,
        String content,
        Boolean visible,
        Boolean replaceExisting
) {
}