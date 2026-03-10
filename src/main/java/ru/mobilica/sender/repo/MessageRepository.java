package ru.mobilica.sender.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.mobilica.sender.domain.MessageStatus;
import ru.mobilica.sender.domain.entity.Message;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query(value = """
            select * from messages
            where status = 'READY'
              and planned_at <= now()
            order by planned_at asc
            for update skip locked
            limit :limit
            """, nativeQuery = true)
    List<Message> lockBatchReady(@Param("limit") int limit);

    @Modifying(flushAutomatically = true)
    @Query("""
            update Message m
            set m.status = :toStatus, m.updatedAt = :now
            where m.id in :ids and m.status = :fromStatus
            """)
    int bulkMoveStatus(@Param("ids") List<Long> ids,
                       @Param("fromStatus") MessageStatus fromStatus,
                       @Param("toStatus") MessageStatus toStatus,
                       @Param("now") OffsetDateTime now);

    long countByCampaignIdAndStatusIn(Long campaignId, List<MessageStatus> statuses);

    @Query(value = """
            select count(*) from messages
            where campaign_id = :campaignId
              and status = 'SENT'
              and sent_at >= date_trunc('day', now())
            """, nativeQuery = true)
    long countSentToday(@Param("campaignId") long campaignId);

    @Query("""
              select m from Message m
              join fetch m.campaign c
              join fetch m.recipient r
              where m.id in :ids
            """)
    List<Message> findAllWithCampaignAndRecipient(@Param("ids") List<Long> ids);

    Optional<Message> findByCampaignIdAndRecipientId(Long campaignId, Long recipientId);

}