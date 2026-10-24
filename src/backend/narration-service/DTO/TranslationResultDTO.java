package DTO;

public record TranslationResultDTO(
        String sourceLanguage,
        String targetLanguage,
        String title,
        String content
) {
}