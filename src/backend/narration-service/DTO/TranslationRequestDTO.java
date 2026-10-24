package DTO;

public record TranslationRequestDTO(
        String sourceLanguage,
        String targetLanguage,
        String title,
        String content
) {
}