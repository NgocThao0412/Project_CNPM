package DAO;

import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ListeningDAO {

    private final JdbcTemplate db;

    public ListeningDAO(JdbcTemplate db) {
        this.db = db;
    }

    /*
     * BUS phải gọi trong transaction.
     * Khóa phiên để các yêu cầu nghe của cùng phiên
     * không ghi trùng khi đến đồng thời.
     */
    public Optional<Long> lockActiveSession(byte[] tokenHash) {
        return db.query("""
                SELECT id
                FROM visitor_sessions
                WHERE token_hash = ?
                  AND ended_at IS NULL
                  AND expires_at > UTC_TIMESTAMP(6)
                FOR UPDATE
                """,
                (result, row) -> result.getLong("id"),
                tokenHash
        ).stream().findFirst();
    }

    public Optional<SavedEvent> findEvent(String eventId) {
        return db.query("""
                SELECT session_id,
                       audio_file_id,
                       trigger_source
                FROM listening_logs
                WHERE event_id = ?
                """,
                (result, row) -> new SavedEvent(
                        result.getLong("session_id"),
                        result.getLong("audio_file_id"),
                        result.getString("trigger_source")
                ),
                eventId
        ).stream().findFirst();
    }

    /*
     * Chỉ ghi nhận audio mà khách được phép nghe.
     * Audio xem trước của admin không đi qua luồng này.
     */
    public boolean isPublicAudio(long audioFileId) {
        Integer count = db.queryForObject("""
                SELECT COUNT(*)
                FROM audio_files a
                JOIN narration_contents n
                  ON n.id = a.content_id
                JOIN points_of_interest p
                  ON p.id = n.poi_id
                JOIN campus_areas ca
                  ON ca.id = p.area_id
                JOIN languages l
                  ON l.id = n.language_id
                WHERE a.id = ?
                  AND a.is_visible = 1
                  AND n.is_visible = 1
                  AND p.is_visible = 1
                  AND ca.is_visible = 1
                  AND l.is_active = 1
                """,
                Integer.class,
                audioFileId
        );

        return count != null && count > 0;
    }

    public void insert(
            String eventId,
            long sessionId,
            long audioFileId,
            String triggerSource
    ) {
        db.update("""
                INSERT INTO listening_logs (
                    event_id,
                    session_id,
                    audio_file_id,
                    trigger_source,
                    started_at
                )
                VALUES (?, ?, ?, ?, UTC_TIMESTAMP(6))
                """,
                eventId,
                sessionId,
                audioFileId,
                triggerSource
        );
    }

    public record SavedEvent(
            long sessionId,
            long audioFileId,
            String triggerSource
    ) {
    }
}