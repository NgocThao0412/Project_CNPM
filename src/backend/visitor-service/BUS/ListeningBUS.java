package BUS;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import DAO.ListeningDAO;
import DTO.ListeningEventDTO;

@Service
public class ListeningBUS {

    private final ListeningDAO dao;

    public ListeningBUS(ListeningDAO dao) {
        this.dao = dao;
    }

    @Transactional
    public void record(String token, ListeningEventDTO request) {
        byte[] tokenHash = hashValidatedToken(token);
        validate(request);

        long sessionId = dao.lockActiveSession(tokenHash)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Phiên không tồn tại, đã kết thúc hoặc hết hạn"
                ));

        var existing = dao.findEvent(request.eventId());

        if (existing.isPresent()) {
            ListeningDAO.SavedEvent saved = existing.get();

            boolean sameEvent =
                    saved.sessionId() == sessionId
                    && saved.audioFileId() == request.audioFileId()
                    && saved.triggerSource().equals(
                            request.triggerSource()
                    );

            if (!sameEvent) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Mã sự kiện đã được sử dụng cho lượt nghe khác"
                );
            }

            // Retry cùng sự kiện: thành công, không thêm bản ghi.
            return;
        }

        if (!dao.isPublicAudio(request.audioFileId())) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Audio không tồn tại hoặc chưa được phát hành"
            );
        }

        try {
            dao.insert(
                    request.eventId(),
                    sessionId,
                    request.audioFileId(),
                    request.triggerSource()
            );
        } catch (DuplicateKeyException exception) {
            // Trường hợp cùng mã sự kiện được gửi từ phiên khác.
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Mã sự kiện đã được sử dụng",
                    exception
            );
        }
    }

    private void validate(ListeningEventDTO request) {
        if (request == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Thiếu thông tin lượt nghe"
            );
        }

        String eventId = request.eventId();

        if (eventId == null || !eventId.matches(
                "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}"
                + "-[0-9a-f]{4}-[0-9a-f]{12}"
        )) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "eventId phải là UUID hợp lệ viết thường"
            );
        }

        if (request.audioFileId() == null
                || request.audioFileId() <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "audioFileId phải lớn hơn 0"
            );
        }

        if (!"GPS".equals(request.triggerSource())
                && !"SIMULATION".equals(request.triggerSource())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "triggerSource chỉ nhận GPS hoặc SIMULATION"
            );
        }
    }

    private byte[] hashValidatedToken(String token) {
        if (token == null || !token.matches("[0-9a-f]{64}")) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Token phiên tham quan không hợp lệ"
            );
        }

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