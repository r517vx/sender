package ru.mobilica.sender.repo;


import org.springframework.data.jpa.repository.JpaRepository;
import ru.mobilica.sender.domain.entity.Suppression;

public interface SuppressionRepository extends JpaRepository<Suppression, String> {
    boolean existsByEmail(String email);
}
