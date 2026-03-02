package ru.mobilica.sender.service;


import ru.mobilica.sender.domain.entity.Template;

import java.security.SecureRandom;
import java.util.List;

public class WeightedPicker {
    private final SecureRandom rnd = new SecureRandom();

    public Template pick(List<Template> variants) {
        int total = variants.stream().mapToInt(t -> Math.max(1, t.getWeight())).sum();
        int r = rnd.nextInt(total);
        int acc = 0;
        for (Template t : variants) {
            acc += Math.max(1, t.getWeight());
            if (r < acc) return t;
        }
        return variants.get(0);
    }
}
