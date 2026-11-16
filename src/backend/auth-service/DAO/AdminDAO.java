package DAO;

import DTO.AdminDTO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class AdminDAO {

        private final JdbcTemplate jdbcTemplate;

        public AdminDAO(JdbcTemplate jdbcTemplate) {
                this.jdbcTemplate = jdbcTemplate;
        }

        private final RowMapper<AdminDTO> adminRowMapper = (rs, rowNum) -> {
                AdminDTO admin = new AdminDTO();

                admin.setId(rs.getLong("id"));
                admin.setUsername(rs.getString("username"));
                admin.setEmail(rs.getString("email"));
                admin.setPasswordHash(rs.getString("password_hash"));
                admin.setFullName(rs.getString("full_name"));
                admin.setAuthVersion(rs.getLong("auth_version"));
                admin.setActive(rs.getBoolean("is_active"));
                admin.setCreatedAt(
                                rs.getTimestamp("created_at").toLocalDateTime());
                admin.setUpdatedAt(
                                rs.getTimestamp("updated_at").toLocalDateTime());

                return admin;
        };

        public Optional<AdminDTO> findByUsername(String username) {
                String sql = """
                                SELECT id, username, email, password_hash, full_name,
                                       auth_version, is_active, created_at, updated_at
                                FROM admins
                                WHERE username = ?
                                """;

                List<AdminDTO> admins = jdbcTemplate.query(
                                sql, adminRowMapper, username);

                return admins.stream().findFirst();
        }

        public Optional<AdminDTO> findById(Long id) {
                String sql = """
                                SELECT id, username, email, password_hash, full_name,
                                       auth_version, is_active, created_at, updated_at
                                FROM admins
                                WHERE id = ?
                                """;

                List<AdminDTO> admins = jdbcTemplate.query(
                                sql, adminRowMapper, id);

                return admins.stream().findFirst();
        }

        public Optional<AdminDTO> findByEmail(String email) {
                String sql = """
                                SELECT id, username, email, password_hash, full_name,
                                       auth_version, is_active, created_at, updated_at
                                FROM admins
                                WHERE email = ? AND is_active = 1
                                """;

                List<AdminDTO> admins = jdbcTemplate.query(
                                sql, adminRowMapper, email);

                return admins.stream().findFirst();
        }

        // Cap nhat mat khau va tang phien ban xac thuc.
        public int updatePassword(Long adminId, String passwordHash) {
                String sql = """
                                UPDATE admins
                                SET password_hash = ?,
                                    auth_version = auth_version + 1,
                                    updated_at = NOW(6)
                                WHERE id = ?
                                  AND is_active = 1
                                """;

                return jdbcTemplate.update(sql, passwordHash, adminId);
        }

        // Lay tai khoan Admin va khoa ban ghi trong transaction.
        public Optional<AdminDTO> findByIdForUpdate(Long id) {
                String sql = """
                                SELECT id, username, email, password_hash, full_name,
                                       auth_version, is_active, created_at, updated_at
                                FROM admins
                                WHERE id = ?
                                FOR UPDATE
                                """;

                List<AdminDTO> admins = jdbcTemplate.query(
                                sql, adminRowMapper, id);

                return admins.stream().findFirst();
        }

}