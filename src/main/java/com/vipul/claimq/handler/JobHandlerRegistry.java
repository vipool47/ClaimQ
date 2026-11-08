package com.vipul.claimq.handler;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
public class JobHandlerRegistry {

    private final Map<String, JobHandler> handlers;

    public JobHandlerRegistry(List<JobHandler> handlerList) {
        Map<String, JobHandler> map = new HashMap<>();
        for (JobHandler handler : handlerList) {
            JobHandler previous = map.put(handler.getTaskType(), handler);
            if (previous != null) {
                throw new IllegalStateException(
                        "Duplicate handler for task type: " + handler.getTaskType());
            }
        }
        this.handlers = Map.copyOf(map);
    }

    public Optional<JobHandler> find(String taskType) {
        return Optional.ofNullable(handlers.get(taskType));
    }

    public boolean supports(String taskType) {
        return handlers.containsKey(taskType);
    }

    public Set<String> taskTypes() {
        return handlers.keySet();
    }
}