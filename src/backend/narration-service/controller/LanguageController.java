package controller;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LanguageController {

    private final JdbcTemplate jdbcTemplate;

    public LanguageController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/api/languages")
    public List<LanguageResponse> getActiveLanguages() {
        String sql = """
                SELECT id, language_code, language_name
                FROM languages
                WHERE is_active = 1
                ORDER BY id
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) ->
                new LanguageResponse(
                        rs.getInt("id"),
                        rs.getString("language_code"),
                        rs.getString("language_name")
                )
        );
    }

    public record LanguageResponse(
            int id,
            String languageCode,
            String languageName
    ) {
    }
}