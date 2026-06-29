package jobforsite.sitejob.module.auth.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class EmailService {
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final JavaMailSender moderationMailSender;
    private final TemplateEngine templateEngine;
    private final String fromEmail;
    private final String moderationFromEmail;
    private final String baseUrl;

    @Value("${app.moderator.emails:}")
    private String moderatorEmails;

    public EmailService(
            @Qualifier("mailSender") JavaMailSender mailSender,
            @Qualifier("moderationMailSender") JavaMailSender moderationMailSender,
            TemplateEngine templateEngine,
            @Value("${app.email.from}") String fromEmail,
            @Value("${app.email.moderation.from}") String moderationFromEmail,
            @Value("${app.base-url}") String baseUrl
    ) {
        this.mailSender = mailSender;
        this.moderationMailSender = moderationMailSender;
        this.templateEngine = templateEngine;
        this.fromEmail = fromEmail;
        this.moderationFromEmail = moderationFromEmail;
        this.baseUrl = baseUrl;
        logger.info("EmailService инициализирован. From: {}, ModerationFrom: {}, BaseUrl: {}",
                fromEmail, moderationFromEmail, baseUrl);
    }

    private void sendHtmlMessageInternal(JavaMailSender sender, String from, String to, String subject, String templateName, Context context) {
        try {
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);

            String htmlContent = templateEngine.process(templateName, context);
            helper.setText(htmlContent, true);

            sender.send(message);
            logger.info("Письмо отправлено с {} на {}", from, to);
        } catch (MessagingException e) {
            logger.warn("Ошибка отправки письма");
            throw new RuntimeException("Failed to send email", e);
        }
    }

    public void sendSimpleMessage(String to, String subject, String text) {
        logger.debug("Отправка простого сообщения на адрес: {}", to);
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
            logger.info("Простое сообщение успешно отправлено на {}", to);
        } catch (Exception e) {
            logger.warn("Ошибка отправки простого сообщения");
            throw new RuntimeException("Failed to send simple email", e);
        }
    }

    public void sendHtmlMessage(String to, String subject, String templateName, Context context) {
        sendHtmlMessageInternal(mailSender, fromEmail, to, subject, templateName, context);
    }

    public void sendHtmlMessageForModeration(String to, String subject, String templateName, Context context) {
        sendHtmlMessageInternal(moderationMailSender, moderationFromEmail, to, subject, templateName, context);
    }

    public void sendRegistrationCode(String email, String code) {
        logger.info("Отправка кода регистрации на email: {}", email);
        Context context = new Context(Locale.getDefault());
        context.setVariable("code", code);
        context.setVariable("baseUrl", baseUrl);

        sendHtmlMessage(email, "Код подтверждения регистрации", "email/registration-code", context);
    }

    public void sendPasswordResetCode(String email, String code) {
        logger.info("Отправка кода восстановления пароля на email: {}", email);
        Context context = new Context(Locale.getDefault());
        context.setVariable("code", code);
        context.setVariable("baseUrl", baseUrl);

        sendHtmlMessage(email, "Восстановление пароля", "email/password-reset-code", context);
    }

    public void sendModerationNotification(
            String postType,
            String title,
            String nameUse,
            String location,
            LocalDateTime createdAt) {

        if (moderatorEmails == null || moderatorEmails.trim().isEmpty()) {
            logger.warn("Email-адреса модераторов не настроены");
            return;
        }

        String[] recipients = moderatorEmails.split(",");
        String typeDisplayName = getTypeDisplayName(postType);

        logger.info("Отправка уведомления модераторам: тип={}, заголовок='{}'",
                postType, title);

        try {
            Context context = new Context(Locale.getDefault());
            context.setVariable("postType", typeDisplayName);
            context.setVariable("postTypeCode", postType);
            context.setVariable("title", title != null ? title : "Без названия");
            context.setVariable("nameUse", nameUse != null ? nameUse : "Неизвестный");
            context.setVariable("location", location != null ? location : "Не указана");
            context.setVariable("baseUrl", baseUrl);
            context.setVariable("moderationUrl", baseUrl + "/page_two/moderat");
            context.setVariable("createdAt", formatDate(createdAt));
            context.setVariable("currentYear", LocalDateTime.now().getYear());

            sendHtmlMessageForModeration(
                    String.join(",", recipients),
                    "Новое объявление на модерацию: " + title,
                    "email/moderation-notification",
                    context
            );

            logger.info("Уведомление о модерации отправлено");

        } catch (Exception e) {
            logger.warn("Ошибка отправки уведомления модераторам");
        }
    }

    private String getTypeDisplayName(String type) {
        switch (type.toLowerCase()) {
            case "product": return "Товар";
            case "service": return "Услуга";
            case "vacancy": return "Вакансия";
            case "resume": return "Резюме";
            default: return "Объявление";
        }
    }

    private String formatDate(LocalDateTime date) {
        if (date == null) return "Не указана";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
        return date.format(formatter);
    }
}