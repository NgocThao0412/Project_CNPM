package BUS;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import DAO.ListeningStatsDAO;
import DTO.ListeningStatsDTO;

@Service
public class ListeningStatsBUS {

    private static final ZoneOffset VIETNAM = ZoneOffset.ofHours(7);

    private static final DateTimeFormatter SQL_TIME =
            DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss");

    private static final Set<String> LANGUAGES =
            Set.of("vi", "en", "zh", "ko");

    private final ListeningStatsDAO dao;

    public ListeningStatsBUS(ListeningStatsDAO dao) {
        this.dao = dao;
    }

    @Transactional(
            readOnly = true,
            isolation = Isolation.REPEATABLE_READ
    )
    public ListeningStatsDTO stats(
            LocalDate from,
            LocalDate to,
            Long poiId,
            String languageCode
    ) {
        // Mặc định: 30 ngày gần nhất, tính cả hôm nay.
        if (to == null) {
            to = LocalDate.now(VIETNAM);
        }

        if (to.getYear() < 1001 || to.getYear() > 9998) {
            throw badRequest("Ngày kết thúc ngoài phạm vi hỗ trợ");
        }

        if (from == null) {
            from = to.minusDays(29);
        }

        if (from.getYear() < 1001 || from.getYear() > 9998) {
            throw badRequest("Ngày bắt đầu ngoài phạm vi hỗ trợ");
        }

        if (from.isAfter(to)) {
            throw badRequest(
                    "Ngày bắt đầu không được sau ngày kết thúc"
            );
        }

        long days = ChronoUnit.DAYS.between(from, to) + 1;

        if (days > 366) {
            throw badRequest(
                    "Mỗi lần thống kê tối đa 366 ngày"
            );
        }

        if (poiId != null && poiId <= 0) {
            throw badRequest("poiId phải lớn hơn 0");
        }

        String code = null;

        if (languageCode != null && !languageCode.isBlank()) {
            code = languageCode.trim()
                    .toLowerCase(Locale.ROOT);

            if (!LANGUAGES.contains(code)) {
                throw badRequest(
                        "Ngôn ngữ chỉ nhận vi, en, zh hoặc ko"
                );
            }
        }

        /*
         * Người dùng chọn ngày theo giờ Việt Nam.
         * Truy vấn bằng UTC; lấy trọn ngày kết thúc
         * bằng mốc đầu ngày kế tiếp, không bao gồm mốc đó.
         */
        String fromUtc = from.atStartOfDay()
                .atOffset(VIETNAM)
                .withOffsetSameInstant(ZoneOffset.UTC)
                .format(SQL_TIME);

        String untilUtc = to.plusDays(1)
                .atStartOfDay()
                .atOffset(VIETNAM)
                .withOffsetSameInstant(ZoneOffset.UTC)
                .format(SQL_TIME);

        var totals = dao.totals(
                fromUtc, untilUtc, poiId, code
        );

        var rows = dao.rows(
                fromUtc, untilUtc, poiId, code
        );

        return new ListeningStatsDTO(
                from,
                to,
                "UTC+07:00",
                totals.totalListens(),
                totals.uniqueSessions(),
                rows
        );
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                message
        );
    }
}