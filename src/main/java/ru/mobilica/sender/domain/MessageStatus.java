package ru.mobilica.sender.domain;

public enum MessageStatus {
    PLANNED, READY, SENDING, SENT, RETRY_WAIT, FAILED_FINAL, SUPPRESSED
}
