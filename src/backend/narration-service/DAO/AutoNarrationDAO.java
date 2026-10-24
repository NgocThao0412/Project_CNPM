package DAO;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AutoNarrationDAO {

    private final JdbcTemplate db;

    public AutoNarrationDAO(JdbcTemplate db) {
        this.db = db;
    }

    public boolean poiExists(long poiId) {
        Long count = db.queryForObject(
                "SELECT COUNT(*) FROM points_of_interest WHERE id = ?",
                Long.class,
                poiId
        );

        return count != null && count > 0;
    }

    public Map<String, Long> activeLanguages() {
        Map<String, Long> result = new LinkedHashMap<>();

        var rows = db.query(
                """
                SELECT id, language_code
                FROM languages
                WHERE is_active = 1
                  AND language_code IN ('vi', 'en', 'zh', 'ko')
                ORDER BY id
                """,
                (row, index) -> Map.entry(
                        row.getString("language_code"),
                        row.getLong("id")
                )
        );

        for (var row : rows) {
            result.put(row.getKey(), row.getValue());
        }

        return result;
    }

    public Optional<Long> findContentId(long poiId, long languageId) {
        return db.query(
                """
                SELECT id
                FROM narration_contents
                WHERE poi_id = ?
                  AND language_id = ?
                """,
                (row, index) -> row.getLong("id"),
                poiId,
                languageId
        ).stream().findFirst();
    }
}