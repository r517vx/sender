package ru.mobilica.sender.domain.entity;

import jakarta.persistence.*;

import java.time.*;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import ru.mobilica.sender.domain.CampaignStatus;

@Getter
@Setter
@Entity
@Table(name = "campaigns")
public class Campaign {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CampaignStatus status;

    @Column(name = "from_email", nullable = false)
    private String fromEmail;

    @Column(name = "reply_to_email")
    private String replyToEmail;

    @Column(name = "daily_limit", nullable = false)
    private int dailyLimit;

    @Column(name = "send_window_start", nullable = false)
    private LocalTime sendWindowStart;

    @Column(name = "send_window_end", nullable = false)
    private LocalTime sendWindowEnd;

    @Column(name = "min_delay_sec", nullable = false)
    private int minDelaySec;

    @Column(name = "max_delay_sec", nullable = false)
    private int maxDelaySec;

    @Column(name = "max_retries", nullable = false)
    private int maxRetries;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "next_planned_at")
    private OffsetDateTime nextPlannedAt;

}

