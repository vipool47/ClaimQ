package com.vipul.claimq.worker;

import com.vipul.claimq.handler.JobHandler;
import com.vipul.claimq.handler.JobHandlerRegistry;
import com.vipul.claimq.model.Job;
import org.springframework.stereotype.Component;

@Component
public class JobProcessor {

    private final JobHandlerRegistry registry;

    public JobProcessor(JobHandlerRegistry registry) {
        this.registry = registry;
    }

    public void process(Job job) throws Exception {
        JobHandler handler = registry.find(job.taskType())
                .orElseThrow(() -> new IllegalStateException(
                        "No handler registered for task type: " + job.taskType()));
        handler.execute(job.payload());
    }
}