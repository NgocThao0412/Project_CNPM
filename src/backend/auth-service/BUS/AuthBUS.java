package BUS;

import DAO.AdminDAO;
import DTO.AdminDTO;
import DTO.LoginRequestDTO;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import DAO.PasswordResetTokenDAO;
import org.springframework.beans.factory.annotation.Value;
import java.util.Locale;

@Service
public class AuthBUS {

        private final AdminDAO adminDAO;
        private final PasswordEncoder passwordEncoder;
        private final PasswordResetTokenDAO tokenDAO;
        private final PasswordResetMailBUS mailBUS;
        private final PasswordResetTransactionBUS resetTransactionBUS;
        private final String passwordResetSecret;

        public AuthBUS(
                        AdminDAO adminDAO,
                        PasswordEncoder passwordEncoder,
                        PasswordResetTokenDAO tokenDAO,
                        PasswordResetMailBUS mailBUS,
                        PasswordResetTransactionBUS resetTransactionBUS,
                        @Value("${sgu.password-reset.secret}") String passwordResetSecret) {

                this.adminDAO = adminDAO;
                this.passwordEncoder = passwordEncoder;
                this.tokenDAO = tokenDAO;
                this.mailBUS = mailBUS;
                this.resetTransactionBUS = resetTransactionBUS;
                this.passwordResetSecret = passwordResetSecret;
        }

        public AdminDTO getAuthenticatedAdmin(Long adminId, long authVersion) {
                AdminDTO admin = adminDAO.findById(adminId)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Phiên đăng nhập không hợp lệ"));

                if (!admin.isActive() || admin.getAuthVersion() != authVersion) {
                        throw new IllegalArgumentException(
                                        "Phiên đăng nhập không hợp lệ");
                }

                return admin;
        }

        public AdminDTO authenticate(LoginRequestDTO request) {
                if (request == null
                                || request.getUsername() == null
                                || request.getUsername().isBlank()
                                || request.getPassword() == null
                                || request.getPassword().isBlank()) {
                        throw new IllegalArgumentException(
                                        "Vui lòng nhập tên đăng nhập và mật khẩu");
                }

                AdminDTO admin = adminDAO
                                .findByUsername(request.getUsername().trim())
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Thông tin đăng nhập không hợp lệ"));

                boolean passwordMatches = passwordEncoder.matches(
                                request.getPassword(),
                                admin.getPasswordHash());

                if (!passwordMatches || !admin.isActive()) {
                        throw new IllegalArgumentException(
                                        "Thông tin đăng nhập không hợp lệ");
                }

                return admin;
        }

        // Tao yeu cau khoi phuc mat khau Admin.
        public String requestPasswordReset(String email) {
                if (email == null || email.isBlank()) {
                        throw new IllegalArgumentException(
                                        "Vui lòng nhập địa chỉ email");
                }

                String normalizedEmail = email.trim()
                                .toLowerCase(Locale.ROOT);

                // Tao request token ngau nhien.
                String requestToken = PasswordResetCrypto.generateRequestToken();

                var adminOptional = adminDAO.findByEmail(normalizedEmail);

                // Khong tiet lo email co ton tai trong he thong hay khong.
                if (adminOptional.isEmpty()) {
                        return requestToken;
                }

                AdminDTO admin = adminOptional.get();

                // Tao ma OTP gom 6 chu so.
                String otp = PasswordResetCrypto.generateOtp();

                // Bam request token va OTP truoc khi luu.
                byte[] tokenHash = PasswordResetCrypto.hashToken(requestToken);

                byte[] otpHash = PasswordResetCrypto.hashOtp(
                                requestToken,
                                otp,
                                passwordResetSecret);

                // Tao yeu cau trong transaction, gioi han 60 giay.
                boolean created = resetTransactionBUS.createResetRequest(
                                admin.getId(),
                                tokenHash,
                                otpHash);

                if (!created) {
                        // Khong tiet lo trang thai tai khoan.
                        return requestToken;
                }

                // Gui OTP den Gmail da dang ky.
                try {
                        mailBUS.sendOtp(admin.getEmail(), otp);
                } catch (org.springframework.mail.MailException exception) {
                        // Xoa yeu cau vua tao neu SMTP bao loi.
                        tokenDAO.deleteFailedRequest(admin.getId(), tokenHash);

                        throw new IllegalStateException(
                                        "Không thể gửi OTP lúc này", exception);
                }
                resetTransactionBUS.invalidateOldRequestsAfterSend(admin.getId(), tokenHash);
                return requestToken;

        }

        // Xac thuc OTP khoi phuc mat khau.
        public PasswordResetTransactionBUS.VerifyResult verifyPasswordResetOtp(
                        String requestToken,
                        String otp) {

                if (requestToken == null || requestToken.isBlank()
                                || otp == null || !otp.matches("\\d{6}")) {
                        throw new IllegalArgumentException(
                                        "Mã OTP hoặc yêu cầu khôi phục không hợp lệ");
                }

                byte[] tokenHash = PasswordResetCrypto.hashToken(requestToken);

                byte[] otpHash = PasswordResetCrypto.hashOtp(
                                requestToken,
                                otp,
                                passwordResetSecret);

                return resetTransactionBUS.verifyOtp(
                                tokenHash,
                                otpHash);
        }

        // Dat lai mat khau sau khi OTP da xac thuc.
        public void resetPassword(
                        String requestToken,
                        String newPassword,
                        String confirmPassword) {

                if (requestToken == null || requestToken.isBlank()) {
                        throw new IllegalArgumentException(
                                        "Yêu cầu khôi phục mật khẩu không hợp lệ");
                }

                if (newPassword == null
                                || newPassword.length() < 8
                                || newPassword.length() > 72) {
                        throw new IllegalArgumentException(
                                        "Mật khẩu mới phải có từ 8 đến 72 ký tự");
                }

                if (!newPassword.equals(confirmPassword)) {
                        throw new IllegalArgumentException(
                                        "Mật khẩu xác nhận không khớp");
                }

                byte[] tokenHash = PasswordResetCrypto.hashToken(requestToken);

                boolean success = resetTransactionBUS.resetPassword(
                                tokenHash,
                                newPassword);

                if (!success) {
                        throw new IllegalArgumentException(
                                        "Yêu cầu khôi phục đã hết hạn, chưa xác thực hoặc đã sử dụng");
                }
        }
}