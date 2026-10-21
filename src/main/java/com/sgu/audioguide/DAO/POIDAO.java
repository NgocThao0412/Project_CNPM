package com.sgu.audioguide.DAO;

import com.sgu.audioguide.DTO.POIDTO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class POIDAO {

    private final JdbcTemplate jdbcTemplate;

    public POIDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<POIDTO> findVisiblePOIs() {
        String sql = """
                SELECT p.id,
                       p.poi_code,
                       p.poi_name,
                       p.area_id,
                       a.area_name,
                       p.description,
                       p.latitude,
                       p.longitude,
                       p.trigger_radius_m,
                       (
                           SELECT i.file_url
                           FROM poi_images i
                           WHERE i.poi_id = p.id
                           ORDER BY i.sort_order
                           LIMIT 1
                       ) AS image_url
                FROM points_of_interest p
                JOIN campus_areas a ON a.id = p.area_id
                WHERE p.is_visible = TRUE
                  AND a.is_visible = TRUE
                ORDER BY p.poi_name, p.id
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new POIDTO(
                rs.getLong("id"),
                rs.getString("poi_code"),
                rs.getString("poi_name"),
                rs.getLong("area_id"),
                rs.getString("area_name"),
                rs.getString("description"),
                rs.getBigDecimal("latitude"),
                rs.getBigDecimal("longitude"),
                rs.getBigDecimal("trigger_radius_m"),
                rs.getString("image_url")
        ));
    }
}