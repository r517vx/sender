package ru.mobilica.sender.service;

import org.springframework.mail.MailException;
import org.springframework.stereotype.Component;

@Component
public class ErrorClassifier {

    public Classification classify(Exception e) {
        String msg = (e.getMessage() == null) ? "" : e.getMessage().toLowerCase();

        // Очень грубая классификация (потом улучшим, когда увидим реальные ответы Яндекса)
        if (msg.contains("user unknown") || msg.contains("mailbox unavailable") || msg.contains("no such user")) {
            return Classification.FINAL_HARD_BOUNCE;
        }
        if (msg.contains("timeout") || msg.contains("try again") || msg.contains("temporarily") || msg.contains("4.")) {
            return Classification.TEMP;
        }
        // MailException часто бывает и на temp, и на final — оставим как TEMP по умолчанию
        if (e instanceof MailException) {
            return Classification.TEMP;
        }
        return Classification.TEMP;
    }

    public enum Classification { TEMP, FINAL_HARD_BOUNCE }
}

