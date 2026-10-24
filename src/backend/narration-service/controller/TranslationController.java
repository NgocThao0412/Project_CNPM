package controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import BUS.TranslationBUS;
import DTO.TranslationRequestDTO;
import DTO.TranslationResultDTO;
import config.AdminAccess;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/admin/narrations")
public class TranslationController {

    private final TranslationBUS bus;
    private final AdminAccess access;

    public TranslationController(
            TranslationBUS bus,
            AdminAccess access
    ) {
        this.bus = bus;
        this.access = access;
    }

    @PostMapping("/translate")
    public TranslationResultDTO translate(
            @RequestBody TranslationRequestDTO request,
            HttpServletRequest httpRequest
    ) {
        access.requireAdmin(httpRequest);
        return bus.translate(request);
    }
}