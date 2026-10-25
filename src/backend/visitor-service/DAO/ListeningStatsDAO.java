package DAO;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import DTO.ListeningStatsDTO;

@Repository
public class ListeningStatsDAO {

    private final JdbcTemplate db;

    public ListeningStatsDAO(JdbcTemplate db) {
        this.db = db;
    }

    /*
     * started_at được lưu theo UTC.
     * Hai tham số thời gian là chuỗi UTC do BUS chuẩn bị.
     *
     * Không lọc trạng thái hiển thị của POI/audio:
     * dữ liệu lịch sử vẫn cần được thống kê sau khi bị ẩn.
     */
    private Query buildQuery(
            String fromUtc,
            String untilUtc,
            Long poiId,
            String languageCode
    ) {
        StringBuilder sql = new StringBuilder("""
                FROM listening_logs ll
                JOIN audio_files a
                  ON a.id = ll.audio_file_id
                JOIN narration_contents n
                  ON n.id = a.content_id
                JOIN points_of_interest p
                  ON p.id = n.poi_id
                JOIN languages l
                  ON l.id = n.language_id
                WHERE ll.started_at >= ?
                  AND ll.started_at < ?
                """);

        List<Object> parameters = new ArrayList<>();
        parameters.add(fromUtc);
        parameters.add(untilUtc);

        if (poiId != null) {
            sql.append(" AND p.id = ? ");
            parameters.add(poiId);
        }

        if (languageCode != null) {
            sql.append(" AND l.language_code = ? ");
            parameters.add(languageCode);
        }

        return new Query(
                sql.toString(),
                parameters.toArray()
        );
    }

    public Totals totals(
            String fromUtc,
            String untilUtc,
            Long poiId,
            String languageCode
    ) {
        Query query = buildQuery(
                fromUtc, untilUtc, poiId, languageCode
        );

        return db.queryForObject("""
                SELECT COUNT(*) AS total_listens,
                       COUNT(DISTINCT ll.session_id)
                           AS unique_sessions
                """ + query.sql(),
                (result, row) -> new Totals(
                        result.getLong("total_listens"),
                        result.getLong("unique_sessions")
                ),
                query.parameters()
        );
    }

    public List<ListeningStatsDTO.Row> rows(
            String fromUtc,
            String untilUtc,
            Long poiId,
            String languageCode
    ) {
        Query query = buildQuery(
                fromUtc, untilUtc, poiId, languageCode
        );

        /*
         * Nhóm ngày theo giờ Việt Nam (UTC+7),
         * không dùng ngày UTC để tránh lệch ngày trên dashboard.
         */
        String sql = """
                SELECT DATE(
                           DATE_ADD(
                               ll.started_at,
                               INTERVAL 7 HOUR
                           )
                       ) AS listen_date,
                       p.id AS poi_id,
                       p.poi_name,
                       l.language_code,
                       l.language_name,
                       COUNT(*) AS listens,
                       COUNT(DISTINCT ll.session_id)
                           AS unique_sessions
                """ + query.sql() + """
                GROUP BY listen_date,
                         p.id,
                         p.poi_name,
                         l.language_code,
                         l.language_name
                ORDER BY listen_date ASC,
                         p.id ASC,
                         l.language_code ASC
                """;

        return db.query(
                sql,
                (result, row) -> new ListeningStatsDTO.Row(
                        LocalDate.parse(
                                result.getString("listen_date")
                        ),
                        result.getLong("poi_id"),
                        result.getString("poi_name"),
                        result.getString("language_code"),
                        result.getString("language_name"),
                        result.getLong("listens"),
                        result.getLong("unique_sessions")
                ),
                query.parameters()
        );
    }

    public record Totals(
            long totalListens,
            long uniqueSessions
    ) {
    }

    private record Query(
            String sql,
            Object[] parameters
    ) {
    }
}