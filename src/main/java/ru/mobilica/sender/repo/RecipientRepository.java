package ru.mobilica.sender.repo;


import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.mobilica.sender.domain.entity.Recipient;

public interface RecipientRepository extends JpaRepository<Recipient, Long> {
    Optional<Recipient> findByEmail(String email);
    boolean existsByEmail(String email);
}
