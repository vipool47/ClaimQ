package com.vipul.claimq.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SlowJobHandler implements JobHandler {

    private static final Logger log = LoggerFactory.getLogger(SlowJobHandler.class);

    @Override
    public String getTaskType() {
        return "slow";
    }

    @Override
    public void execute(String payload) throws Exception {
        log.info("Slow job started");
        Thread.sleep(8000);
        log.info("Slow job finished");
    }
}