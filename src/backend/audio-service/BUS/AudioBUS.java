package BUS;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
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

import DAO.AudioDAO;
import DTO.AudioDTO;

@Service
public class AudioBUS {

    private static final long MAX_AUDIO_SIZE = 20L * 1024 * 1024;

    private final AudioDAO dao;
    private final Path dir;
    private final String publicBase;

    public AudioBUS(
            AudioDAO dao,
            @Value("${audio.storage.dir:./data/uploads}") String storage,
            @Value("${audio.public-base-url:http://localhost:8084}") String base
    ) {
        this.dao = dao;
        this.dir = Path.of(storage).toAbsolutePath().normalize();
        this.publicBase = base.replaceAll("/+$", "");
    }

    @Transactional(rollbackFor = Exception.class)
    public AudioDTO upload(
            long narrationId,
            MultipartFile audio,
            double duration
    ) throws IOException {
        if (narrationId <= 0) {
            throw badRequest("ID nội dung phải lớn hơn 0");
        }

        if (!Double.isFinite(duration) || duration <= 0) {
            throw badRequest("durationSeconds phải lớn hơn 0");
        }

        if (audio == null || audio.isEmpty()) {
            throw badRequest("File audio không được rỗng");
        }

        if (audio.getSize() > MAX_AUDIO_SIZE) {
            throw new ResponseStatusException(
                    HttpStatus.PAYLOAD_TOO_LARGE,
                    "File audio tối đa 20 MB"
            );
        }

        String original = audio.getOriginalFilename();

        if (original == null
                || original.isBlank()
                || original.length() > 255
                || !original.toLowerCase(Locale.ROOT).endsWith(".mp3")) {
            throw badRequest(
                    "Tên file phải có đuôi .mp3 và không quá 255 ký tự"
            );
        }

        Files.createDirectories(dir);
        Path temp = Files.createTempFile(dir, "upload-", ".tmp");

        try {
            audio.transferTo(temp);

            byte[] signature;

            try (InputStream input = Files.newInputStream(temp)) {
                signature = input.readNBytes(3);
            }

            if (signature.length < 3) {
                throw badRequest("MP3 rỗng hoặc không hợp lệ");
            }

            boolean mp3 = (
                    signature[0] == 'I'
                    && signature[1] == 'D'
                    && signature[2] == '3'
            ) || (
                    (signature[0] & 255) == 255
                    && (signature[1] & 224) == 224
            );

            if (!mp3) {
                throw badRequest("Tệp không có header MP3 hợp lệ");
            }

            AudioDTO saved = dao.save(
                    narrationId,
                    UUID.randomUUID() + ".mp3",
                    original,
                    Files.size(temp),
                    duration,
                    publicBase + "/api/audio/files/pending.mp3"
            );

            Path savedPath = dir.resolve(saved.id() + ".mp3");

            // Mỗi phiên bản có ID riêng, không ghi đè file cũ.
            Files.move(temp, savedPath);

            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCompletion(int status) {
                            if (status != STATUS_COMMITTED) {
                                try {
                                    Files.deleteIfExists(savedPath);
                                } catch (IOException ignored) {
                                    // Việc dọn file không làm thay đổi kết quả transaction.
                                }
                            }
                        }
                    }
            );

            return saved;
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    public List<AudioDTO> listByContent(
            long contentId,
            boolean adminPreview
    ) { 
        if (contentId <= 0) {
            throw badRequest("ID nội dung phải lớn hơn 0");
        }

        return dao.listByContent(contentId, adminPreview);
    }

    public Resource open(long id) {
        if (id <= 0) {
            throw badRequest("ID audio phải lớn hơn 0");
        }

        dao.active(id).orElseThrow(
                () -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không có audio đang phát hành"
                )
        );

        return openFile(id);
    }

    public Resource openForPreview(long id) {
        if (id <= 0) {
            throw badRequest("ID audio phải lớn hơn 0");
        }

        dao.findForPreview(id).orElseThrow(
                () -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy audio"
                )
        );

        return openFile(id);
    }

    private Resource openFile(long id) {
        Path file = dir.resolve(id + ".mp3");

        if (!Files.isRegularFile(file)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Tệp audio không tồn tại"
            );
        }

        return new FileSystemResource(file);
    }

    @Transactional(rollbackFor = Exception.class)
    public AudioDTO uploadRaw(
            long contentId,
            String filename,
            byte[] bytes
    ) throws IOException {
        if (contentId <= 0) {
            throw badRequest("ID nội dung phải lớn hơn 0");
        }

        if (filename == null
                || filename.isBlank()
                || filename.length() > 255
                || !filename.toLowerCase(Locale.ROOT).endsWith(".mp3")) {
            throw badRequest(
                    "Tên file phải có đuôi .mp3 và không quá 255 ký tự"
            );
        }

        if (bytes == null || bytes.length == 0) {
            throw badRequest("File audio không được rỗng");
        }

        if (bytes.length > MAX_AUDIO_SIZE) {
            throw new ResponseStatusException(
                    HttpStatus.PAYLOAD_TOO_LARGE,
                    "File audio tối đa 20 MB"
            );
        }

        try {
            double duration = inspectMp3Duration(bytes);

            return upload(
                    contentId,
                    new ByteArrayMultipartFile(filename, bytes),
                    duration
            );
        } catch (IllegalArgumentException exception) {
            throw badRequest(exception.getMessage());
        }
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                message
        );
    }

    private static double inspectMp3Duration(byte[] bytes) {
        int offset = 0;

        if (bytes.length >= 10
                && bytes[0] == 'I'
                && bytes[1] == 'D'
                && bytes[2] == '3') {
            for (int i = 6; i < 10; i++) {
                if ((bytes[i] & 128) != 0) {
                    throw new IllegalArgumentException(
                            "Thông tin ID3 của MP3 không hợp lệ"
                    );
                }
            }

            int tagSize = ((bytes[6] & 127) << 21)
                    | ((bytes[7] & 127) << 14)
                    | ((bytes[8] & 127) << 7)
                    | (bytes[9] & 127);

            offset = 10 + tagSize;

            // ID3v2.4 có thể có footer dài 10 byte.
            if (bytes[3] == 4 && (bytes[5] & 16) != 0) {
                offset += 10;
            }

            if (offset > bytes.length) {
                throw new IllegalArgumentException(
                        "Thông tin ID3 vượt quá kích thước file"
                );
            }
        }

        double duration = 0;
        int frames = 0;
        int skipped = 0;

        while (offset + 4 <= bytes.length) {
            int header = ((bytes[offset] & 255) << 24)
                    | ((bytes[offset + 1] & 255) << 16)
                    | ((bytes[offset + 2] & 255) << 8)
                    | (bytes[offset + 3] & 255);

            int version = (header >>> 19) & 3;
            int layer = (header >>> 17) & 3;
            int bitrateIndex = (header >>> 12) & 15;
            int sampleRateIndex = (header >>> 10) & 3;
            int padding = (header >>> 9) & 1;

            boolean validHeader = (header & 0xffe00000) == 0xffe00000
                    && version != 1
                    && layer == 1
                    && bitrateIndex > 0
                    && bitrateIndex < 15
                    && sampleRateIndex != 3;

            if (!validHeader) {
                if (frames > 0 || ++skipped > 4096) {
                    break;
                }

                offset++;
                continue;
            }

            int[] bitrates = version == 3
                    ? new int[]{
                        0, 32, 40, 48, 56, 64, 80, 96,
                        112, 128, 160, 192, 224, 256, 320
                    }
                    : new int[]{
                        0, 8, 16, 24, 32, 40, 48, 56,
                        64, 80, 96, 112, 128, 144, 160
                    };

            int[] sampleRates = {44100, 48000, 32000};
            int sampleRate = sampleRates[sampleRateIndex];

            if (version == 2) {
                sampleRate /= 2;
            } else if (version == 0) {
                sampleRate /= 4;
            }

            int frameLength = (version == 3 ? 144 : 72)
                    * bitrates[bitrateIndex]
                    * 1000
                    / sampleRate
                    + padding;

            if (frameLength < 4 || offset + frameLength > bytes.length) {
                break;
            }

            duration += (version == 3 ? 1152.0 : 576.0) / sampleRate;
            frames++;
            offset += frameLength;
        }

        if (frames < 3 || duration < 0.05) {
            throw new IllegalArgumentException("Tệp MP3 không hợp lệ");
        }

        return duration;
    }

    private static final class ByteArrayMultipartFile
            implements MultipartFile {

        private final String filename;
        private final byte[] bytes;

        private ByteArrayMultipartFile(String filename, byte[] bytes) {
            this.filename = filename;
            this.bytes = bytes;
        }

        @Override
        public String getName() {
            return "audio";
        }

        @Override
        public String getOriginalFilename() {
            return filename;
        }

        @Override
        public String getContentType() {
            return "audio/mpeg";
        }

        @Override
        public boolean isEmpty() {
            return bytes.length == 0;
        }

        @Override
        public long getSize() {
            return bytes.length;
        }

        @Override
        public byte[] getBytes() {
            return bytes.clone();
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(bytes);
        }

        @Override
        public void transferTo(File destination) throws IOException {
            Files.write(destination.toPath(), bytes);
        }

        @Override
        public void transferTo(Path destination) throws IOException {
            Files.write(destination, bytes);
        }
    }
}