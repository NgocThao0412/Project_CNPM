package com.sgu.audioguide.DAO;

import com.sgu.audioguide.DTO.CreatePOIRequestDTO;
import com.sgu.audioguide.DTO.POIDTO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import com.sgu.audioguide.DTO.UpdatePOIRequestDTO;
import java.sql.PreparedStatement;
import java.sql.Statement;
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

    public boolean existsByCode(String poiCode) {
        String sql = """
                SELECT COUNT(*)
                FROM points_of_interest
                WHERE poi_code = ?
                """;

        Long count = jdbcTemplate.queryForObject(
                sql, Long.class, poiCode
        );

        return count != null && count > 0;
    }

    public boolean areaExists(Long areaId) {
        String sql = """
                SELECT COUNT(*)
                FROM campus_areas
                WHERE id = ?
                """;

        Long count = jdbcTemplate.queryForObject(
                sql, Long.class, areaId
        );

        return count != null && count > 0;
    }

    public Long insert(CreatePOIRequestDTO request) {
        String sql = """
                INSERT INTO points_of_interest (
                    area_id,
                    poi_code,
                    poi_name,
                    description,
                    latitude,
                    longitude,
                    trigger_radius_m,
                    is_visible
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, TRUE)
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    sql,
                    Statement.RETURN_GENERATED_KEYS
            );

            statement.setLong(1, request.areaId());
            statement.setString(2, request.poiCode().trim());
            statement.setString(3, request.poiName().trim());
            statement.setString(4, request.description());
            statement.setBigDecimal(5, request.latitude());
            statement.setBigDecimal(6, request.longitude());
            statement.setBigDecimal(7, request.triggerRadiusM());

            return statement;
        }, keyHolder);

        Number generatedId = keyHolder.getKey();

        if (generatedId == null) {
            throw new IllegalStateException(
                    "Không lấy được mã POI sau khi thêm"
            );
        }

        return generatedId.longValue();
    }
    public boolean existsById(Long id) {
    String sql = """
            SELECT COUNT(*)
            FROM points_of_interest
            WHERE id = ?
            """;

    Long count = jdbcTemplate.queryForObject(
            sql, Long.class, id
    );

    return count != null && count > 0;
}

public boolean existsByCodeExcludingId(String poiCode, Long id) {
    String sql = """
            SELECT COUNT(*)
            FROM points_of_interest
            WHERE poi_code = ?
              AND id <> ?
            """;

    Long count = jdbcTemplate.queryForObject(
            sql, Long.class, poiCode, id
    );

    return count != null && count > 0;
}

public int update(Long id, UpdatePOIRequestDTO request) {
    String sql = """
            UPDATE points_of_interest
            SET area_id = ?,
                poi_code = ?,
                poi_name = ?,
                description = ?
            WHERE id = ?
            """;

    return jdbcTemplate.update(
            sql,
            request.areaId(),
            request.poiCode().trim(),
            request.poiName().trim(),
            request.description(),
            id
    );
}
}