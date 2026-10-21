package com.sgu.audioguide.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;

@Configuration
@Profile("dev")
@ConditionalOnProperty(
        name = "app.bootstrap-admin.enabled",
        havingValue = "true"
)
public class AdminBootstrapConfig {

    @Bean
    public CommandLineRunner createDemoAdmin(
            JdbcTemplate jdbcTemplate,
            PasswordEncoder passwordEncoder,
            Environment environment
    ) {
        return args -> {
            String username = "admin";

            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM admins WHERE username = ?",
                    Integer.class,
                    username
            );

            if (count != null && count > 0) {
                System.out.println(
                        "Tai khoan admin da ton tai, giu nguyen du lieu."
                );
                return;
            }

            String password = environment.getProperty(
                    "SGU_ADMIN_PASSWORD"
            );

            if (password == null || password.isBlank()
                    || password.length() < 6
                    || password.getBytes(StandardCharsets.UTF_8).length > 72) {
                throw new IllegalStateException(
                        "SGU_ADMIN_PASSWORD can tu 6 ky tu va toi da 72 byte."
                );
            }

            String sql = """
                    INSERT INTO admins
                        (username, email, password_hash, full_name,
                         auth_version, is_active)
                    VALUES (?, ?, ?, ?, 0, TRUE)
                    """;

            jdbcTemplate.update(
                    sql,
                    username,
                    "admin@example.com",
                    passwordEncoder.encode(password),
                    "Admin Demo"
            );

            System.out.println("Da tao tai khoan admin thu nghiem.");
        };
    }
}