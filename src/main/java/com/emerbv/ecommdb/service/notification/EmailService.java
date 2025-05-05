package com.emerbv.ecommdb.service.notification;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.concurrent.CompletableFuture;

/**
 * Servicio para envío de notificaciones vía correo electrónico
 */
@Service
@RequiredArgsConstructor
public class EmailService {
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender emailSender;

    @Value("${notification.email.from}")
    private String emailFrom;

    @Value("${notification.email.enabled:true}")
    private boolean emailEnabled;

    @Value("${notification.email.reply-to:support@emerbv-ecommerce.com}")
    private String replyToEmail;

    /**
     * Envía un correo electrónico
     * @param to Dirección de correo del destinatario
     * @param subject Asunto del correo
     * @param htmlContent Contenido HTML del correo
     */
    public void sendEmail(String to, String subject, String htmlContent) {
        if (!emailEnabled) {
            logger.info("Envío de email desactivado: {}, {}", to, subject);
            return;
        }

        try {
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(emailFrom);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            helper.setReplyTo(replyToEmail);

            CompletableFuture.runAsync(() -> {
                try {
                    emailSender.send(message);
                    logger.info("Email enviado correctamente a: {}", to);
                } catch (Exception e) {
                    logger.error("Error enviando email a {}: {}", to, e.getMessage(), e);
                }
            });

        } catch (MessagingException e) {
            logger.error("Error preparando email para {}: {}", to, e.getMessage(), e);
            throw new RuntimeException("Error al enviar email", e);
        }
    }

    /**
     * Envía un correo con copia oculta (BCC)
     * @param to Destinatario principal
     * @param bcc Lista de destinatarios en copia oculta
     * @param subject Asunto
     * @param htmlContent Contenido HTML
     */
    public void sendEmailWithBcc(String to, String[] bcc, String subject, String htmlContent) {
        if (!emailEnabled) {
            logger.info("Envío de email desactivado: {}, {}", to, subject);
            return;
        }

        try {
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(emailFrom);
            helper.setTo(to);
            helper.setBcc(bcc);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            helper.setReplyTo(replyToEmail);

            CompletableFuture.runAsync(() -> {
                try {
                    emailSender.send(message);
                    logger.info("Email con copia oculta enviado correctamente");
                } catch (Exception e) {
                    logger.error("Error enviando email con copia oculta: {}", e.getMessage(), e);
                }
            });

        } catch (MessagingException e) {
            logger.error("Error preparando email con copia oculta: {}", e.getMessage(), e);
            throw new RuntimeException("Error al enviar email con copia oculta", e);
        }
    }
}
