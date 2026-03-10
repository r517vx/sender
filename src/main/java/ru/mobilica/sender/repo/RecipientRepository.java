package ru.mobilica.sender.repo;


import org.springframework.data.jpa.repository.JpaRepository;
import ru.mobilica.sender.domain.entity.Recipient;

import java.util.List;
import java.util.Optional;

public interface RecipientRepository extends JpaRepository<Recipient, Long> {
    Optional<Recipient> findByEmail(String email);
    boolean existsByEmail(String email);

    List<Recipient> findAllBySource(String source);
}
