package DAO;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class EventRouteDAO {
    private final JdbcTemplate jdbc;
    public EventRouteDAO(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<Map<String,Object>> all(boolean includeDrafts) {
        String sql = "SELECT r.id,r.route_code AS routeCode,r.title,r.summary,r.audience,"
            + "r.is_published AS published,r.is_template AS template,"
            + "(SELECT COUNT(*) FROM campus_event_stops s WHERE s.route_id=r.id) AS stopCount "
            + "FROM campus_event_routes r " + (includeDrafts?"":"WHERE r.is_published=1 ")
            + "ORDER BY r.id";
        return jdbc.queryForList(sql);
    }
    public Map<String,Object> one(long id, boolean includeDrafts) {
        List<Map<String,Object>> rows=jdbc.queryForList(
            "SELECT id,route_code AS routeCode,title,summary,audience,is_published AS published,"
            + "is_template AS template FROM campus_event_routes WHERE id=? "
            + (includeDrafts?"":"AND is_published=1"), id);
        if(rows.isEmpty())return null;
        Map<String,Object> route=new LinkedHashMap<>(rows.getFirst());
        String allowed=includeDrafts?"1=1":"p.is_visible=1 AND a.is_visible=1";
        String sql="SELECT s.id,s.sort_order AS sortOrder,s.title,s.instructions,"
           + "s.location_label AS locationLabel,s.duration_minutes AS durationMinutes,"
           + "CASE WHEN "+allowed+" THEN p.id ELSE NULL END AS poiId,"
           + "CASE WHEN "+allowed+" THEN p.poi_name ELSE NULL END AS poiName "
           + "FROM campus_event_stops s LEFT JOIN points_of_interest p ON p.id=s.poi_id "
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
        if(id==0) {
            KeyHolder keys=new GeneratedKeyHolder();
            jdbc.update(con->{PreparedStatement st=con.prepareStatement(
              "INSERT INTO campus_event_routes(route_code,title,summary,audience,is_published,is_template) VALUES(?,?,?,?,?,?)",
              Statement.RETURN_GENERATED_KEYS);
              st.setString(1,code);st.setString(2,title);st.setString(3,summary);
              st.setString(4,audience);st.setBoolean(5,published);st.setBoolean(6,template);
              return st;},keys);
            id=Objects.requireNonNull(keys.getKey()).longValue();
        } else {
            int changed=jdbc.update("UPDATE campus_event_routes SET route_code=?,title=?,summary=?,audience=?,is_published=?,is_template=? WHERE id=?",
                code,title,summary,audience,published,template,id);
            if(changed==0)throw new NoSuchElementException("Không tìm thấy tuyến đường.");
            jdbc.update("DELETE FROM campus_event_stops WHERE route_id=?",id);
        }
        int order=0;
        for(var stop:steps) {
            order++;
            jdbc.update("INSERT INTO campus_event_stops(route_id,sort_order,title,instructions,location_label,poi_id,duration_minutes) VALUES(?,?,?,?,?,?,?)",
                id,order,stop.get("title"),stop.get("instructions"),stop.get("locationLabel"),stop.get("poiId"),stop.get("durationMinutes"));
        }
        return id;
    }
    public void delete(long id) { jdbc.update("DELETE FROM campus_event_routes WHERE id=?",id); }
}
