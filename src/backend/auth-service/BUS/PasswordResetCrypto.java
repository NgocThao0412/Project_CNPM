
package BUS;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class PasswordResetCrypto {

    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordResetCrypto() {
    }

    // Tao request token ngau nhien 256 bit.
    public static String generateRequestToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    // Tao OTP gom 6 chu so.
    public static String generateOtp() {
        int number = RANDOM.nextInt(1_000_000);
        return String.format(java.util.Locale.ROOT, "%06d", number);
    }

    // Bam request token bang SHA-256.
    public static byte[] hashToken(String requestToken) {
        try {
            byte[] tokenBytes = Base64.getUrlDecoder()
                    .decode(requestToken);

            if (tokenBytes.length != 32) {
                throw new IllegalArgumentException(
                        "Request token khong hop le");
            }

            return MessageDigest.getInstance("SHA-256")
                    .digest(tokenBytes);

        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Request token khong hop le");
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Khong the bam request token", exception);
        }
    }

    // Bam OTP bang HMAC-SHA256, gan voi request token.
    public static byte[] hashOtp(
            String requestToken,
            String otp,
            String secret) {

        try {
            Mac mac = Mac.getInstance("HmacSHA256");

            SecretKeySpec key = new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256");

            mac.init(key);

            String data = requestToken + ":" + otp;

            return mac.doFinal(
                    data.getBytes(StandardCharsets.UTF_8));

        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Khong the bam OTP", exception);
        }
    }
}
