package ru.mobilica.sender.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.mobilica.sender.controller.dto.CreateRecipientRequest;
import ru.mobilica.sender.domain.entity.Recipient;
import ru.mobilica.sender.repo.RecipientRepository;

@RestController
@RequestMapping("/api/recipients")
public class RecipientController {

    private final RecipientRepository repo;

    public RecipientController(RecipientRepository repo) {
        this.repo = repo;
    }

    @PostMapping
    public Recipient create(@RequestBody @Valid CreateRecipientRequest req) {
        Recipient r = new Recipient();
        r.setEmail(req.email().trim().toLowerCase());
        r.setFirstName(req.firstName());
        r.setLastName(req.lastName());
        r.setCompany(req.company());
        r.setPosition(req.position());
        r.setSource(req.source());
        return repo.save(r);
    }
}
