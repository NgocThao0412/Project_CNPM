package com.sgu.audioguide.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.sgu.audioguide.BUS.AudioBUS;
import com.sgu.audioguide.DTO.AudioDTO;
import com.sgu.audioguide.config.AdminAccess;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/audio")
public class AudioController {

    private final AudioBUS bus;
    private final AdminAccess access;

    public AudioController(AudioBUS bus, AdminAccess access) {
        this.bus = bus;
        this.access = access;
    }

    @GetMapping
    public List<AudioDTO> list(
            @RequestParam("contentId") long contentId,
            @RequestParam(name = "admin", defaultValue = "0") int adminMode,
            HttpServletRequest request
    ) {
        validateAdminMode(adminMode);

        boolean adminPreview = adminMode == 1;

        if (adminPreview) {
            access.requireAdmin(request);
        }

        return bus.listByContent(contentId, adminPreview);
    }

    @PostMapping(
            value = "/narrations/{id}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public AudioDTO upload(
            @PathVariable("id") long id,
            @RequestPart("audio") MultipartFile file,
            @RequestParam("durationSeconds") double durationSeconds,
            HttpServletRequest request
    ) throws IOException {
        access.requireAdmin(request);
        return bus.upload(id, file, durationSeconds);
    }

    @GetMapping(
            value = {"/files/{id}.mp3", "/{id}/stream"},
            produces = "audio/mpeg"
    )
    public ResponseEntity<Resource> download(
            @PathVariable("id") long id,
            @RequestParam(name = "admin", defaultValue = "0") int adminMode,
            HttpServletRequest request
    ) {
        validateAdminMode(adminMode);

        Resource audio;

        if (adminMode == 1) {
            access.requireAdmin(request);
            audio = bus.openForPreview(id);
        } else {
            audio = bus.open(id);
        }

        return ResponseEntity.ok()
                .header("Cache-Control", "no-store")
                .contentType(MediaType.parseMediaType("audio/mpeg"))
                .body(audio);
    }

    private void validateAdminMode(int adminMode) {
        if (adminMode != 0 && adminMode != 1) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "admin chỉ nhận 0 hoặc 1"
            );
        }
    }
}