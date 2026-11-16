package BUS;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class PasswordResetMailBUS {

    private final JavaMailSender mailSender;
    private final String senderEmail;

    public PasswordResetMailBUS(
            JavaMailSender mailSender,
            @Value("${spring.mail.username}") String senderEmail) {
        this.mailSender = mailSender;
        this.senderEmail = senderEmail;
    }

    public void sendOtp(String recipientEmail, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(senderEmail);
        message.setTo(recipientEmail);
        message.setSubject("Saigon Audio Guide - Khoi phuc mat khau");
        message.setText(
                "Ma OTP khoi phuc mat khau Admin: " + otp
                + "\n\nMa co hieu luc trong 5 phut."
                + "\nNeu ban khong yeu cau, hay bo qua email nay."
        );

        mailSender.send(message);
    }
}