
package DTO;

public record ResetPasswordRequestDTO(
        String requestToken,
        String newPassword,
        String confirmPassword) {
}
