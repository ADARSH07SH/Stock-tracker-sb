package com.ash.auth_service.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendForgotPasswordOtp(String toEmail, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Your OTP for Password Reset");
        message.setText(
                "Your OTP for resetting your password is: " + otp +
                        "\n\nThis OTP is valid for 5 minutes." +
                        "\nDo not share this OTP with anyone."
        );
        mailSender.send(message);
    }

    public void sendVerificationEmail(String toEmail, String userName) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Welcome to Stock Tracker - Verify Your Email");
        message.setText(
                "Hello " + (userName != null ? userName : "User") + ",\n\n" +
                        "Thank you for registering with Stock Tracker!\n\n" +
                        "Your account has been created successfully. You can start using the app right away.\n\n" +
                        "For enhanced security and to unlock all features, we recommend verifying your email address.\n\n" +
                        "Note: Email verification will be available in the app settings soon.\n\n" +
                        "Happy tracking!\n" +
                        "The Stock Tracker Team"
        );
        mailSender.send(message);
    }
}
