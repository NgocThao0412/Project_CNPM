package BUS;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import DAO.VisitorDAO;
import DTO.VisitorHeartbeatDTO;
import DTO.VisitorSessionDTO;
import DTO.VisitorStatsDTO;

@Service
public class VisitorBUS {

    private final VisitorDAO dao;
    private final int activeWindowSeconds;
    private final SecureRandom random = new SecureRandom();

    public VisitorBUS(
            VisitorDAO dao,
            @Value("${visitor.active-window-seconds:30}") int activeWindowSeconds
    ) {
        if (activeWindowSeconds < 10 || activeWindowSeconds > 300) {
            throw new IllegalArgumentException(
                    "Thời gian xác định phiên hoạt động phải từ 10 đến 300 giây"
            );
        }

        this.dao = dao;
        this.activeWindowSeconds = activeWindowSeconds;
    }

    public VisitorSessionDTO create() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);

        String token = HexFormat.of().formatHex(bytes);

        dao.create(hash(token));

        return new VisitorSessionDTO(
                token,
                30 * 60,
                10,
                activeWindowSeconds
        );
    }

    public void heartbeat(String token, VisitorHeartbeatDTO request) {
        byte[] tokenHash = hashValidatedToken(token);

        if (request == null || request.listening() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Vui lòng gửi listening là true hoặc false"
            );
        }

        int updated = dao.heartbeat(tokenHash, request.listening());

        if (updated == 0) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Phiên không tồn tại, đã kết thúc hoặc hết hạn"
            );
        }
    }

    public void end(String token) {
        // Gửi kết thúc nhiều lần vẫn an toàn.
        dao.end(hashValidatedToken(token));
    }

    public VisitorStatsDTO stats() {
        VisitorDAO.Counts counts = dao.countActive(activeWindowSeconds);

        return new VisitorStatsDTO(
                counts.activeVisitors(),
                counts.activeListeners(),
                activeWindowSeconds,
                Instant.now()
        );
    }

    private byte[] hashValidatedToken(String token) {
        if (token == null || !token.matches("[0-9a-f]{64}")) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Token phiên tham quan không hợp lệ"
            );
        }

        return hash(token);
    }

    private byte[] hash(String token) {
        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "Không thể tạo mã băm phiên",
                    exception
            );
        }
    }
}