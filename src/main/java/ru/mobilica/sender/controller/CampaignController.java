package ru.mobilica.sender.controller;


import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.mobilica.sender.controller.dto.CreateCampaignRequest;
import ru.mobilica.sender.controller.dto.EnqueueRequest;
import ru.mobilica.sender.domain.CampaignStatus;
import ru.mobilica.sender.domain.entity.Campaign;
import ru.mobilica.sender.repo.CampaignRepository;
import ru.mobilica.sender.repo.MessageRepository;
import ru.mobilica.sender.repo.RecipientRepository;
import ru.mobilica.sender.repo.SuppressionRepository;
import ru.mobilica.sender.service.CampaignService;
import ru.mobilica.sender.service.SenderRunner;

import java.util.Map;

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

        int affected = campaignService.enqueueByIds(id, req.recipientIds(), req.emails());

        return ResponseEntity.ok(Map.of(
                "campaignId", id,
                "messagesPlanned", affected
        ));
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

}
