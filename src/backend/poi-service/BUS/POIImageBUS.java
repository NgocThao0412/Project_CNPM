package BUS;

import DAO.POIImageDAO;
import DTO.POIImageDTO;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class POIImageBUS {
    public static final int MAX_BYTES = 5 * 1024 * 1024;
    private static final int MAX_IMAGES = 10;
    private final POIImageDAO dao;
    private final Path root;
    private final Path legacyRoot;

    public POIImageBUS(POIImageDAO dao,
            @Value("${poi.images.directory:./data/poi-images}") String directory,
            @Value("${poi.images.legacy-directory:./data/AnhPOI}") String legacyDirectory) {
        this.dao = dao;
        this.root = Path.of(directory).toAbsolutePath().normalize();
        this.legacyRoot = Path.of(legacyDirectory).toAbsolutePath().normalize();
    }

    private void positive(long id) {
        if (id <= 0)
            throw new IllegalArgumentException("ID không hợp lệ");
    }

    public List<POIImageDTO> list(long poiId) {
        positive(poiId);
        if (!dao.lockPoiReadOnly(poiId))
            throw new NoSuchElementException("POI không tồn tại");
        return dao.list(poiId);
    }

    /**
     * File type is checked from the bytes, not only the filename or browser MIME.
     */
    private String extension(byte[] bytes, String mime) {
        if (bytes == null || bytes.length == 0 || bytes.length > MAX_BYTES)
            throw new IllegalArgumentException("Ảnh phải có dung lượng từ 1 byte đến 5 MB");
        String found = null;
        if (bytes.length >= 8 && (bytes[0] & 255) == 0x89 && bytes[1] == 0x50
                && bytes[2] == 0x4e && bytes[3] == 0x47 && bytes[4] == 0x0d
                && bytes[5] == 0x0a && bytes[6] == 0x1a && bytes[7] == 0x0a)
            found = "png";
        else if (bytes.length >= 4 && (bytes[0] & 255) == 0xff && (bytes[1] & 255) == 0xd8
                && (bytes[2] & 255) == 0xff)
            found = "jpg";
        else if (bytes.length >= 12 && bytes[0] == 'R' && bytes[1] == 'I'
                && bytes[2] == 'F' && bytes[3] == 'F' && bytes[8] == 'W'
                && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P')
            found = "webp";
        if (found == null)
            throw new IllegalArgumentException("File không phải ảnh JPG, PNG hoặc WebP hợp lệ");
        String expected = switch (found) {
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            default -> "image/jpeg";
        };
        if (!expected.equalsIgnoreCase(mime))
            throw new IllegalArgumentException("Định dạng ảnh và loại file không trùng khớp");
        return found;
    }

    @Transactional
    public POIImageDTO upload(long poiId, String filename, String mime, byte[] data) throws IOException {
        positive(poiId);
        String ext = extension(data, mime);
        if (!dao.lockPoi(poiId))
            throw new NoSuchElementException("POI không tồn tại");
        if (dao.count(poiId) >= MAX_IMAGES)
            throw new IllegalArgumentException("Mỗi địa điểm được lưu tối đa 10 ảnh");
        int position = dao.nextPosition(poiId);
        Files.createDirectories(root);

        String baseName = filename == null ? "" : filename.trim();
        baseName = baseName.replace('\\', '/');
        baseName = baseName.substring(baseName.lastIndexOf('/') + 1);
        baseName = baseName.replaceFirst("(?i)\\.(jpg|jpeg|png|webp)$", "");
        baseName = java.text.Normalizer.normalize(
                baseName, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        baseName = baseName.replaceAll("[^a-zA-Z0-9_-]", "_")
                .replaceAll("_+", "_");

        if (baseName.isBlank() || baseName.equalsIgnoreCase("default-poi")) {
            baseName = "POI_" + poiId;
        }

        String name = baseName + "." + ext;
        Path file = root.resolve(name);

        int suffix = 2;
        while (Files.exists(file)) {
            name = baseName + "_" + suffix++ + "." + ext;
            file = root.resolve(name);
        }
        Files.write(file, data, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        String url = "/poi-image-files/" + poiId + "/" + name;
        try {
            long id = dao.insert(poiId, url, position);
            return new POIImageDTO(id, poiId, url, null, position);
        } catch (RuntimeException ex) {
            Files.deleteIfExists(file);
            throw ex;
        }
    }

    @Transactional
    public void delete(long poiId, long imageId) {
        positive(poiId);
        positive(imageId);
        if (!dao.lockPoi(poiId))
            throw new NoSuchElementException("POI không tồn tại");
        POIImageDTO image = dao.find(poiId, imageId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy ảnh"));
        if (dao.delete(poiId, imageId) == 0)
            throw new NoSuchElementException("Không tìm thấy ảnh");
        Path old = managedFile(poiId, image.fileUrl());
        if (old != null) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        Files.deleteIfExists(old);
                    } catch (IOException ex) {
                        System.err.println("Không xóa được ảnh cũ: " + ex.getMessage());
                    }
                }
            });
        }
    }

    /**
     * Reject non-generated paths so database values cannot choose arbitrary disk
     * files.
     */
    private Path managedFile(long poiId, String url) {
        String prefix = "/poi-image-files/" + poiId + "/";

        if (url == null || !url.startsWith(prefix)) {
            return null;
        }

        String name = url.substring(prefix.length());

        if (!name.matches("[A-Za-z0-9_-]+\\.(?i:jpg|png|webp)")) {
            return null;
        }

        if (name.equalsIgnoreCase("default-poi.png")) {
            return null;
        }

        // Cau truc moi: anh nam truc tiep trong poi-images
        Path file = root.resolve(name).normalize();

        if (file.startsWith(root) && Files.isRegularFile(file)) {
            return file;
        }

        // Ho tro anh UUID theo cau truc cu trong luc chuyen doi
        Path old = legacyRoot.resolve(String.valueOf(poiId))
                .resolve(name).normalize();

        if (old.startsWith(legacyRoot) && Files.isRegularFile(old)) {
            return old;
        }

        // Ho tro cac thu muc ID con ton tai trong poi-images
        Path oldInRoot = root.resolve(String.valueOf(poiId))
                .resolve(name).normalize();

        if (oldInRoot.startsWith(root) && Files.isRegularFile(oldInRoot)) {
            return oldInRoot;
        }

        return null;
    }

    public record ImageData(String mime, byte[] bytes) {
    }

    private ImageData read(Path file) throws IOException {
        if (file == null || !Files.isRegularFile(file) || Files.size(file) > MAX_BYTES)
            throw new NoSuchElementException("Không tìm thấy file ảnh");
        String name = file.getFileName().toString();
        String mime = name.endsWith(".png") ? "image/png" : name.endsWith(".webp") ? "image/webp" : "image/jpeg";
        return new ImageData(mime, Files.readAllBytes(file));
    }

    public ImageData readPublic(long poiId, String filename) throws IOException {
        positive(poiId);
        String url = "/poi-image-files/" + poiId + "/" + filename;
        if (!dao.isPublic(poiId, url))
            throw new NoSuchElementException("Ảnh không được công khai");
        return read(managedFile(poiId, url));
    }

    public ImageData readAdmin(long poiId, long imageId) throws IOException {
        positive(poiId);
        positive(imageId);
        POIImageDTO image = dao.find(poiId, imageId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy ảnh"));
        return read(managedFile(poiId, image.fileUrl()));
    }
}
