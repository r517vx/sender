package ru.mobilica.sender.controller.dto;

public record ImportRecipientsResult(int totalLines,
                                     int validEmails,
                                     int inserted,
                                     int duplicates,
                                     int invalid) {
}
