package ru.mobilica.sender.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.mobilica.sender.domain.MessageStatus;
import ru.mobilica.sender.domain.entity.Campaign;
import ru.mobilica.sender.domain.entity.Message;
import ru.mobilica.sender.domain.entity.Recipient;
import ru.mobilica.sender.repo.CampaignRepository;
import ru.mobilica.sender.repo.MessageRepository;
import ru.mobilica.sender.repo.RecipientRepository;
import ru.mobilica.sender.repo.SuppressionRepository;
import ru.mobilica.sender.util.CommonUtils;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CampaignService {

    private final CampaignRepository campaignRepo;
    private final MessageRepository messageRepo;
    private final RecipientRepository recipientRepo;
    private final SuppressionRepository suppressionRepo;

    @Transactional
    public int enqueueBySource(Long campaignId, String source) {
        Campaign c = campaignRepo.lockById(campaignId);
        if (c == null) {
            throw new IllegalArgumentException("Campaign not found");
        }

        OffsetDateTime base = nowUtc();
        if (c.getNextPlannedAt() != null && c.getNextPlannedAt().isAfter(base)) {
            base = c.getNextPlannedAt();
        }

        List<Recipient> recipients = recipientRepo.findAllBySource(source);

        int affected = 0;
        for (Recipient r : recipients) {
            OffsetDateTime newBase = upsertMessageQueued(c, r, base);
            if (!newBase.equals(base)) {
                affected++;
                base = newBase;
            }
        }

        c.setNextPlannedAt(base);
        return affected;
    }

    @Transactional
    public int enqueueByIds(Long campaignId, List<Long> recipientIds, List<String> emails) {
        Campaign c = campaignRepo.lockById(campaignId);
        if (c == null) {
            throw new IllegalArgumentException("Campaign not found");
        }

        OffsetDateTime base = nowUtc();
        if (c.getNextPlannedAt() != null && c.getNextPlannedAt().isAfter(base)) {
            base = c.getNextPlannedAt();
        }

        int affected = 0;

        if (recipientIds != null) {
            for (Long rid : recipientIds) {
                Recipient r = recipientRepo.findById(rid)
                        .orElseThrow(() -> new IllegalArgumentException("Recipient ID not found: " + rid));
                OffsetDateTime newBase = upsertMessageQueued(c, r, base);
                if (!newBase.equals(base)) {
                    affected++;
                    base = newBase;
                }
            }
        }

        if (emails != null) {
            for (String email : emails) {
                Recipient r = recipientRepo.findByEmail(email)
                        .orElseThrow(() -> new IllegalArgumentException("Recipient not found: " + email));
                OffsetDateTime newBase = upsertMessageQueued(c, r, base);
                if (!newBase.equals(base)) {
                    affected++;
                    base = newBase;
                }
            }
        }

        c.setNextPlannedAt(base);
        return affected;
    }

    private OffsetDateTime upsertMessageQueued(Campaign c, Recipient r, OffsetDateTime base) {
        if (suppressionRepo.existsByEmail(r.getEmail())) {
            return base;
        }

        OffsetDateTime plannedAt = base.plusSeconds(CommonUtils.randomDelaySeconds(c));

        messageRepo.findByCampaignIdAndRecipientId(c.getId(), r.getId())
                .ifPresentOrElse(existing -> {
                    if (existing.getStatus() == MessageStatus.SENT) return;
                    if (existing.getStatus() == MessageStatus.FAILED_FINAL || existing.getStatus() == MessageStatus.SUPPRESSED) {
                        return;
                    }

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

        return plannedAt;
    }

    private OffsetDateTime nowUtc() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }
}
