package ru.mobilica.sender.service;


import java.util.*;
import org.springframework.stereotype.Service;
import ru.mobilica.sender.domain.TemplateType;
import ru.mobilica.sender.domain.entity.Campaign;
import ru.mobilica.sender.domain.entity.Recipient;
import ru.mobilica.sender.domain.entity.Template;
import ru.mobilica.sender.repo.TemplateRepository;

@Service
public class EmailComposerService {
    private final TemplateRepository templateRepo;
    private final WeightedPicker picker = new WeightedPicker();
    private final TemplateRenderService renderer = new TemplateRenderService();

    public EmailComposerService(TemplateRepository templateRepo) {
        this.templateRepo = templateRepo;
    }

    public ComposedEmail compose(Campaign c, Recipient r) {
        List<Template> subj = templateRepo.findByCampaignIdAndTypeAndEnabledTrue(c.getId(), TemplateType.SUBJECT);
        List<Template> greet = templateRepo.findByCampaignIdAndTypeAndEnabledTrue(c.getId(), TemplateType.GREETING);
        List<Template> open = templateRepo.findByCampaignIdAndTypeAndEnabledTrue(c.getId(), TemplateType.OPENING);
        List<Template> body = templateRepo.findByCampaignIdAndTypeAndEnabledTrue(c.getId(), TemplateType.BODY);

        // Минимальные дефолты на случай пустой базы
        String subjectT = subj.isEmpty() ? "Быстрый вопрос" : picker.pick(subj).getContent();
        String greetT   = greet.isEmpty() ? "{{firstName}}, добрый день." : picker.pick(greet).getContent();
        String openT    = open.isEmpty() ? "Пишу по делу, займет минуту." : picker.pick(open).getContent();
        String bodyT    = body.isEmpty() ? "Коротко: мы проверяем интерес к продукту. Можно 1 вопрос?" : picker.pick(body).getContent();

        String subject = renderer.render(subjectT, r, Map.of());
        String greeting = renderer.render(greetT, r, Map.of());
        String opening  = renderer.render(openT, r, Map.of());
        String bodyText = renderer.render(bodyT, r, Map.of());

        // Сильно рекомендую начинать с plain text (без HTML), чтобы выглядеть как “человеческое” письмо
        String text = String.join("\n\n",
                greeting.trim(),
                opening.trim(),
                bodyText.trim(),
                signature(c)
        ).trim();

        // variantJson можно потом положить: какие шаблоны выбраны
        String variantJson = "{\"subject\":\"" + escape(subjectT) + "\"}";

        return new ComposedEmail(subject, text, variantJson);
    }

    private String signature(Campaign c) {
        // Можно вынести в шаблон, но для старта — так
        return "—\nЛев\nmobilica.ru\nЕсли неактуально — ответьте «стоп», и я больше не напишу.";
    }

    private String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public record ComposedEmail(String subject, String textBody, String variantJson) {}
}
