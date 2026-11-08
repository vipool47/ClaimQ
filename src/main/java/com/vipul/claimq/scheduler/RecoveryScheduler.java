package com.vipul.claimq.scheduler;

import com.vipul.claimq.service.JobLifecycleService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "claimq.recovery.enabled", havingValue = "true", matchIfMissing = true)
public class RecoveryScheduler {

    private final JobLifecycleService lifecycle;

    public RecoveryScheduler(JobLifecycleService lifecycle) {
        this.lifecycle = lifecycle;
    }

    @Scheduled(fixedDelayString = "${claimq.recovery.interval:15000}")
    public void recoverExpiredClaims() {
        lifecycle.recoverExpiredClaims();
    }
}