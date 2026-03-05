package ru.mobilica.sender.util;

import ru.mobilica.sender.domain.entity.Campaign;

import java.util.concurrent.ThreadLocalRandom;

public class CommonUtils {

    public static long randomDelaySeconds(Campaign c) {
        long min = c.getMinDelaySec();
        long max = c.getMaxDelaySec();
        if (max < min) max = min;
        return ThreadLocalRandom.current().nextLong(min, max + 1);
    }

}
