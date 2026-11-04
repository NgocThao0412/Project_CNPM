package controller;

import BUS.POIImageBUS;
import DTO.POIImageDTO;
import client.AuthServiceClient;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class POIImageController {
    private final POIImageBUS bus;
    private final AuthServiceClient access;
    public POIImageController(POIImageBUS bus, AuthServiceClient access) {
        this.bus = bus;
        this.access = access;
    }

    @GetMapping("/api/admin/pois/{poiId}/images")
    public List<POIImageDTO> list(@PathVariable("poiId") long poiId, HttpServletRequest request) {
        access.requireAdmin(request);
        return bus.list(poiId);
    }

    @PostMapping(value = "/api/admin/pois/{poiId}/images",
            consumes = {"image/jpeg", "image/png", "image/webp"})
    public ResponseEntity<POIImageDTO> upload(@PathVariable("poiId") long poiId,
            @RequestParam(value = "filename", defaultValue = "") String filename,
            HttpServletRequest request) throws IOException {
        access.requireAdmin(request);
        if (request.getContentLengthLong() > POIImageBUS.MAX_BYTES)
            throw new ResponseStatusException(HttpStatus.CONTENT_TOO_LARGE, "Ảnh tối đa 5 MB");
        byte[] data = request.getInputStream().readNBytes(POIImageBUS.MAX_BYTES + 1);
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(bus.upload(poiId, filename, request.getContentType(), data));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        } catch (NoSuchElementException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        } catch (DataAccessException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Không thể lưu thông tin ảnh");
        }
    }

    @DeleteMapping("/api/admin/pois/{poiId}/images/{imageId}")
    public ResponseEntity<Void> delete(@PathVariable("poiId") long poiId,
            @PathVariable("imageId") long imageId, HttpServletRequest request) {
        access.requireAdmin(request);
        try {
            bus.delete(poiId, imageId);
            return ResponseEntity.noContent().build();
        } catch (NoSuchElementException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    @GetMapping("/api/admin/pois/{poiId}/images/{imageId}/file")
    public ResponseEntity<byte[]> preview(@PathVariable("poiId") long poiId,
            @PathVariable("imageId") long imageId, HttpServletRequest request) throws IOException {
        access.requireAdmin(request);
        try {
            return imageResponse(bus.readAdmin(poiId, imageId));
        } catch (NoSuchElementException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    @GetMapping("/poi-image-files/{poiId}/{filename:.+}")
    public ResponseEntity<byte[]> publicImage(@PathVariable("poiId") long poiId,
            @PathVariable("filename") String filename) throws IOException {
        try {
            return imageResponse(bus.readPublic(poiId, filename));
        } catch (NoSuchElementException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    private ResponseEntity<byte[]> imageResponse(POIImageBUS.ImageData image) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.mime()))
                .cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .body(image.bytes());
    }
}
