package DAO;

import DTO.POIImageDTO;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import java.sql.Statement;
import org.springframework.stereotype.Repository;

@Repository
public class POIImageDAO {
    private final JdbcTemplate jdbc;
    public POIImageDAO(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<POIImageDTO> list(long poiId) {
        return jdbc.query("""
            SELECT id, poi_id, file_url, caption, sort_order
              FROM poi_images WHERE poi_id = ? ORDER BY sort_order, id
            """, (rs, n) -> new POIImageDTO(rs.getLong("id"), rs.getLong("poi_id"),
                    rs.getString("file_url"), rs.getString("caption"), rs.getInt("sort_order")), poiId);
    }

    public Optional<POIImageDTO> find(long poiId, long imageId) {
        return jdbc.query("""
            SELECT id, poi_id, file_url, caption, sort_order
              FROM poi_images WHERE poi_id = ? AND id = ?
            """, (rs, n) -> new POIImageDTO(rs.getLong("id"), rs.getLong("poi_id"),
                    rs.getString("file_url"), rs.getString("caption"), rs.getInt("sort_order")),
            poiId, imageId).stream().findFirst();
    }

    /** Lock the owning POI to serialize image-position allocation. */
    public boolean lockPoi(long poiId) {
        return !jdbc.queryForList("SELECT id FROM points_of_interest WHERE id = ? FOR UPDATE", poiId).isEmpty();
    }

    public boolean lockPoiReadOnly(long poiId) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM points_of_interest WHERE id = ?", Integer.class, poiId);
        return count != null && count > 0;
    }

    public int count(long poiId) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM poi_images WHERE poi_id = ?", Integer.class, poiId);
        return count == null ? 0 : count;
    }

    public int nextPosition(long poiId) {
        Integer next = jdbc.queryForObject("SELECT COALESCE(MAX(sort_order), 0) + 1 FROM poi_images WHERE poi_id = ?", Integer.class, poiId);
        return next == null ? 1 : next;
    }

    public long insert(long poiId, String url, int position) {
        KeyHolder key = new GeneratedKeyHolder();
        jdbc.update(conn -> {
            var ps = conn.prepareStatement("INSERT INTO poi_images (poi_id, file_url, sort_order) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, poiId);
            ps.setString(2, url);
            ps.setInt(3, position);
            return ps;
        }, key);
        if (key.getKey() == null) throw new IllegalStateException("Không lấy được ID ảnh");
        return key.getKey().longValue();
    }

    public int delete(long poiId, long imageId) {
        return jdbc.update("DELETE FROM poi_images WHERE poi_id = ? AND id = ?", poiId, imageId);
    }

    /** Public media is accessible only for an image belonging to a visible POI + area. */
    public boolean isPublic(long poiId, String url) {
        Integer count = jdbc.queryForObject("""
            SELECT COUNT(*) FROM poi_images i
            JOIN points_of_interest p ON p.id = i.poi_id
            JOIN campus_areas a ON a.id = p.area_id
            WHERE i.poi_id = ? AND i.file_url = ?
              AND p.is_visible = TRUE AND a.is_visible = TRUE
            """, Integer.class, poiId, url);
        return count != null && count > 0;
    }
}
