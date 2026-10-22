package com.sgu.audioguide.BUS;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.sgu.audioguide.DAO.AudioDAO;
import com.sgu.audioguide.DTO.AudioDTO;
import com.sgu.audioguide.util.InMemoryAudioFile;
import com.sgu.audioguide.util.MP3;

@Service
public class AudioBUS {

    private final AudioDAO dao;
    private final Path dir;
    private final String publicBase;

    public AudioBUS(AudioDAO dao, @Value("${audio.storage.dir:./data/uploads}") String storage,
            @Value("${audio.public-base-url:http://localhost:8084}") String base) {
        this.dao = dao;
        this.dir = Path.of(storage).toAbsolutePath().normalize();
        this.publicBase = base.replaceAll("/+$", "");
    }

    @Transactional(rollbackFor = Exception.class)
    public AudioDTO upload(long narrationId, MultipartFile audio, double duration) throws IOException {
        if (narrationId <= 0 || !Double.isFinite(duration) || duration <= 0) {
            throw new IllegalArgumentException("durationSeconds phải lớn hơn 0");
        }
        String original = audio.getOriginalFilename() == null ? "" : audio.getOriginalFilename();
        if (audio.isEmpty() || audio.getSize() > 20L * 1024 * 1024 || !original.toLowerCase().endsWith(".mp3")) {
            throw new IllegalArgumentException("Chỉ nhận MP3 hợp lệ, tối đa 20 MB");
        }
        Files.createDirectories(dir);
        Path temp = Files.createTempFile(dir, "upload-", ".tmp");
        try {
            audio.transferTo(temp);
            byte[] signature = new byte[3];
            try (var input = Files.newInputStream(temp)) {
                if (input.read(signature) < 3) {
                    throw new IllegalArgumentException("MP3 rỗng hoặc không hợp lệ");

                }
            }
            boolean mp3 = (signature[0] == 'I' && signature[1] == 'D' && signature[2] == '3')
                    || ((signature[0] & 255) == 255 && ((signature[1] & 224) == 224));
            if (!mp3) {
                throw new IllegalArgumentException("Tệp không có header MP3 hợp lệ");
            }
            AudioDTO saved = dao.save(narrationId, UUID.randomUUID() + ".mp3", original, Files.size(temp), duration,
                    publicBase + "/api/audio/files/pending.mp3");
            Path savedPath = dir.resolve(saved.id() + ".mp3");
            Files.move(temp, savedPath, StandardCopyOption.REPLACE_EXISTING);
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status != TransactionSynchronization.STATUS_COMMITTED) {
                        try {
                            Files.deleteIfExists(savedPath);
                        } catch (IOException ignored) {
                        }
                    }
                }
            });
            return saved;
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    public Resource open(long id) {
        dao.active(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không có audio đang phát hành"));
        Path file = dir.resolve(id + ".mp3");
        if (!Files.isRegularFile(file)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tệp audio không tồn tại");
        }
        return new FileSystemResource(file);
    }

    public List<AudioDTO> listByContent(
            long contentId,
            boolean adminPreview
    ) {
        if (contentId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "ID nội dung phải lớn hơn 0"
            );
        }

        return dao.listByContent(contentId, adminPreview);
    }

    @Transactional(rollbackFor = Exception.class)
    public AudioDTO uploadRaw(
            long contentId,
            String filename,
            byte[] bytes
    ) throws IOException {
        if (contentId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "ID nội dung phải lớn hơn 0"
            );
        }

        if (filename == null
                || filename.isBlank()
                || filename.length() > 255
                || !filename.toLowerCase(Locale.ROOT).endsWith(".mp3")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Tên file phải có đuôi .mp3 và không quá 255 ký tự"
            );
        }

        if (bytes == null || bytes.length == 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "File audio không được rỗng"
            );
        }

        if (bytes.length > 20 * 1024 * 1024) {
            throw new ResponseStatusException(
                    HttpStatus.PAYLOAD_TOO_LARGE,
                    "File audio tối đa 20 MB"
            );
        }

        double duration = MP3.inspect(bytes).duration();

        try {
            return upload(
                    contentId,
                    new InMemoryAudioFile(filename, bytes),
                    duration
            );
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    exception.getMessage()
            );
        }
    }

    public Resource openForPreview(long id) {
        if (id <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "ID audio phải lớn hơn 0"
            );
        }

        dao.findForPreview(id).orElseThrow(()
                -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy audio"
                )
        );

        Path file = dir.resolve(id + ".mp3");

        if (!Files.isRegularFile(file)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Tệp audio không tồn tại"
            );
        }

        return new FileSystemResource(file);
    }
}
