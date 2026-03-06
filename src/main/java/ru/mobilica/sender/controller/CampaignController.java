package ru.mobilica.sender.controller;


import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.mobilica.sender.controller.dto.CreateCampaignRequest;
import ru.mobilica.sender.controller.dto.EnqueueRequest;
import ru.mobilica.sender.domain.CampaignStatus;
import ru.mobilica.sender.domain.MessageStatus;
import ru.mobilica.sender.domain.entity.Campaign;
import ru.mobilica.sender.domain.entity.Message;
import ru.mobilica.sender.domain.entity.Recipient;
import ru.mobilica.sender.repo.CampaignRepository;
import ru.mobilica.sender.repo.MessageRepository;
import ru.mobilica.sender.repo.RecipientRepository;
import ru.mobilica.sender.repo.SuppressionRepository;
import ru.mobilica.sender.service.CampaignService;
import ru.mobilica.sender.service.SenderRunner;
import ru.mobilica.sender.util.CommonUtils;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequestMapping("/api")
@AllArgsConstructor
public class CampaignController {

    private final CampaignRepository campaignRepo;
    private final RecipientRepository recipientRepo;
    private final MessageRepository messageRepo;
    private final SenderRunner runner;
    private final SuppressionRepository suppressionRepo;
    private final CampaignService campaignService;


    @PostMapping("/campaigns")
    public Campaign createCampaign(@RequestBody @Valid CreateCampaignRequest req) {
        Campaign c = new Campaign();
        c.setName(req.name());
        c.setFromEmail(req.fromEmail());
        c.setReplyToEmail(req.replyToEmail());
        c.setStatus(CampaignStatus.DRAFT);
        c.setDailyLimit(20);
        c.setMinDelaySec(90);
        c.setMaxDelaySec(210);
        c.setMaxRetries(2);
        c.setSendWindowStart(java.time.LocalTime.of(9, 30));
        c.setSendWindowEnd(java.time.LocalTime.of(18, 30));
        return campaignRepo.save(c);
    }

    @PostMapping("/campaigns/{id}/activate")
    public Campaign activate(@PathVariable Long id) {
        Campaign c = campaignRepo.findById(id).orElseThrow();
        c.setStatus(CampaignStatus.ACTIVE);
        return campaignRepo.save(c);
    }

    @PostMapping("/campaigns/{id}/pause")
    public Campaign pause(@PathVariable Long id) {
        Campaign c = campaignRepo.findById(id).orElseThrow();
        c.setStatus(CampaignStatus.PAUSED);
        return campaignRepo.save(c);
    }

    @PostMapping("/campaigns/{id}/enqueue")
    @Transactional
    public ResponseEntity<?> enqueue(@PathVariable Long id, @RequestBody EnqueueRequest req) {

        Campaign c = campaignRepo.lockById(id); // важно: FOR UPDATE

        OffsetDateTime base = nowUtc();
        if (c.getNextPlannedAt() != null && c.getNextPlannedAt().isAfter(base)) {
            base = c.getNextPlannedAt();
        }

        if (req.recipientIds() != null) {
            for (Long rid : req.recipientIds()) {
                Recipient r = recipientRepo.findById(rid).orElseThrow();
                base = upsertMessageQueued(c, r, base);
            }
        }
        if (req.emails() != null) {
            for (String email : req.emails()) {
                Recipient r = recipientRepo.findByEmail(email)
                        .orElseThrow(() -> new IllegalArgumentException("Recipient not found: " + email));
                base = upsertMessageQueued(c, r, base);
            }
        }

        c.setNextPlannedAt(base);
        // campaignRepo.save(c); // необязательно, т.к. managed entity в транзакции
        return ResponseEntity.ok().build();
    }

    private OffsetDateTime upsertMessageQueued(Campaign c, Recipient r, OffsetDateTime base) {
        // suppression
        if (suppressionRepo.existsByEmail(r.getEmail())) return base;

        // следующий слот
        OffsetDateTime plannedAt = base.plusSeconds(CommonUtils.randomDelaySeconds(c));

        messageRepo.findByCampaignIdAndRecipientId(c.getId(), r.getId())
                .ifPresentOrElse(existing -> {
                    if (existing.getStatus() == MessageStatus.SENT) return;
                    if (existing.getStatus() == MessageStatus.FAILED_FINAL || existing.getStatus() == MessageStatus.SUPPRESSED)
                        return;

                    existing.setStatus(MessageStatus.READY);
                    existing.setPlannedAt(plannedAt);
                    existing.setLastErrorCode(null);
                    existing.setLastErrorText(null);
                }, () -> {
                    Message m = new Message();
                    m.setCampaign(c);
                    m.setRecipient(r);
                    m.setStatus(MessageStatus.READY);
                    m.setPlannedAt(plannedAt);
                    messageRepo.save(m);
                });

        return plannedAt; // важно: base двигаем вперёд
    }

    @PostMapping("/sender/run-once")
    public SenderRunner.RunResult runOnce(@RequestParam(defaultValue = "5") int batchSize) {
        return runner.runOnce(batchSize);
    }

    @PostMapping("/campaigns/{id}/enqueue-by-source")
    public Map<String, Object> enqueueBySource(
            @PathVariable Long id,
            @RequestParam String source
    ) {

        int inserted = campaignService.enqueueBySource(id, source);

        return Map.of(
                "campaignId", id,
                "source", source,
                "messagesCreated", inserted
        );
    }

    private OffsetDateTime nowUtc() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }
}
