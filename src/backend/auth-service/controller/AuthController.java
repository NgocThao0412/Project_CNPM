package controller;

import BUS.AuthBUS;
import DTO.AdminDTO;
import DTO.LoginRequestDTO;
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
import DTO.ForgotPasswordRequestDTO;
import DTO.VerifyOtpRequestDTO;
import DTO.ResetPasswordRequestDTO;
import BUS.PasswordResetTransactionBUS;
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

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(
            @RequestBody ForgotPasswordRequestDTO request) {

        if (request == null || request.email() == null
                || request.email().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Vui lòng nhập email"));
        }

        try {
            String requestToken = authBUS.requestPasswordReset(request.email());

            return ResponseEntity.ok(Map.of(
                    "message",
                    "Nếu email đã đăng ký, hệ thống sẽ gửi mã OTP.",
                    "requestToken", requestToken));
        } catch (Exception exception) {
            return ResponseEntity.status(
                    HttpStatus.INTERNAL_SERVER_ERROR).body(
                            Map.of(
                                    "message", "Không thể xử lý yêu cầu lúc này"));
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(
            @RequestBody VerifyOtpRequestDTO request) {

        if (request == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Dữ liệu không hợp lệ"));
        }

        try {
            var result = authBUS.verifyPasswordResetOtp(
                    request.requestToken(),
                    request.otp());

            if (result == PasswordResetTransactionBUS.VerifyResult.SUCCESS) {
                return ResponseEntity.ok(Map.of(
                        "message", "Xác thực OTP thành công"));
            }

            if (result == PasswordResetTransactionBUS.VerifyResult.ALREADY_VERIFIED) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "OTP đã được xác thực"));
            }

            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "message", "OTP không hợp lệ, hết hạn hoặc đã bị khóa"));

        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", exception.getMessage()));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(
            @RequestBody ResetPasswordRequestDTO request) {

        if (request == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Dữ liệu không hợp lệ"));
        }

        try {
            authBUS.resetPassword(
                    request.requestToken(),
                    request.newPassword(),
                    request.confirmPassword());

            return ResponseEntity.ok(Map.of(
                    "message", "Đổi mật khẩu thành công. Vui lòng đăng nhập lại."));

        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", exception.getMessage()));
        }
    }
}