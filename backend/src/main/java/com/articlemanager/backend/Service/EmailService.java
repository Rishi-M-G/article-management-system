package com.articlemanager.backend.Service;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.articlemanager.backend.entity.User;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Value("${spring.mail.from:noreply@articlemanager.com")
    private String fromEmail;

    public void sendFollowNotification(User follower, User followingUser) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

            String subject = follower.getFirstName() + "has followed you !";
            String htmlContent = buildFollowNotificationHtml(follower, followingUser);

            helper.setFrom(fromEmail);
            helper.setTo(followingUser.getEmail());
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);
            log.info("email.follow_notification.sent to={} from={}", followingUser.getEmail(), follower.getFirstName());
        } catch (MessagingException ex) {
            log.error("email.follow_notification.failed to={} error={}", followingUser.getEmail(), ex.getMessage());
            throw new RuntimeException("Failed to send follow notification email", ex);
        }
    }

    private String buildFollowNotificationHtml(User follower, User followingUser) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<body style='font-family: Arial, sans-serif; background-color: #f5f5f5; padding: 20px;'>" +
                "<div style='background-color: white; padding: 30px; border-radius: 8px; max-width: 500px; margin: 0 auto;'>"
                +
                "<h2 style='color: #333;'>New Follower!</h2>" +
                "<p style='color: #666; font-size: 16px;'>" +
                follower.getFirstName() + " " + follower.getLastName() + " has followed you!" +
                "</p>" +
                "<p style='color: #999; font-size: 14px; margin-top: 20px;'>" +
                "This is a notification from Article Manager." +
                "</p>" +
                "</div>" +
                "</body>" +
                "</html>";
    }
}
