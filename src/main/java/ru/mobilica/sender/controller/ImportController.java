package ru.mobilica.sender.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.mobilica.sender.controller.dto.ImportRecipientsResult;
import ru.mobilica.sender.service.RecipientImportService;

import java.io.IOException;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ImportController {
    private final RecipientImportService importService;
    @PostMapping(value = "/recipients/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportRecipientsResult importRecipients(
            @RequestPart("file") MultipartFile file,
            @RequestParam(defaultValue = "manual_import") String source
    ) throws IOException {

        return importService.importPlainEmailList(file, source);
    }
}
