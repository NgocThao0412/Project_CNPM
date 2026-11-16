
package DAO;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public class PasswordResetTokenDAO {

        private final JdbcTemplate jdbcTemplate;

        public PasswordResetTokenDAO(JdbcTemplate jdbcTemplate) {
                this.jdbcTemplate = jdbcTemplate;
        }

        // Luu OTP da bam, thoi han 5 phut.
        public void create(
                        Long adminId,
                        byte[] tokenHash,
                        byte[] otpHash) {

                String sql = """
                                INSERT INTO password_reset_tokens
                                    (admin_id, token_hash, otp_hash,
                                     failed_attempts, created_at, expires_at)
                                VALUES
                                    (?, ?, ?, 0, NOW(6),
                                     DATE_ADD(NOW(6), INTERVAL 5 MINUTE))
                                """;

                jdbcTemplate.update(
                                sql,
                                adminId,
                                tokenHash,
                                otpHash);
        }

        public record ResetToken(
                        Long id,
                        Long adminId,
                        byte[] otpHash,
                        int failedAttempts,
                        boolean verified) {
        }

        // Tim OTP con han, chua su dung va chua qua 5 lan nhap sai.
        // Ham nay duoc goi trong transaction de khoa ban ghi.
        public Optional<ResetToken> findActiveForUpdate(byte[] tokenHash) {
                String sql = """
                                SELECT id, admin_id, otp_hash,
                                       failed_attempts, verified_at
                                FROM password_reset_tokens
                                WHERE token_hash = ?
                                  AND expires_at > NOW(6)
                                  AND used_at IS NULL
                                  AND failed_attempts < 5
                                FOR UPDATE
                                """;

                List<ResetToken> tokens = jdbcTemplate.query(
                                sql,
                                (rs, rowNum) -> new ResetToken(
                                                rs.getLong("id"),
                                                rs.getLong("admin_id"),
                                                rs.getBytes("otp_hash"),
                                                rs.getInt("failed_attempts"),
                                                rs.getTimestamp("verified_at") != null),
                                tokenHash);

                return tokens.stream().findFirst();
        }

        // Tang so lan nhap OTP sai.
        public void increaseFailedAttempts(Long tokenId) {
                String sql = """
                                UPDATE password_reset_tokens
                                SET failed_attempts = failed_attempts + 1
                                WHERE id = ?
                                  AND failed_attempts < 5
                                """;

                jdbcTemplate.update(sql, tokenId);
        }

        // Danh dau OTP da duoc xac thuc thanh cong.
        public void markVerified(Long tokenId) {
                String sql = """
                                UPDATE password_reset_tokens
                                SET verified_at = NOW(6)
                                WHERE id = ?
                                  AND verified_at IS NULL
                                  AND used_at IS NULL
                                  AND expires_at > NOW(6)
                                """;

                jdbcTemplate.update(sql, tokenId);
        }

        // Danh dau OTP da duoc su dung sau khi doi mat khau.
        public int markUsed(Long tokenId) {
                String sql = """
                                UPDATE password_reset_tokens
                                SET used_at = NOW(6)
                                WHERE id = ?
                                  AND verified_at IS NOT NULL
                                  AND used_at IS NULL
                                  AND expires_at > NOW(6)
                                """;

                return jdbcTemplate.update(sql, tokenId);
        }

        // Vo hieu hoa cac yeu cau khoi phuc cu.
        public int invalidatePreviousRequests(Long adminId) {
                String sql = """
                                DELETE FROM password_reset_tokens
                                WHERE admin_id = ?
                                  AND used_at IS NULL
                                """;

                return jdbcTemplate.update(sql, adminId);
        }

        // Kiem tra tai khoan da yeu cau OTP trong 60 giay gan nhat.
        public boolean hasRecentRequest(Long adminId) {
                String sql = """
                                SELECT COUNT(*)
                                FROM password_reset_tokens
                                WHERE admin_id = ?
                                  AND created_at > DATE_SUB(
                                      NOW(6), INTERVAL 60 SECOND
                                  )
                                """;

                Integer count = jdbcTemplate.queryForObject(
                                sql,
                                Integer.class,
                                adminId);

                return count != null && count > 0;
        }

        // Xoa yeu cau OTP khi gui email that bai.
        public int deleteFailedRequest(Long adminId, byte[] tokenHash) {
                String sql = """
                                DELETE FROM password_reset_tokens
                                WHERE admin_id = ?
                                  AND token_hash = ?
                                  AND verified_at IS NULL
                                  AND used_at IS NULL
                                """;

                return jdbcTemplate.update(sql, adminId, tokenHash);
        }

        public int invalidatePreviousRequestsExcept(
                        Long adminId,
                        byte[] newTokenHash) {

                String sql = """
                                DELETE FROM password_reset_tokens
                                WHERE admin_id = ?
                                  AND token_hash <> ?
                                  AND used_at IS NULL
                                """;

                return jdbcTemplate.update(
                                sql,
                                adminId,
                                newTokenHash);
        }
}
