package com.vipul.claimq.support;

import com.vipul.claimq.handler.JobHandler;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RecordingJobHandler implements JobHandler {

    private final Map<String, AtomicInteger> calls = new ConcurrentHashMap<>();
    private final Set<String> threads = ConcurrentHashMap.newKeySet();

    @Override
    public String getTaskType() {
        return "recording";
    }

    @Override
    public void execute(String payload) throws Exception {
        calls.computeIfAbsent(payload, key -> new AtomicInteger()).incrementAndGet();
        threads.add(Thread.currentThread().getName());
        Thread.sleep(20);
    }

    public Map<String, Integer> calls() {
        Map<String, Integer> snapshot = new HashMap<>();
        calls.forEach((payload, count) -> snapshot.put(payload, count.get()));
        return snapshot;
    }

    public Set<String> threadNames() {
        return new HashSet<>(threads);
    }

    public void reset() {
        calls.clear();
        threads.clear();
    }
}