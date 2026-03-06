package ru.mobilica.sender.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.mobilica.sender.domain.entity.Campaign;
import ru.mobilica.sender.repo.CampaignRepository;
import ru.mobilica.sender.repo.MessageRepository;

@Service
@RequiredArgsConstructor
public class CampaignService {
    private final CampaignRepository campaignRepo;
    private final MessageRepository messageRepo;

    @Transactional
    public int enqueueBySource(Long campaignId, String source) {

        Campaign campaign = campaignRepo.findById(campaignId)
                .orElseThrow(() -> new IllegalArgumentException("Campaign not found"));

        return messageRepo.enqueueBySource(campaign.getId(), source);
    }
}
