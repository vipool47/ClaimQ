package com.vipul.claimq.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

@Component
public class FlakyJobHandler implements JobHandler {

    private static final Logger log = LoggerFactory.getLogger(FlakyJobHandler.class);

    @Override
    public String getTaskType() {
        return "flaky";
    }

    @Override
    public void execute(String payload) throws Exception {
        Thread.sleep(500);
        if (ThreadLocalRandom.current().nextBoolean()) {
            throw new IllegalStateException("Simulated random failure");
        }
        log.info("Flaky job succeeded");
    }
}