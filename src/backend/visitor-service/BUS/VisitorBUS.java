package BUS;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.DateTimeException;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
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

    private static final ZoneOffset VIETNAM = ZoneOffset.ofHours(7);
    private static final DateTimeFormatter SQL_TIME =
            DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss");

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

    public VisitorStatsDTO.History history(
            String period,
            Integer day,
            Integer month,
            Integer year
    ) {
        String selected = period == null ? "month" : period;
        if (!selected.equals("day") && !selected.equals("month")
                && !selected.equals("year")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Chỉ hỗ trợ thống kê ngày, tháng hoặc năm"
            );
        }

        LocalDate today = LocalDate.now(VIETNAM);
        int selectedYear = year == null ? today.getYear() : year;
        int selectedMonth = month == null ? today.getMonthValue() : month;
        int selectedDay = day == null ? today.getDayOfMonth() : day;
        if (selectedYear < 2000 || selectedYear > today.getYear() + 1) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Năm thống kê không hợp lệ"
            );
        }

        LocalDate from;
        LocalDate until;
        try {
            switch (selected) {
                case "day" -> {
                    from = LocalDate.of(selectedYear, selectedMonth, selectedDay);
                    until = from.plusDays(1);
                }
                case "month" -> {
                    from = LocalDate.of(selectedYear, selectedMonth, 1);
                    until = from.plusMonths(1);
                }
                default -> {
                    from = LocalDate.of(selectedYear, 1, 1);
                    until = from.plusYears(1);
                }
            }
        } catch (DateTimeException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Ngày hoặc tháng thống kê không hợp lệ"
            );
        }

        String fromUtc = from.atStartOfDay().atOffset(VIETNAM)
                .withOffsetSameInstant(ZoneOffset.UTC).format(SQL_TIME);
        String untilUtc = until.atStartOfDay().atOffset(VIETNAM)
                .withOffsetSameInstant(ZoneOffset.UTC).format(SQL_TIME);

        var rows = dao.history(fromUtc, untilUtc, selected);
        long total = rows.stream().mapToLong(VisitorStatsDTO.HistoryRow::visits).sum();
        return new VisitorStatsDTO.History(
                selected, from, until.minusDays(1), "UTC+07:00", total, rows
        );
    }

    public VisitorStatsDTO.Report report(LocalDate from, LocalDate to) {
        if (from == null || to == null
                || from.getYear() < 1001 || from.getYear() > 9998
                || to.getYear() < 1001 || to.getYear() > 9998
                || from.isAfter(to)
                || ChronoUnit.DAYS.between(from, to) + 1 > 366) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Khoảng ngày thống kê không hợp lệ (tối đa 366 ngày)"
            );
        }

        String fromUtc = from.atStartOfDay().atOffset(VIETNAM)
                .withOffsetSameInstant(ZoneOffset.UTC).format(SQL_TIME);
        String untilUtc = to.plusDays(1).atStartOfDay().atOffset(VIETNAM)
                .withOffsetSameInstant(ZoneOffset.UTC).format(SQL_TIME);

        return new VisitorStatsDTO.Report(
                from, to, "UTC+07:00", dao.countVisits(fromUtc, untilUtc)
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