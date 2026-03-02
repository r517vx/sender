package ru.mobilica.sender.controller.dto;

import java.util.List;

public record EnqueueRequest(
        List<Long> recipientIds,
        List<String> emails // можно слать прямо emails, если ids нет
) {}

