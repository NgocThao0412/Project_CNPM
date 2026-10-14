package com.sgu.audioguide.controller;

import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DatabaseHealthController {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseHealthController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/api/health/database")
    public Map<String, Object> checkDatabase() {
        String database = jdbcTemplate.queryForObject(
            "SELECT DATABASE()",
            String.class
        );

        Integer tableCount = jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM information_schema.tables
            WHERE table_schema = DATABASE()
              AND table_type = 'BASE TABLE'
            """,
            Integer.class
        );

        return Map.of(
            "status", "OK",
            "database", database,
            "tableCount", tableCount
        );
    }
}