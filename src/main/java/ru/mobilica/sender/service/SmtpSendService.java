package ru.mobilica.sender.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.*;
import org.springframework.stereotype.Service;

@Service
public class SmtpSendService {
    private final JavaMailSender mailSender;

    public SmtpSendService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendPlainText(String from, String replyTo, String to, String subject, String text) throws MessagingException {
        MimeMessage msg = mailSender.createMimeMessage();
        MimeMessageHelper h = new MimeMessageHelper(msg, "UTF-8");
        msg.addHeader("List-Unsubscribe", "<mailto:hello@mobilica.ru?subject=unsubscribe>");
        msg.setHeader("Message-ID", "<" + java.util.UUID.randomUUID() + "@mobilica.ru>");
        h.setFrom(from);
        if (replyTo != null && !replyTo.isBlank()) h.setReplyTo(replyTo);
        h.setTo(to);
        h.setSubject(subject);
        h.setText(text, false); // plain text
        mailSender.send(msg);
    }
}
