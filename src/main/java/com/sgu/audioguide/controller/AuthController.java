package com.sgu.audioguide.controller;

import com.sgu.audioguide.BUS.AuthBUS;
import com.sgu.audioguide.DTO.AdminDTO;
import com.sgu.audioguide.DTO.LoginRequestDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthBUS authBUS;

    public AuthController(AuthBUS authBUS) {
        this.authBUS = authBUS;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequestDTO request,
            HttpServletRequest httpRequest) {
        AdminDTO admin;

        try {
            admin = authBUS.authenticate(request);
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "message", "Thông tin đăng nhập không hợp lệ"));
        }

        // Hủy phiên cũ và tạo phiên mới sau khi xác thực thành công.
        HttpSession oldSession = httpRequest.getSession(false);
        if (oldSession != null) {
            oldSession.invalidate();
        }

        HttpSession session = httpRequest.getSession(true);
        session.setAttribute("ADMIN_ID", admin.getId());
        session.setAttribute("AUTH_VERSION", admin.getAuthVersion());
        session.setMaxInactiveInterval(30 * 60);

        return ResponseEntity.ok(Map.of(
                "message", "Đăng nhập thành công",
                "admin", Map.of(
                        "id", admin.getId(),
                        "username", admin.getUsername(),
                        "fullName", admin.getFullName(),
                        "email", admin.getEmail())));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);

        if (session == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Bạn chưa đăng nhập"));
        }

        Object adminId = session.getAttribute("ADMIN_ID");
        Object authVersion = session.getAttribute("AUTH_VERSION");

        if (!(adminId instanceof Long)
                || !(authVersion instanceof Long)) {
            session.invalidate();

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Phiên đăng nhập không hợp lệ"));
        }

        AdminDTO admin;

        try {
            admin = authBUS.getAuthenticatedAdmin(
                    (Long) adminId,
                    (Long) authVersion);
        } catch (IllegalArgumentException exception) {
            session.invalidate();

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Phiên đăng nhập không hợp lệ"));
        }

        return ResponseEntity.ok(Map.of(
                "id", admin.getId(),
                "username", admin.getUsername(),
                "fullName", admin.getFullName(),
                "email", admin.getEmail()));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);

        if (session != null) {
            session.invalidate();
        }

        return ResponseEntity.ok(Map.of(
                "message", "Đăng xuất thành công"));
    }
}