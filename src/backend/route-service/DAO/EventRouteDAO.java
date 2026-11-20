package DAO;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Reads and writes only tour_routes and tour_route_pois (plus POI lookups). */
@Repository
public class EventRouteDAO {
    private final JdbcTemplate jdbc;
    public EventRouteDAO(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<Map<String,Object>> all(boolean includeDrafts) {
        String sql = "SELECT r.id,r.route_code AS routeCode,r.route_name AS title,"
            + "r.description AS summary,r.audience,r.is_visible AS published,r.is_template AS template,"
            + "(SELECT COUNT(*) FROM tour_route_pois s WHERE s.route_id=r.id) AS stopCount "
            + "FROM tour_routes r " + (includeDrafts?"":"WHERE r.is_visible=1 AND r.is_template=0 ")
            + "ORDER BY r.id";
        return jdbc.queryForList(sql);
    }

    public Map<String,Object> one(long id, boolean includeDrafts) {
        List<Map<String,Object>> rows=jdbc.queryForList(
            "SELECT id,route_code AS routeCode,route_name AS title,description AS summary,"
            + "audience,is_visible AS published,is_template AS template "
            + "FROM tour_routes WHERE id=? " + (includeDrafts?"":"AND is_visible=1 AND is_template=0"),id);
        if(rows.isEmpty())return null;
        Map<String,Object> route=new LinkedHashMap<>(rows.getFirst());
        String allowed=includeDrafts?"1=1":"p.is_visible=1 AND a.is_visible=1";
        String sql="SELECT s.id,s.sort_order AS sortOrder,"
            + "COALESCE(NULLIF(s.title,''),p.poi_name,'Bước tham quan') AS title,"
            + "COALESCE(NULLIF(s.instructions,''),p.description,'') AS instructions,"
            + "COALESCE(NULLIF(s.location_label,''),p.poi_name,'') AS locationLabel,"
            + "s.duration_minutes AS durationMinutes,"
            + "CASE WHEN "+allowed+" THEN p.id ELSE NULL END AS poiId,"
            + "CASE WHEN "+allowed+" THEN p.poi_name ELSE NULL END AS poiName,"
            + "CASE WHEN "+allowed+" THEN p.latitude ELSE NULL END AS latitude,"
            + "CASE WHEN "+allowed+" THEN p.longitude ELSE NULL END AS longitude "
            + "FROM tour_route_pois s LEFT JOIN points_of_interest p ON p.id=s.poi_id "
            + "LEFT JOIN campus_areas a ON a.id=p.area_id "
            + "WHERE s.route_id=? ORDER BY s.sort_order";
        route.put("stops",jdbc.queryForList(sql,id));
        return route;
    }

    public boolean poiExists(long id) {
        Integer count=jdbc.queryForObject("SELECT COUNT(*) FROM points_of_interest WHERE id=?",Integer.class,id);
        return count!=null && count>0;
    }

    @Transactional
    public long save(long id,Map<String,Object> route,List<Map<String,Object>> steps) {
        String code=(String)route.get("routeCode");
        String title=(String)route.get("title");
        String summary=(String)route.get("summary");
        String audience=(String)route.get("audience");
        boolean published=Boolean.TRUE.equals(route.get("published"));
        boolean template=Boolean.TRUE.equals(route.get("template"));
        int minutes=0;
        for(Map<String,Object> step:steps) minutes+=((Number)step.get("durationMinutes")).intValue();
        minutes=Math.max(1,minutes);

        long routeId=id;
        if(id==0) {
            final int estimate=minutes;
            KeyHolder keys=new GeneratedKeyHolder();
            jdbc.update(con->{PreparedStatement st=con.prepareStatement(
                "INSERT INTO tour_routes(route_code,route_name,description,estimated_duration_minutes,is_visible,audience,is_template) VALUES(?,?,?,?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS);
                st.setString(1,code);st.setString(2,title);st.setString(3,summary);
                st.setInt(4,estimate);st.setBoolean(5,published);st.setString(6,audience);st.setBoolean(7,template);
                return st;
            },keys);
            routeId=Objects.requireNonNull(keys.getKey()).longValue();
        } else {
            int changed=jdbc.update("UPDATE tour_routes SET route_code=?,route_name=?,description=?,estimated_duration_minutes=?,is_visible=?,audience=?,is_template=? WHERE id=?",
                code,title,summary,minutes,published,audience,template,id);
            if(changed==0)throw new NoSuchElementException("Không tìm thấy tuyến đường.");
            jdbc.update("DELETE FROM tour_route_pois WHERE route_id=?",id);
        }
        int order=0;
        for(Map<String,Object> stop:steps) {
            jdbc.update("INSERT INTO tour_route_pois(route_id,sort_order,title,instructions,location_label,poi_id,duration_minutes) VALUES(?,?,?,?,?,?,?)",
                routeId,++order,stop.get("title"),stop.get("instructions"),stop.get("locationLabel"),stop.get("poiId"),stop.get("durationMinutes"));
        }
        return routeId;
    }

    @Transactional
    public void setPublished(long id,boolean published) {
        jdbc.update("UPDATE tour_routes SET is_visible=?, is_template=CASE WHEN ? THEN 0 ELSE is_template END WHERE id=?",published,published,id);
    }
}
