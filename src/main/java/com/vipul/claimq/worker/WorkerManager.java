package com.vipul.claimq.worker;

import com.vipul.claimq.config.ClaimQProperties;
import com.vipul.claimq.service.JobClaimService;
import com.vipul.claimq.service.JobLifecycleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class WorkerManager implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(WorkerManager.class);
    private static final long FORCE_STOP_WAIT_SECONDS = 5;

    private final ClaimQProperties properties;
    private final JobClaimService claimService;
    private final JobProcessor processor;
    private final JobLifecycleService lifecycle;

    private final List<JobWorker> workers = new ArrayList<>();
    private ExecutorService executor;
    private volatile boolean running = false;

    public WorkerManager(ClaimQProperties properties,
                         JobClaimService claimService,
                         JobProcessor processor,
                         JobLifecycleService lifecycle) {
        this.properties = properties;
        this.claimService = claimService;
        this.processor = processor;
        this.lifecycle = lifecycle;
    }

    @Override
    public synchronized void start() {
        int count = properties.worker().count();
        long pollMs = properties.worker().pollInterval().toMillis();

        AtomicInteger threadNumber = new AtomicInteger(1);
        executor = Executors.newFixedThreadPool(count,
                runnable -> new Thread(runnable, "claimq-worker-" + threadNumber.getAndIncrement()));

        workers.clear();
        for (int i = 1; i <= count; i++) {
            JobWorker worker = new JobWorker("worker-" + i, pollMs, claimService, processor, lifecycle);
            workers.add(worker);
            executor.submit(worker);
        }

        running = true;
        log.info("Started {} workers (poll interval {} ms)", count, pollMs);
    }

    @Override
    public synchronized void stop() {
        if (executor == null || !running) {
            return;
        }

        long graceMs = properties.shutdown().gracePeriod().toMillis();
        log.info("Shutting down: no new jobs will be claimed, waiting up to {} ms for running jobs", graceMs);

        workers.forEach(JobWorker::stopPolling);
        executor.shutdown();

        try {
            if (executor.awaitTermination(graceMs, TimeUnit.MILLISECONDS)) {
                log.info("All workers finished cleanly");
            } else {
                log.warn("Grace period expired, interrupting running jobs (their claims are left for recovery)");
                executor.shutdownNow();
                executor.awaitTermination(FORCE_STOP_WAIT_SECONDS, TimeUnit.SECONDS);
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public boolean isAutoStartup() {
        return properties.worker().autoStart();
    }
}