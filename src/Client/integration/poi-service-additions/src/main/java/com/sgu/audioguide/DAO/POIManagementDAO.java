package com.sgu.audioguide.DAO;
import com.sgu.audioguide.DTO.*;
import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;
@Repository public class POIManagementDAO {
    private final JdbcTemplate jdbc;
    public POIManagementDAO(JdbcTemplate jdbc) {
        this.jdbc=jdbc;
    }
    public List<POIManagementDTO> findAll() {
        return jdbc.query("""
 SELECT p.id,p.poi_code,p.poi_name,p.area_id,a.area_name,p.description,p.latitude,p.longitude,p.trigger_radius_m,p.is_visible,
 (SELECT i.file_url FROM poi_images i WHERE i.poi_id=p.id ORDER BY i.sort_order LIMIT 1) AS image_url
 FROM points_of_interest p JOIN campus_areas a ON a.id=p.area_id ORDER BY p.poi_name,p.id
 """,(r,n)->new POIManagementDTO(r.getLong("id"),r.getString("poi_code"),r.getString("poi_name"),r.getLong("area_id"),r.getString("area_name"),r.getString("description"),r.getBigDecimal("latitude"),r.getBigDecimal("longitude"),r.getBigDecimal("trigger_radius_m"),r.getString("image_url"),r.getBoolean("is_visible")));
    }
    public List<Map<String,Object>> areas() {
        return jdbc.query("SELECT id,area_code,area_name,is_visible FROM campus_areas ORDER BY area_name,id",(r,n)->Map.<String,Object>of("id",r.getLong("id"),"areaCode",r.getString("area_code"),"areaName",r.getString("area_name"),"visible",r.getBoolean("is_visible")));
    }
    public boolean exists(Long id) {
        Long n=jdbc.queryForObject("SELECT COUNT(*) FROM points_of_interest WHERE id=?",Long.class,id);
        return n!=null&&n>0;
    }
    public boolean areaExists(Long id) {
        Long n=jdbc.queryForObject("SELECT COUNT(*) FROM campus_areas WHERE id=?",Long.class,id);
        return n!=null&&n>0;
    }
    public void update(Long id,CreatePOIRequestDTO r) {
        jdbc.update("UPDATE points_of_interest SET poi_code=?,poi_name=?,area_id=?,description=?,latitude=?,longitude=?,trigger_radius_m=? WHERE id=?",r.poiCode().trim(),r.poiName().trim(),r.areaId(),r.description(),r.latitude(),r.longitude(),r.triggerRadiusM(),id);
    }
    public void visibility(Long id,boolean value) {
        jdbc.update("UPDATE points_of_interest SET is_visible=? WHERE id=?",value,id);
    }
}
