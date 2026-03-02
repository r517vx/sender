package ru.mobilica.sender.service;


import ru.mobilica.sender.domain.entity.Recipient;

import java.util.Map;

public class TemplateRenderService {

    public String render(String template, Recipient r, Map<String, String> extra) {
        // Простой плейсхолдерный рендер
        // {{firstName}}, {{company}} ...
        String out = template;
        out = replace(out, "firstName", safe(r.getFirstName()));
        out = replace(out, "lastName", safe(r.getLastName()));
        out = replace(out, "company", safe(r.getCompany()));
        out = replace(out, "position", safe(r.getPosition()));
        if (extra != null) {
            for (var e : extra.entrySet()) {
                out = replace(out, e.getKey(), safe(e.getValue()));
            }
        }
        return out;
    }

    private String replace(String s, String key, String value) {
        return s.replace("{{" + key + "}}", value);
    }

    private String safe(String v) {
        return v == null ? "" : v;
    }
}
