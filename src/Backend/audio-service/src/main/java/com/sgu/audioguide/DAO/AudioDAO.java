package com.sgu.audioguide.DAO;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.sgu.audioguide.DTO.AudioDTO;

@Repository
public class AudioDAO {

    private final JdbcTemplate db;

    public AudioDAO(JdbcTemplate db) {
        this.db = db;
    }

    private static AudioDTO map(java.sql.ResultSet r, int row) throws java.sql.SQLException {
        return new AudioDTO(r.getLong("id"), r.getLong("content_id"), r.getString("file_name"),
                r.getString("file_url"), r.getLong("file_size_bytes"), r.getDouble("duration_seconds"),
                r.getInt("version_number"), r.getBoolean("is_visible"));
    }

    public Optional<AudioDTO> active(long id) {
        return db.query("""
     SELECT a.id,a.content_id,a.file_name,a.file_url,a.file_size_bytes,a.duration_seconds,a.version_number,a.is_visible
     FROM audio_files a JOIN narration_contents n ON n.id=a.content_id
     JOIN points_of_interest p ON p.id=n.poi_id
     JOIN campus_areas ca ON ca.id=p.area_id
     JOIN languages l ON l.id=n.language_id
     WHERE a.id=? AND a.is_visible=1 AND n.is_visible=1 AND p.is_visible=1 AND ca.is_visible=1 AND l.is_active=1
     """, AudioDAO::map, id).stream().findFirst();
    }

    @Transactional
    public AudioDTO save(long contentId, String storedName, String filename, long size, double duration, String url) {
        Integer count = db.queryForObject(
                "SELECT COUNT(*) FROM narration_contents WHERE id=?",
                Integer.class,
                contentId
        );
        if (count == null || count == 0) {
            throw new IllegalArgumentException("Nội dung thuyết minh không tồn tại");
        }
        // SERIALIZED within the database for same content_id; avoids duplicate active file/version on parallel uploads.
        db.queryForObject("SELECT id FROM narration_contents WHERE id=? FOR UPDATE", Long.class, contentId);
        Integer previous = db.queryForObject("SELECT COALESCE(MAX(version_number),0) FROM audio_files WHERE content_id=?", Integer.class, contentId);
        int version = previous + 1;
        db.update("UPDATE audio_files SET is_visible=0 WHERE content_id=? AND is_visible=1", contentId);
        GeneratedKeyHolder holder = new GeneratedKeyHolder();
        db.update(conn -> {
            PreparedStatement st = conn.prepareStatement("""
       INSERT INTO audio_files(content_id,file_name,file_url,mime_type,file_size_bytes,duration_seconds,version_number,is_visible)
       VALUES (?,?,?,'audio/mpeg',?,?,?,TRUE)
       """, Statement.RETURN_GENERATED_KEYS);
            st.setLong(1, contentId);
            st.setString(2, filename);
            st.setString(3, url);
            st.setLong(4, size);
            st.setDouble(5, duration);
            st.setInt(6, version);
            return st;
        }, holder);
        Long id = holder.getKey().longValue();
        // file_url is assigned later because it contains the generated id.
        db.update("UPDATE audio_files SET file_url=? WHERE id=?", url.replace("/pending.mp3", "/" + id + ".mp3"), id);
        return new AudioDTO(id, contentId, filename, url.replace("/pending.mp3", "/" + id + ".mp3"), size, duration, version, true);
    }

    public List<AudioDTO> listByContent(long contentId, boolean adminPreview) {
        String sql = """
            SELECT a.id, a.content_id, a.file_name, a.file_url,
                   a.file_size_bytes, a.duration_seconds,
                   a.version_number, a.is_visible
            FROM audio_files a
            JOIN narration_contents n ON n.id = a.content_id
            JOIN points_of_interest p ON p.id = n.poi_id
            JOIN campus_areas ca ON ca.id = p.area_id
            JOIN languages l ON l.id = n.language_id
            WHERE a.content_id = ?
              AND a.is_visible = 1
            """;

        if (!adminPreview) {
            sql += """
                  AND n.is_visible = 1
                  AND p.is_visible = 1
                  AND ca.is_visible = 1
                  AND l.is_active = 1
                """;
        }

        sql += " ORDER BY a.version_number DESC";

        return db.query(sql, AudioDAO::map, contentId);
    }

}
