package Eventos.eventos.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String from;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void enviarHtml(String para, String asunto, String html) throws MessagingException {
        MimeMessage msg = mailSender.createMimeMessage();
        MimeMessageHelper h = new MimeMessageHelper(msg, true, "UTF-8");
        h.setFrom(from);
        h.setTo(para);
        h.setSubject(asunto);
        h.setText(html, true);
        mailSender.send(msg);
    }

    public boolean enviarSeguro(String para, String asunto, String html) {
        try {
            enviarHtml(para, asunto, html);
            return true;
        } catch (Exception e) {
            log.error("No se pudo enviar correo a {}: {}", para, e.getMessage());
            return false;
        }
    }
}