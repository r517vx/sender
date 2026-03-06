package ru.mobilica.sender.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.mobilica.sender.controller.dto.ImportRecipientsResult;
import ru.mobilica.sender.domain.entity.Recipient;
import ru.mobilica.sender.repo.RecipientRepository;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RecipientImportService {
    private final RecipientRepository recipientRepo;

    @Transactional
    public ImportRecipientsResult importPlainEmailList(MultipartFile file, String source) throws IOException {
        int total = 0, valid = 0, inserted = 0, duplicates = 0, invalid = 0;

        // Для дедупликации внутри одного файла
        Set<String> seen = new HashSet<>(2048);

        try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                total++;
                String email = normalize(line);
                if (email == null) {
                    invalid++;
                    continue;
                }

                if (!seen.add(email)) {
                    duplicates++;
                    continue; // дубль внутри файла
                }

                valid++;

                // быстрый путь: пытаемся вставить, а дубликаты ловим по unique index
                try {
                    Recipient r = new Recipient();
                    r.setEmail(email);
                    r.setSource(source);
                    recipientRepo.save(r);
                    inserted++;
                } catch (DataIntegrityViolationException ex) {
                    // дубликат в БД (по uq_recipients_email_lower)
                    duplicates++;
                }
            }
        }

        return new ImportRecipientsResult(total, valid, inserted, duplicates, invalid);
    }

    private String normalize(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.isEmpty()) return null;

        // иногда в файлах бывают "email;comment" — на будущее можно резать по ; , пробелу
        // но твой файл уже чистый. Оставлю минимально безопасно:
        s = s.replace("\uFEFF", ""); // BOM если вдруг

        s = s.toLowerCase(Locale.ROOT);

        // простая валидация
        if (!s.contains("@")) return null;
        if (s.startsWith("@") || s.endsWith("@")) return null;
        if (s.contains(" ")) return null;
        if (s.length() > 254) return null;

        return s;
    }
}
