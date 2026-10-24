package DAO;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class VisitorDAO {

    private final JdbcTemplate db;

    public VisitorDAO(JdbcTemplate db) {
        this.db = db;
    }

    // Chỉ lưu mã băm của token, không lưu token gốc.
    public void create(byte[] tokenHash) {
        db.update("""
                INSERT INTO visitor_sessions (
                    token_hash,
                    started_at,
                    expires_at,
                    last_seen_at,
                    is_listening
                )
                VALUES (
                    ?,
                    UTC_TIMESTAMP(6),
                    DATE_ADD(UTC_TIMESTAMP(6), INTERVAL 30 MINUTE),
                    UTC_TIMESTAMP(6),
                    0
                )
                """, tokenHash);
    }

    // Phiên hết hạn hoặc đã kết thúc không được tự kích hoạt lại.
    public int heartbeat(byte[] tokenHash, boolean listening) {
        return db.update("""
                UPDATE visitor_sessions
                SET last_seen_at = UTC_TIMESTAMP(6),
                    is_listening = ?,
                    expires_at = DATE_ADD(
                        UTC_TIMESTAMP(6),
                        INTERVAL 30 MINUTE
                    )
                WHERE token_hash = ?
                  AND ended_at IS NULL
                  AND expires_at > UTC_TIMESTAMP(6)
                """, listening, tokenHash);
    }

    public int end(byte[] tokenHash) {
        return db.update("""
                UPDATE visitor_sessions
                SET ended_at = UTC_TIMESTAMP(6),
                    is_listening = 0
                WHERE token_hash = ?
                  AND ended_at IS NULL
                """, tokenHash);
    }

    public Counts countActive(int activeWindowSeconds) {
        return db.queryForObject("""
                SELECT COUNT(*) AS active_visitors,
                       COALESCE(
                           SUM(
                               CASE
                                   WHEN is_listening = 1 THEN 1
                                   ELSE 0
                               END
                           ),
                           0
                       ) AS active_listeners
                FROM visitor_sessions
                WHERE ended_at IS NULL
                  AND expires_at > UTC_TIMESTAMP(6)
                  AND last_seen_at >= DATE_SUB(
                      UTC_TIMESTAMP(6),
                      INTERVAL ? SECOND
                  )
                """,
                (result, row) -> new Counts(
                        result.getLong("active_visitors"),
                        result.getLong("active_listeners")
                ),
                activeWindowSeconds
        );
    }

    public record Counts(
            long activeVisitors,
            long activeListeners
    ) {
    }
}