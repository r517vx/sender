package ru.mobilica.sender.controller.dto;

import jakarta.validation.constraints.*;

public record CreateCampaignRequest(
        @NotBlank String name,
        @Email @NotBlank String fromEmail,
        String replyToEmail
) {}
