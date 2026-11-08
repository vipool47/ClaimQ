package com.vipul.claimq.service;

import com.vipul.claimq.config.ClaimQProperties;
import com.vipul.claimq.model.JobStatus;
import com.vipul.claimq.model.RecoveredJob;
import com.vipul.claimq.repository.JobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class JobLifecycleService {

    private static final Logger log = LoggerFactory.getLogger(JobLifecycleService.class);
    private static final int MAX_ERROR_LENGTH = 1000;

    private final JobRepository jobRepository;
    private final ClaimQProperties properties;

    public JobLifecycleService(JobRepository jobRepository, ClaimQProperties properties) {
        this.jobRepository = jobRepository;
        this.properties = properties;
    }

    public boolean complete(long jobId, UUID claimToken) {
        boolean updated = jobRepository.complete(jobId, claimToken);
        if (!updated) {
            log.warn("Job {} was not completed: claim lost or job already finished", jobId);
        }
        return updated;
    }

    public Optional<JobStatus> fail(long jobId, UUID claimToken, String error) {
        String trimmed = error.length() > MAX_ERROR_LENGTH
                ? error.substring(0, MAX_ERROR_LENGTH)
                : error;

        Optional<JobStatus> result = jobRepository.fail(
                jobId, claimToken, trimmed,
                properties.retry().baseDelay().toMillis(),
                properties.retry().maxDelay().toMillis());

        if (result.isEmpty()) {
            log.warn("Job {} failure not recorded: claim lost or job already finished", jobId);
        } else if (result.get() == JobStatus.FAILED) {
            log.error("Job {} permanently FAILED after all attempts", jobId);
        } else {
            log.info("Job {} failed, scheduled for retry", jobId);
        }
        return result;
    }

    public List<RecoveredJob> recoverExpiredClaims() {
        List<RecoveredJob> recovered = jobRepository.recoverExpired(
                properties.worker().leaseTimeout().toMillis());

        for (RecoveredJob job : recovered) {
            if (job.status() == JobStatus.FAILED) {
                log.error("Job {} permanently FAILED: claim expired and attempts exhausted", job.id());
            } else {
                log.warn("Job {} claim expired, returned to the queue", job.id());
            }
        }
        return recovered;
    }
}