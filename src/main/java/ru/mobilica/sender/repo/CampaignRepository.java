package ru.mobilica.sender.repo;


import ru.mobilica.sender.domain.CampaignStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.mobilica.sender.domain.entity.Campaign;

public interface CampaignRepository extends JpaRepository<Campaign, Long> {
    List<Campaign> findByStatus(CampaignStatus status);
}
