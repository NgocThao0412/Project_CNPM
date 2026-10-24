package BUS;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import DAO.AutoNarrationDAO;
import DTO.AdminNarrationWriteDTO;
import DTO.AutoNarrationRequestDTO;
import DTO.NarrationDTO;
import DTO.TranslationRequestDTO;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Service
public class AutoNarrationBUS {

    private static final List<String> CODES = List.of("vi", "en", "zh", "ko");

    private final AutoNarrationDAO dao;
    private final NarrationBUS narrations;
    private final TranslationBUS translations;
    private final String audioBase;

    private final JsonMapper json = JsonMapper.builder().build();

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final Map<String, Job> jobs = new ConcurrentHashMap<>();

    private final ThreadPoolExecutor worker = new ThreadPoolExecutor(
            1, 1, 0L, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(2),
            runnable -> {
                Thread thread = new Thread(runnable, "auto-narration");
                thread.setDaemon(true);
                return thread;
            },
            new ThreadPoolExecutor.AbortPolicy()
    );

    public AutoNarrationBUS(
            AutoNarrationDAO dao,
            NarrationBUS narrations,
            TranslationBUS translations,
            @Value("${services.audio.url:http://127.0.0.1:8084}") String audioBase
    ) {
        this.dao = dao;
        this.narrations = narrations;
        this.translations = translations;
        this.audioBase = audioBase.replaceAll("/+$", "");
    }

    public synchronized Map<String, Object> start(
            AutoNarrationRequestDTO request,
            String adminCookie
    ) {
        validate(request);

        if (adminCookie == null || adminCookie.isBlank()) {
            throw error(HttpStatus.UNAUTHORIZED, "Thiếu phiên quản trị");
        }

        Map<String, Long> languages = dao.activeLanguages();

        if (!languages.keySet().containsAll(CODES)) {
            throw error(
                    HttpStatus.BAD_REQUEST,
                    "Cần kích hoạt đủ ngôn ngữ vi, en, zh, ko"
            );
        }

        boolean replace = Boolean.TRUE.equals(request.replaceExisting());

        var sourceId = dao.findContentId(
                request.poiId(), languages.get("vi")
        );

        if (sourceId.isPresent() && !replace) {
            var current = narrations.getAdminDetail(sourceId.get());

            if (!Objects.equals(current.title(), request.title())
                    || !Objects.equals(current.content(), request.content())) {
                throw error(
                        HttpStatus.CONFLICT,
                        "Bài tiếng Việt đã có nội dung khác. "
                                + "Chọn tạo lại để thay nội dung đã lưu."
                );
            }
        }

        long now = System.currentTimeMillis();

        jobs.entrySet().removeIf(entry ->
                !"QUEUED".equals(entry.getValue().status)
                && !"RUNNING".equals(entry.getValue().status)
                && now - entry.getValue().createdAt > 3_600_000
        );

        if (jobs.size() >= 50) {
            throw error(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Danh sách tác vụ đang đầy. Vui lòng thử lại sau."
            );
        }

        Job job = new Job();
        jobs.put(job.id, job);

        try {
            worker.execute(() -> run(job, request, languages, adminCookie));
        } catch (java.util.concurrent.RejectedExecutionException exception) {
            jobs.remove(job.id);

            throw error(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Đang có nhiều tác vụ tạo thuyết minh. Thử lại sau."
            );
        }

        return view(job);
    }

    public Map<String, Object> get(String jobId) {
        Job job = jobs.get(jobId);

        if (job == null) {
            throw error(
                    HttpStatus.NOT_FOUND,
                    "Không tìm thấy tác vụ hoặc tác vụ đã hết hạn"
            );
        }

        return view(job);
    }

    private void run(
            Job job,
            AutoNarrationRequestDTO request,
            Map<String, Long> languages,
            String cookie
    ) {
        job.status = "RUNNING";
        NarrationDTO source = null;
        int completed = 0;

        for (String code : CODES) {
            Long contentId = null;

            try {
                progress(job, code, "RUNNING", "Đang chuẩn bị nội dung", null, null);

                NarrationDTO story;

                if ("vi".equals(code)) {
                    story = save(
                            request, languages.get(code),
                            request.title(), request.content()
                    );

                    // Bài gốc có thể tiếp tục dùng để dịch dù bước audio lỗi.
                    source = story;
                } else {
                    if (source == null) {
                        throw error(
                                HttpStatus.CONFLICT,
                                "Chưa lưu được bài tiếng Việt để dịch"
                        );
                    }

                    var currentSource = narrations.getAdminDetail(source.id());

                    if (!Objects.equals(source.title(), currentSource.title())
                            || !Objects.equals(
                                    source.content(), currentSource.content()
                            )) {
                        throw error(
                                HttpStatus.CONFLICT,
                                "Bài gốc đã được sửa. Hãy tạo lại tác vụ."
                        );
                    }

                    var existing = dao.findContentId(
                            request.poiId(), languages.get(code)
                    );

                    if (existing.isPresent()
                            && !Boolean.TRUE.equals(request.replaceExisting())) {
                        story = narrations.getAdminDetail(existing.get());
                    } else {
                        progress(
                                job, code, "RUNNING",
                                "Đang dịch nội dung", null, null
                        );

                        var translated = translations.translate(
                                new TranslationRequestDTO(
                                        "vi", code,
                                        source.title(), source.content()
                                )
                        );

                        story = save(
                                request, languages.get(code),
                                translated.title(), translated.content()
                        );
                    }
                }

                contentId = story.id();

                progress(
                        job, code, "RUNNING",
                        "Đang kiểm tra audio", contentId, null
                );

                JsonNode tracks = audioRequest(
                        "GET",
                        "/api/audio?contentId=" + contentId + "&admin=1",
                        cookie
                );

                if (!tracks.isArray()) {
                    throw error(
                            HttpStatus.BAD_GATEWAY,
                            "Audio server trả danh sách không hợp lệ"
                    );
                }

                long audioId;

                if (!tracks.isEmpty()
                        && !Boolean.TRUE.equals(request.replaceExisting())) {
                    audioId = tracks.path(0).path("id").longValue();
                } else {
                    progress(
                            job, code, "RUNNING",
                            "Đang tạo giọng đọc", contentId, null
                    );

                    JsonNode audio = audioRequest(
                            "POST",
                            "/api/admin/audio/" + contentId + "/generate",
                            cookie
                    );

                    audioId = audio.path("id").longValue();
                }

                if (audioId <= 0) {
                    throw error(
                            HttpStatus.BAD_GATEWAY,
                            "Audio server chưa trả ID âm thanh hợp lệ"
                    );
                }

                progress(
                        job, code, "DONE",
                        "Đã có nội dung và audio", contentId, audioId
                );

                completed++;

            } catch (RuntimeException exception) {
                String message = exception instanceof ResponseStatusException response
                        ? response.getReason()
                        : "Không hoàn tất được ngôn ngữ này. Kiểm tra log server.";

                progress(job, code, "FAILED", message, contentId, null);
            }
        }

        job.status = completed == 4 ? "DONE"
                : completed == 0 ? "FAILED" : "PARTIAL";
    }

    private NarrationDTO save(
            AutoNarrationRequestDTO request,
            long languageId,
            String title,
            String content
    ) {
        var existing = dao.findContentId(request.poiId(), languageId);

        if (existing.isPresent()
                && !Boolean.TRUE.equals(request.replaceExisting())) {
            return narrations.getAdminDetail(existing.get());
        }

        var value = new AdminNarrationWriteDTO(
                request.poiId(),
                languageId,
                title,
                content,
                Boolean.TRUE.equals(request.visible())
        );

        return existing.isPresent()
                ? narrations.updateFromAdmin(existing.get(), value)
                : narrations.createFromAdmin(value);
    }

    private JsonNode audioRequest(
            String method,
            String path,
            String cookie
    ) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(audioBase + path))
                    .timeout(Duration.ofSeconds(180))
                    .header("Cookie", cookie)
                    .header("Accept", "application/json")
                    .method(method, HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response = http.send(
                    request, HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw error(
                        HttpStatus.BAD_GATEWAY,
                        "Audio server trả lỗi HTTP " + response.statusCode()
                );
            }

            return json.readTree(response.body());

        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw error(HttpStatus.SERVICE_UNAVAILABLE, "Tác vụ bị gián đoạn");
        } catch (IOException exception) {
            throw error(
                    HttpStatus.BAD_GATEWAY,
                    "Không kết nối được audio server cổng 8084"
            );
        } catch (tools.jackson.core.JacksonException exception) {
            throw error(
                    HttpStatus.BAD_GATEWAY,
                    "Audio server trả dữ liệu sai định dạng"
            );
        }
    }

    private void validate(AutoNarrationRequestDTO request) {
        if (request == null || request.poiId() == null
                || request.poiId() <= 0
                || request.title() == null || request.title().isBlank()
                || request.title().length() > 200
                || request.content() == null || request.content().isBlank()
                || request.content().length() > 6000) {
            throw error(
                    HttpStatus.BAD_REQUEST,
                    "Cần chọn POI, tiêu đề tối đa 200 ký tự "
                            + "và nội dung tiếng Việt tối đa 6000 ký tự"
            );
        }

        if (!dao.poiExists(request.poiId())) {
            throw error(HttpStatus.NOT_FOUND, "Không tìm thấy POI");
        }
    }

    private void progress(
            Job job, String code, String status,
            String message, Long contentId, Long audioId
    ) {
        job.items.put(
                code, new Item(code, status, message, contentId, audioId)
        );
    }

    private Map<String, Object> view(Job job) {
        return Map.of(
                "jobId", job.id,
                "status", job.status,
                "languages", CODES.stream().map(job.items::get).toList()
        );
    }

    private static ResponseStatusException error(
            HttpStatus status, String message
    ) {
        return new ResponseStatusException(status, message);
    }

    @PreDestroy
    public void shutdown() {
        worker.shutdownNow();
    }

    public record Item(
            String languageCode,
            String status,
            String message,
            Long contentId,
            Long audioId
    ) {
    }

    private static final class Job {
        final String id = UUID.randomUUID().toString();
        final long createdAt = System.currentTimeMillis();
        final Map<String, Item> items = new ConcurrentHashMap<>();
        volatile String status = "QUEUED";

        Job() {
            for (String code : CODES) {
                items.put(code, new Item(code, "QUEUED", "Đang chờ", null, null));
            }
        }
    }
}