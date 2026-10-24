package DAO;

import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SpeechDAO {

    private final JdbcTemplate db;

    private static final String SELECT_SOURCE = """
            SELECT n.id, n.content,
                   l.language_code, l.is_active
            FROM narration_contents n
            JOIN languages l ON l.id = n.language_id
            WHERE n.id = ?
            """;

    public SpeechDAO(JdbcTemplate db) {
        this.db = db;
    }

    public Optional<Source> find(long contentId) {
        return read(contentId, false);
    }

    // Chỉ gọi trong transaction khi chuẩn bị lưu audio.
    public Optional<Source> lock(long contentId) {
        return read(contentId, true);
    }

    private Optional<Source> read(long contentId, boolean lock) {
        String sql = SELECT_SOURCE + (lock ? " FOR UPDATE" : "");

        return db.query(
                sql,
                (row, index) -> new Source(
                        row.getLong("id"),
                        row.getString("content"),
                        row.getString("language_code"),
                        row.getBoolean("is_active")
                ),
                contentId
        ).stream().findFirst();
    }

    public record Source(
            long id,
            String content,
            String languageCode,
            boolean languageActive
    ) {
    }
}