package ru.mobilica.sender.repo;


import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.mobilica.sender.domain.TemplateType;
import ru.mobilica.sender.domain.entity.Template;

public interface TemplateRepository extends JpaRepository<Template, Long> {
    List<Template> findByCampaignIdAndTypeAndEnabledTrue(Long campaignId, TemplateType type);
}
