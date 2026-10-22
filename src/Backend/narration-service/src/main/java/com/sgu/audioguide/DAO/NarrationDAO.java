package com.sgu.audioguide.DAO;

import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.sgu.audioguide.DTO.NarrationDTO;
import com.sgu.audioguide.DTO.NarrationWriteDTO;

@Repository
public class NarrationDAO {

    private final JdbcTemplate db;

    public NarrationDAO(JdbcTemplate db) {
        this.db = db;
    }
    private static final String SELECT = """
        SELECT n.id,n.poi_id,l.language_code,n.title,n.content,n.is_visible,
          (SELECT a.file_url FROM audio_files a WHERE a.content_id=n.id AND a.is_visible=1
             ORDER BY a.version_number DESC LIMIT 1) audio_url
        FROM narration_contents n JOIN languages l ON l.id=n.language_id
        JOIN points_of_interest p ON p.id=n.poi_id
        JOIN campus_areas ca ON ca.id=p.area_id
        """;

    private static NarrationDTO map(java.sql.ResultSet r, int row) throws java.sql.SQLException {
        return new NarrationDTO(r.getLong("id"), r.getLong("poi_id"), r.getString("language_code"),
                r.getString("title"), r.getString("content"), r.getString("audio_url"), r.getBoolean("is_visible"));
    }

    public List<NarrationDTO> visible() {
        return db.query(SELECT + " WHERE n.is_visible=1 AND l.is_active=1 AND p.is_visible=1 AND ca.is_visible=1 ORDER BY n.id", NarrationDAO::map);
    }

    public List<NarrationDTO> all() {
        return db.query(SELECT + " ORDER BY n.id", NarrationDAO::map);
    }

    public Optional<NarrationDTO> find(long id) {
        return db.query(SELECT + " WHERE n.id=?", NarrationDAO::map, id).stream().findFirst();
    }

    private Long languageId(String code) {
        List<Long> list = db.query("SELECT id FROM languages WHERE language_code=? AND is_active=1", (r, i) -> r.getLong(1), code);
        if (list.isEmpty()) {
            throw new IllegalArgumentException("Ngôn ngữ chưa hỗ trợ: " + code);
        }
        return list.getFirst();
    }

    @Transactional
    public NarrationDTO create(NarrationWriteDTO n) {
        Long lang = languageId(n.languageCode());
        db.update("INSERT INTO narration_contents(poi_id,language_id,title,content,is_visible) VALUES (?,?,?,?,?)",
                n.poiId(), lang, n.title(), n.content(), n.isVisible() == null || n.isVisible());
        Long id = db.queryForObject("SELECT id FROM narration_contents WHERE poi_id=? AND language_id=?", Long.class, n.poiId(), lang);
        return find(id).orElseThrow();
    }

    @Transactional
    public NarrationDTO update(long id, NarrationWriteDTO n) {
        NarrationDTO current = find(id).orElseThrow(() -> new IllegalArgumentException("Không có nội dung cần sửa"));
        // Identity is immutable in the database: create a new narration to change POI/language.
        if (!current.poiId().equals(n.poiId()) || !current.languageCode().equals(n.languageCode())) {
            throw new IllegalArgumentException("Không được đổi POI/ngôn ngữ của bài cũ");
        }
        db.update("UPDATE narration_contents SET title=?,content=?,is_visible=? WHERE id=?",
                n.title(), n.content(), n.isVisible() == null || n.isVisible(), id);
        return find(id).orElseThrow();
    }

    public int visible(long id, boolean enabled) {
        return db.update("UPDATE narration_contents SET is_visible=? WHERE id=?", enabled, id);
    }

    public List<NarrationDTO> visible(Long poiId, String languageCode) {
        String sql = SELECT + """
            WHERE n.is_visible = 1
              AND l.is_active = 1
              AND p.is_visible = 1
              AND ca.is_visible = 1
            """;

        java.util.List<Object> parameters = new java.util.ArrayList<>();

        if (poiId != null) {
            sql += " AND n.poi_id = ?";
            parameters.add(poiId);
        }

        if (languageCode != null) {
            sql += " AND l.language_code = ?";
            parameters.add(languageCode);
        }

        sql += " ORDER BY n.id";

        return db.query(
                sql,
                NarrationDAO::map,
                parameters.toArray()
        );
    }
}
