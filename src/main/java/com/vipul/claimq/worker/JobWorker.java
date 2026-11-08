package com.vipul.claimq.worker;

import com.vipul.claimq.model.Job;
import com.vipul.claimq.service.JobClaimService;
import com.vipul.claimq.service.JobLifecycleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class JobWorker implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(JobWorker.class);

    private final String workerId;
    private final long pollIntervalMs;
    private final JobClaimService claimService;
    private final JobProcessor processor;
    private final JobLifecycleService lifecycle;

    public JobWorker(String workerId,
                     long pollIntervalMs,
                     JobClaimService claimService,
                     JobProcessor processor,
                     JobLifecycleService lifecycle) {
        this.workerId = workerId;
        this.pollIntervalMs = pollIntervalMs;
        this.claimService = claimService;
        this.processor = processor;
        this.lifecycle = lifecycle;
    }

    private volatile boolean running = true;

    public void stopPolling() {
        running = false;
    }

    @Override
    public void run() {
        log.info("{} started", workerId);

        while (running && !Thread.currentThread().isInterrupted())  {
            try {
                Optional<Job> claimed = claimService.claimNext(workerId);
                if (claimed.isPresent()) {
                    runJob(claimed.get());
                } else {
                    pause();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                log.error("{}: unexpected error in worker loop", workerId, e);
                pause();
            }
        }

        log.info("{} stopped", workerId);
    }

    private void runJob(Job job) throws InterruptedException {
        log.info("{} claimed job {} (attempt {}/{})",
                workerId, job.id(), job.attempts(), job.maxAttempts());

        try {
            processor.process(job);
        } catch (InterruptedException e) {
            log.warn("{} interrupted while running job {}, leaving claim for recovery",
                    workerId, job.id());
            throw e;
        }  catch (Exception e) {
        log.error("{} job {} failed: {}", workerId, job.id(), e.toString());
        lifecycle.fail(job.id(), job.claimToken(), e.toString());
        return;
    }

        if (lifecycle.complete(job.id(), job.claimToken())) {
            log.info("{} completed job {}", workerId, job.id());
        }
    }

    private void pause() {
        try {
            Thread.sleep(pollIntervalMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}