package ru.mobilica.sender.domain.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import ru.mobilica.sender.domain.SuppressionReason;

@Getter
@Setter
@Entity
@Table(name = "suppression")
public class Suppression {
    @Id
    @Column(nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SuppressionReason reason;

    private String comment;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

}