package controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import BUS.SpeechBUS;
import DTO.AudioDTO;
import config.AdminAccess;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/admin/audio")
public class SpeechController {

    private final SpeechBUS bus;
    private final AdminAccess access;

    public SpeechController(SpeechBUS bus, AdminAccess access) {
        this.bus = bus;
        this.access = access;
    }

    @PostMapping("/{contentId}/generate")
    public ResponseEntity<AudioDTO> generate(
            @PathVariable("contentId") long contentId,
            HttpServletRequest request
    ) {
        access.requireAdmin(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bus.generate(contentId));
    }
}