package ru.mobilica.sender.repo;


import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.mobilica.sender.domain.CampaignStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.mobilica.sender.domain.entity.Campaign;

public interface CampaignRepository extends JpaRepository<Campaign, Long> {
    List<Campaign> findByStatus(CampaignStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Campaign c where c.id = :id")
    Campaign lockById(@Param("id") Long id);
}
