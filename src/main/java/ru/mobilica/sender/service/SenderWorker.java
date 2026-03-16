package ru.mobilica.sender.service;


import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SenderWorker {

    private final SenderRunner runner;

    @Scheduled(fixedDelayString = "PT5S")
    public void tick() {
        // в проде можно сделать “тише”; в dev удобно часто
        runner.runOnce(1);
    }
}
