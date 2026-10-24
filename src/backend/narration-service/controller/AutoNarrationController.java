package controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import BUS.AutoNarrationBUS;
import DTO.AutoNarrationRequestDTO;
import config.AdminAccess;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/admin/narrations/auto/jobs")
public class AutoNarrationController {

    private final AutoNarrationBUS bus;
    private final AdminAccess access;

    public AutoNarrationController(
            AutoNarrationBUS bus,
            AdminAccess access
    ) {
        this.bus = bus;
        this.access = access;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> start(
            @RequestBody AutoNarrationRequestDTO request,
            HttpServletRequest httpRequest
    ) {
        access.requireAdmin(httpRequest);

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(bus.start(request, adminCookie(httpRequest)));
    }

    @GetMapping("/{jobId}")
    public Map<String, Object> progress(
            @PathVariable("jobId") String jobId,
            HttpServletRequest request
    ) {
        access.requireAdmin(request);
        return bus.get(jobId);
    }

    private String adminCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("SGU_ADMIN_SESSION".equals(cookie.getName())) {
                    String value = cookie.getValue();

                    if (value != null
                            && value.matches("[A-Za-z0-9._-]{1,256}")) {
                        return "SGU_ADMIN_SESSION=" + value;
                    }
                }
            }
        }

        throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Không tìm thấy cookie quản trị hợp lệ"
        );
    }
}