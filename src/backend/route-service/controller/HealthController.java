package controller;

import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {
    private final JdbcTemplate jdbc;
    public HealthController(JdbcTemplate jdbc) {this.jdbc=jdbc;}

    @GetMapping("/api/health")
    public Map<String,Object> status() {
        Integer routes=jdbc.queryForObject("SELECT COUNT(*) FROM tour_routes",Integer.class);
        Integer stops=jdbc.queryForObject("SELECT COUNT(*) FROM tour_route_pois",Integer.class);
        return Map.of("service","route-service","status","UP","routes",routes,"stops",stops);
    }
}
