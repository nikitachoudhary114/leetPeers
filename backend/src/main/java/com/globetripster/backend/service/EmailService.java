package com.globetripster.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    @Autowired
    private JavaMailSender mailSender;

    public void sendResetEmail(String toEmail, String resetLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("LeetPeers - Password Reset");
        message.setText("Click the link to reset your password: " + resetLink);
        message.setFrom("your-email@gmail.com");

        mailSender.send(message);
    }

    public void sendOtpEmail(String toEmail, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("LeetPeers - Your OTP Code");
        message.setText("Your OTP is: " + otp + "\n\nIt will expire in 5 minutes.\n\nDo not share this with anyone.");
        message.setFrom("your-email@gmail.com");

        mailSender.send(message);
    }
}
