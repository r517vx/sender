package ru.mobilica.sender.controller.dto;

import jakarta.validation.constraints.*;

public record CreateRecipientRequest(@Email @NotBlank String email,
                                     String firstName,
                                     String lastName,
                                     String company,
                                     String position,
                                     String source) {
}
