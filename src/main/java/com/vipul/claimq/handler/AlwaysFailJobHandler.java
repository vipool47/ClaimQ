package com.vipul.claimq.handler;

import org.springframework.stereotype.Component;

@Component
public class AlwaysFailJobHandler implements JobHandler {

    @Override
    public String getTaskType() {
        return "always-fail";
    }

    @Override
    public void execute(String payload) {
        throw new IllegalStateException("This handler always fails");
    }
}