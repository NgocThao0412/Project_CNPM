
package BUS;

import DAO.AdminDAO;
import DAO.PasswordResetTokenDAO;
import DTO.AdminDTO;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.security.MessageDigest;
import java.util.Optional;

@Service
public class PasswordResetTransactionBUS {

    public enum VerifyResult {
        SUCCESS,
        INVALID,
        ALREADY_VERIFIED
    }

    private final PasswordResetTokenDAO tokenDAO;
    private final AdminDAO adminDAO;
    private final TransactionTemplate transactionTemplate;

    private final PasswordEncoder passwordEncoder;

    public PasswordResetTransactionBUS(
            PasswordResetTokenDAO tokenDAO,
            AdminDAO adminDAO,
            PasswordEncoder passwordEncoder,
            PlatformTransactionManager transactionManager) {

        this.tokenDAO = tokenDAO;
        this.adminDAO = adminDAO;
        this.passwordEncoder = passwordEncoder;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public VerifyResult verifyOtp(
            byte[] tokenHash,
            byte[] submittedOtpHash) {

        return transactionTemplate.execute(status -> {

            var tokenOptional = tokenDAO.findActiveForUpdate(tokenHash);

            if (tokenOptional.isEmpty()) {
                return VerifyResult.INVALID;
            }

            var token = tokenOptional.get();

            if (token.verified()) {
                return VerifyResult.ALREADY_VERIFIED;
            }

            boolean matches = MessageDigest.isEqual(
                    token.otpHash(),
                    submittedOtpHash);

            if (!matches) {
                tokenDAO.increaseFailedAttempts(token.id());
                return VerifyResult.INVALID;
            }

            tokenDAO.markVerified(token.id());
            return VerifyResult.SUCCESS;
        });
    }

    public boolean resetPassword(
            byte[] tokenHash,
            String newPassword) {

        return Boolean.TRUE.equals(
                transactionTemplate.execute(status -> {

                    var tokenOptional = tokenDAO.findActiveForUpdate(tokenHash);

                    if (tokenOptional.isEmpty()) {
                        return false;
                    }

                    var token = tokenOptional.get();

                    // OTP phai duoc xac thuc truoc do.
                    if (!token.verified()) {
                        return false;
                    }

                    var adminOptional = adminDAO.findByIdForUpdate(token.adminId());

                    if (adminOptional.isEmpty()
                            || !adminOptional.get().isActive()) {
                        return false;
                    }

                    // Bam mat khau moi bang BCrypt.
                    String passwordHash = passwordEncoder.encode(newPassword);

                    int updated = adminDAO.updatePassword(
                            token.adminId(),
                            passwordHash);

                    if (updated != 1) {
                        status.setRollbackOnly();
                        return false;
                    }

                    // Danh dau OTP da su dung.
                    int tokenUpdated = tokenDAO.markUsed(token.id());

                    if (tokenUpdated != 1) {
                        status.setRollbackOnly();
                        return false;
                    }
                    return true;
                }));
    }

    // Tao OTP moi trong transaction, co khoa tai khoan Admin.
    public boolean createResetRequest(
            Long adminId,
            byte[] tokenHash,
            byte[] otpHash) {

        return Boolean.TRUE.equals(
                transactionTemplate.execute(status -> {

                    // Khoa ban ghi Admin de ngan yeu cau dong thoi.
                    Optional<AdminDTO> adminOptional = adminDAO.findByIdForUpdate(adminId);

                    if (adminOptional.isEmpty()
                            || !adminOptional.get().isActive()) {
                        return false;
                    }

                    // Gioi han gui OTP: 60 giay/lan.
                    if (tokenDAO.hasRecentRequest(adminId)) {
                        return false;
                    }

                    // Luu OTP moi, hieu luc 5 phut.
                    tokenDAO.create(adminId, tokenHash, otpHash);

                    return true;
                }));
    }

    public void invalidateOldRequestsAfterSend(
            Long adminId,
            byte[] newTokenHash) {

        transactionTemplate.executeWithoutResult(status -> {
            tokenDAO.invalidatePreviousRequestsExcept(
                    adminId,
                    newTokenHash);
        });
    }
}
