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

    public void sendForgotPasswordOtp(String toEmail,String otp){
        SimpleMailMessage message=new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Your OTP for Password Reset");
        message.setText(
                "Your OTP for resetting your password is: " + otp +
                        "\n\nThis OTP is valid for 5 minutes." +
                        "\nDo not share this OTP with anyone."
        );
        mailSender.send(message);
    }
}
