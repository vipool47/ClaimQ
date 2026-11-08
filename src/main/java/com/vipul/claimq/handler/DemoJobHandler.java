package com.vipul.claimq.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DemoJobHandler implements JobHandler {

    private static final Logger log = LoggerFactory.getLogger(DemoJobHandler.class);

    @Override
    public String getTaskType() {
        return "demo";
    }

    @Override
    public void execute(String payload) throws Exception {
        log.info("Demo job started, payload={}", payload);
        Thread.sleep(2000);
        log.info("Demo job finished");
    }
}