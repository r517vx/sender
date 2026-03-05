package ru.mobilica.sender.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mobilica.sender.domain.CampaignStatus;
import ru.mobilica.sender.domain.MessageStatus;
import ru.mobilica.sender.domain.SuppressionReason;
import ru.mobilica.sender.domain.entity.Campaign;
import ru.mobilica.sender.domain.entity.Message;
import ru.mobilica.sender.domain.entity.Suppression;
import ru.mobilica.sender.repo.MessageRepository;
import ru.mobilica.sender.repo.SuppressionRepository;
import ru.mobilica.sender.util.CommonUtils;

import java.time.*;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class SenderRunner {
    private static final Logger log = LoggerFactory.getLogger(SenderWorker.class);
    private static final Duration OVERDUE_GRACE = Duration.ofMinutes(3);

    private final MessageRepository messageRepo;
    private final SuppressionRepository suppressionRepo;
    private final EmailComposerService composer;
    private final SmtpSendService smtp;
    private final ErrorClassifier classifier;

    @Transactional
    public SenderRunner.RunResult runOnce(int batchSize) {
        List<Message> locked = messageRepo.lockBatchReady(batchSize);
        if (locked.isEmpty()) return new RunResult(0, 0, 0);

        List<Long> ids = locked.stream().map(Message::getId).toList();

        // Важно: работать дальше НЕ со списком locked, а с fully loaded:
        List<Message> batch = messageRepo.findAllWithCampaignAndRecipient(ids);
        if (batch.isEmpty()) return new SenderRunner.RunResult(0, 0, 0);

        int sent = 0, retry = 0, failed = 0;

        OffsetDateTime nowUtc = nowUtc();

        // Переводим в SENDING (защита от дублей)
        messageRepo.bulkMoveStatus(ids, MessageStatus.READY, MessageStatus.SENDING, nowUtc);
        // Примечание: записи, которые были RETRY_WAIT, останутся RETRY_WAIT; их можно обрабатывать без bulkMove,
        // но проще: выбирать только READY, а RETRY_WAIT переводить в READY отдельным джобом.
        // Для старта оставим READY.

        for (Message m : batch) {
            Campaign c = m.getCampaign();

            OffsetDateTime pa = m.getPlannedAt();
            if (pa != null && pa.isBefore(nowUtc.minus(OVERDUE_GRACE))) {
                // Сервис долго был оффлайн / окно пропущено — НЕ отправляем сразу.
                OffsetDateTime newPlanned = nextSlotPlannedAt(c, nowUtc);
                m.setStatus(MessageStatus.READY);       // важно: возвращаем из SENDING обратно
                m.setPlannedAt(newPlanned);

                log.info("Rescheduled overdue messageId={} oldPlannedAt={} newPlannedAt={}",
                        m.getId(), pa, newPlanned);
                continue;
            }

            if (c.getStatus() != CampaignStatus.ACTIVE) {
                // вернем обратно
                m.setStatus(MessageStatus.READY);
                continue;
            }
            if (!isWithinSendWindow(c)) {
                m.setStatus(MessageStatus.READY);
                // перенесем на начало окна завтра/сегодня
                m.setPlannedAt(nextWindowStart(c));
                continue;
            }
            if (messageRepo.countSentToday(c.getId()) >= c.getDailyLimit()) {
                m.setStatus(MessageStatus.READY);
                m.setPlannedAt(nextDayWindowStart(c));
                continue;
            }

            String to = m.getRecipient().getEmail();
            if (suppressionRepo.existsByEmail(to)) {
                m.setStatus(MessageStatus.SUPPRESSED);
                m.setLastErrorCode("SUPPRESSED");
                m.setLastErrorText("Email is in suppression list");
                failed++;
                continue;
            }

            try {
                EmailComposerService.ComposedEmail email = composer.compose(c, m.getRecipient());
                smtp.sendPlainText(c.getFromEmail(), c.getReplyToEmail(), to, email.subject(), email.textBody());

                m.setStatus(MessageStatus.SENT);
                m.setSentAt(nowUtc());
                m.setAttempts(m.getAttempts() + 1);
                m.setVariantJson(email.variantJson());
                m.setLastErrorCode(null);
                m.setLastErrorText(null);

                // Пауза — через plannedAt для следующих сообщений (сервисно).
                // Здесь можно ничего не делать; plannedAt актуален для задач, а пауза управляется самим воркером.
                sent++;
                log.info("Sent messageId={} to={}", m.getId(), to);
            } catch (Exception e) {
                m.setAttempts(m.getAttempts() + 1);
                m.setLastErrorCode(e.getClass().getSimpleName());
                m.setLastErrorText(trim(e.getMessage(), 1000));

                var cls = classifier.classify(e);
                if (cls == ErrorClassifier.Classification.FINAL_HARD_BOUNCE || m.getAttempts() > c.getMaxRetries()) {
                    m.setStatus(MessageStatus.FAILED_FINAL);
                    failed++;

                    // hard bounce -> suppression (по желанию можно включать по флагу кампании)
                    if (cls == ErrorClassifier.Classification.FINAL_HARD_BOUNCE) {
                        Suppression s = new Suppression();
                        s.setEmail(to);
                        s.setReason(SuppressionReason.BOUNCE_HARD);
                        s.setComment("Auto from SMTP error: " + trim(e.getMessage(), 200));
                        suppressionRepo.save(s);
                    }
                } else {
                    m.setStatus(MessageStatus.READY);
                    m.setPlannedAt(nowUtc().plusSeconds(backoffSeconds(m.getAttempts())));
                    retry++;
                }
                log.warn("Send failed messageId={} to={} err={}", m.getId(), to, e.toString());
            }

            // Рандомная пауза между отправками:
            // Важно: sleep внутри транзакции — плохо. Поэтому паузы делаем НЕ внутри транзакции, а как plannedAt.
            // Для старта — не sleep. Следующий тик подхватит следующую задачу.
            // Если очень нужно именно “один воркер, один цикл” — вынесем tick без @Transactional и будем коммитить по одному.
        }

        // JPA flush на выходе из @Transactional
        return new SenderRunner.RunResult(sent, retry, failed);
    }

    private boolean isWithinSendWindow(Campaign c) {
        ZoneId tz = ZoneId.systemDefault(); // можно взять из конфига sender.timezone
        LocalTime now = LocalTime.now(tz);
        LocalTime start = c.getSendWindowStart();
        LocalTime end = c.getSendWindowEnd();
        if (start.equals(end)) return true;
        if (start.isBefore(end)) {
            return !now.isBefore(start) && now.isBefore(end);
        }
        // окно через полночь
        return !now.isBefore(start) || now.isBefore(end);
    }

    private OffsetDateTime nextWindowStart(Campaign c) {
        ZoneId tz = ZoneId.systemDefault();
        ZonedDateTime z = ZonedDateTime.now(tz);
        ZonedDateTime startToday = z.with(c.getSendWindowStart());
        if (z.isBefore(startToday)) return startToday.withZoneSameInstant(ZoneOffset.UTC).toOffsetDateTime();
        return startToday.plusDays(1).withZoneSameInstant(ZoneOffset.UTC).toOffsetDateTime();
    }

    private OffsetDateTime nextDayWindowStart(Campaign c) {
        ZoneId tz = ZoneId.systemDefault();
        ZonedDateTime z = ZonedDateTime.now(tz).plusDays(1).with(c.getSendWindowStart());
        return z.withZoneSameInstant(ZoneOffset.UTC).toOffsetDateTime();
    }

    private OffsetDateTime nowUtc() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }

    private long backoffSeconds(int attempts) {
        // 1-я ошибка: ~5 минут, 2-я: ~30 минут
        long base = (attempts <= 1) ? 5 * 60 : 30 * 60;
        long jitter = (long) (base * 0.3);
        long rnd = ThreadLocalRandom.current().nextLong(-jitter, jitter + 1);
        return Math.max(60, base + rnd);
    }

    private String trim(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }

    public record RunResult(int sent, int retryScheduled, int failedFinal) {
    }

    // планируем следующий слот по кампании (гарантирует разрыв minDelay..maxDelay)
    private OffsetDateTime nextSlotPlannedAt(Campaign c, OffsetDateTime nowUtc) {
        OffsetDateTime base = nowUtc;
        if (c.getNextPlannedAt() != null && c.getNextPlannedAt().isAfter(base)) {
            base = c.getNextPlannedAt();
        }
        OffsetDateTime planned = base.plusSeconds(CommonUtils.randomDelaySeconds(c)).withNano(0);
        c.setNextPlannedAt(planned);
        return planned;
    }
}
