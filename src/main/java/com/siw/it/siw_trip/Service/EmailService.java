package com.siw.it.siw_trip.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.net.InetAddress;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;
    
    @Autowired
    private TemplateEngine templateEngine;

    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        try {
            // Test network connectivity first
            System.out.println("Testing network connectivity...");
            InetAddress address = InetAddress.getByName("1.179.115.1");
            System.out.println("Successfully resolved IP: " + address.getHostAddress());
            
            // Create MimeMessage for HTML email
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(toEmail);
            helper.setSubject("Password Reset Request - SiW Books");
            helper.setFrom("ismar.costabile@icloud.com");
            
            // Prepare Thymeleaf context
            Context context = new Context();
            String resetUrl = "http://localhost:8080/reset-password?token=" + resetToken;
            context.setVariable("resetUrl", resetUrl);
            
            // Process the HTML template
            String htmlContent = templateEngine.process("email/password-reset", context);
            helper.setText(htmlContent, true); // true indicates HTML content
            
            System.out.println("Attempting to send HTML email via JavaMailSender...");
            mailSender.send(message);
            System.out.println("Password reset email sent successfully to: " + toEmail);
        } catch (Exception e) {
            System.err.println("Failed to send email to " + toEmail + ": " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to send password reset email", e);
        }
    }
}
