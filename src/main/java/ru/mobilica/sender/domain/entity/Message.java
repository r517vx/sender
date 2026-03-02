package ru.mobilica.sender.domain.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import ru.mobilica.sender.domain.MessageStatus;

@Getter
@Setter
@Entity
@Table(name = "messages", uniqueConstraints = {
        @UniqueConstraint(name = "uq_messages_campaign_recipient", columnNames = {"campaign_id", "recipient_id"})
}, indexes = {
        @Index(name = "idx_messages_status_planned", columnList = "status,planned_at"),
        @Index(name = "idx_messages_campaign", columnList = "campaign_id")
})
public class Message {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_id", nullable = false)
    private Recipient recipient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageStatus status;

    @Column(name = "planned_at", nullable = false)
    private OffsetDateTime plannedAt;

    @Column(name = "sent_at")
    private OffsetDateTime sentAt;

    @Column(nullable = false)
    private int attempts = 0;

    @Column(name = "last_error_code")
    private String lastErrorCode;

    @Column(name = "last_error_text", columnDefinition = "text")
    private String lastErrorText;

    @Column(name = "smtp_message_id")
    private String smtpMessageId;

    @Column(name = "variant_json", columnDefinition = "jsonb")
    private String variantJson;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    // getters/setters
    // ...
}
