package BUS;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Service
public class GeminiSpeechBUS {

    private static final Map<String,String> LANGUAGES = Map.of(
            "vi", "Vietnamese",
            "en", "English",
            "zh", "Mandarin Chinese",
            "ko", "Korean"
    );

    private final String apiKey;
    private final String model;
    private final Mp3EncoderBUS encoder;

    private final JsonMapper json = JsonMapper.builder().build();

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public GeminiSpeechBUS(
            Mp3EncoderBUS encoder,
            @Value("${GEMINI_API_KEY:}") String apiKey,
            @Value("${speech.model:gemini-3.8-flash-lite-tts}") String model
    ) {
        this.encoder = encoder;
        this.apiKey = apiKey;
        this.model = model;
    }

    public byte[] generateMp3(String text, String languageCode) {
        if (text == null || text.isBlank() || text.length() > 6000) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Nội dung tạo giọng đọc cần từ 1 đến 6000 ký tự"
            );
        }

        if (languageCode == null || !LANGUAGES.containsKey(languageCode)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Giọng đọc chỉ hỗ trợ vi, en, zh hoặc ko"
            );
        }

        if (apiKey.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Audio server chưa có GEMINI_API_KEY"
            );
        }

        Map<String,Object> payload = Map.of(
                "model", model,
                "input", List.of(Map.of(
                        "type", "user_input",
                        "content", List.of(Map.of(
                                "type", "text",
                                "text", text,
                                "annotations", List.of(Map.of(
                                        "type", "speech_metadata",
                                        "style",
                                        "Read clearly in "
                                                + LANGUAGES.get(languageCode)
                                                + " with a warm audio-guide tone."
                                ))
                        ))
                )),
                "response_format", Map.of(
                        "type", "audio",
                        "mime_type", "audio/wav"
                ),
                "generation_config", Map.of(
                        "speech_config", List.of(Map.of("voice", "Kore"))
                )
        );

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(
                            "https://generativelanguage.googleapis.com/v1beta/interactions"
                    ))
                    .timeout(Duration.ofSeconds(120))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .header("x-goog-api-key", apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(
                            json.writeValueAsString(payload)
                    ))
                    .build();

            HttpResponse<String> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );
            if (response.statusCode() != 200 && response.statusCode() != 201) {
    System.err.println("Gemini TTS HTTP: " + response.statusCode());
    System.err.println(response.body().replace(apiKey, "[REDACTED]"));
}

            if (response.statusCode() == 429) {
                throw new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "Dịch vụ giọng đọc đang giới hạn lượt gọi. Thử lại sau."
                );
            }

            if (response.statusCode() != 200
                    && response.statusCode() != 201) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Dịch vụ giọng đọc trả lỗi HTTP "
                                + response.statusCode()
                );
            }

            JsonNode root = json.readTree(response.body());
            String encodedAudio = null;

            for (JsonNode step : root.path("steps")) {
                if (!"model_output".equals(step.path("type").stringValue())) {
                    continue;
                }

                for (JsonNode part : step.path("content")) {
                    if ("audio".equals(part.path("type").stringValue())) {
                        String data = part.path("data").stringValue();

                        if (data != null && !data.isBlank()) {
                            encodedAudio = data;
                        }
                    }
                }
            }

            if (encodedAudio == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Dịch vụ chưa trả âm thanh hoàn chỉnh"
                );
            }

            if (encodedAudio.length() > 42_000_000) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Âm thanh được tạo vượt giới hạn xử lý"
                );
            }

            byte[] wav;

            try {
                wav = Base64.getDecoder().decode(encodedAudio);
            } catch (IllegalArgumentException exception) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Dữ liệu âm thanh trả về không hợp lệ"
                );
            }

            return encoder.encode(wav);

        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Yêu cầu tạo giọng đọc bị gián đoạn"
            );
        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Không tạo hoặc chuyển đổi được âm thanh",
                    exception
            );
        } catch (JacksonException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Dịch vụ giọng đọc trả dữ liệu sai định dạng"
            );
        }
    }
}