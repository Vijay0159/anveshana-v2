package com.anveshana.search.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendOtpEmail(String toEmail, String fullName, String otp) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Your Anveshana Verification Code");

            String html = """
                    <div style="font-family: Arial, sans-serif; max-width: 480px; margin: auto;
                                border: 1px solid #e5e7eb; border-radius: 12px; overflow: hidden;">
                        <div style="background: #0d6efd; padding: 24px; text-align: center;">
                            <h1 style="color: white; margin: 0; font-size: 1.6rem;">🔍 Anveshana</h1>
                        </div>
                        <div style="padding: 32px;">
                            <p style="font-size: 1rem; color: #374151;">Hi <strong>%s</strong>,</p>
                            <p style="color: #374151;">Use the code below to verify your email address.
                               It expires in <strong>10 minutes</strong>.</p>
                            <div style="text-align: center; margin: 28px 0;">
                                <span style="font-size: 2.4rem; font-weight: 700; letter-spacing: 10px;
                                             color: #0d6efd; background: #eff6ff; padding: 14px 28px;
                                             border-radius: 10px; display: inline-block;">%s</span>
                            </div>
                            <p style="color: #6b7280; font-size: 0.85rem;">
                                If you didn't create an Anveshana account, you can safely ignore this email.
                            </p>
                        </div>
                        <div style="background: #f9fafb; padding: 16px; text-align: center;
                                    color: #9ca3af; font-size: 0.78rem;">
                            © 2026 Anveshana. All rights reserved.
                        </div>
                    </div>
                    """.formatted(fullName, otp);

            helper.setText(html, true);
            mailSender.send(message);

        } catch (Exception e) {
            throw new RuntimeException("Failed to send OTP email: " + e.getMessage(), e);
        }
    }
}
