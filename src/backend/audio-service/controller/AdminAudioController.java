package controller;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import BUS.AudioBUS;
import DTO.AudioDTO;
import config.AdminAccess;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/admin/audio")
public class AdminAudioController {

    private static final int MAX_BYTES = 20 * 1024 * 1024;

    private final AudioBUS bus;
    private final AdminAccess access;

    public AdminAudioController(AudioBUS bus, AdminAccess access) {
        this.bus = bus;
        this.access = access;
    }

    @PostMapping(consumes = "audio/mpeg")
    @ResponseStatus(HttpStatus.CREATED)
    public AudioDTO upload(
            @RequestParam("contentId") long contentId,
            @RequestParam("filename") String filename,
            HttpServletRequest request
    ) throws IOException {
        access.requireAdmin(request);

        if (request.getContentLengthLong() > MAX_BYTES) {
            throw new ResponseStatusException(
                    HttpStatus.PAYLOAD_TOO_LARGE,
                    "File audio tối đa 20 MB"
            );
        }

        byte[] bytes = request.getInputStream()
                .readNBytes(MAX_BYTES + 1);

        return bus.uploadRaw(contentId, filename, bytes);
    }
}