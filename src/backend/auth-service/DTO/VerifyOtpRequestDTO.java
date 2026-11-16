package DTO;

public record VerifyOtpRequestDTO(
        String requestToken,
        String otp) {
}
