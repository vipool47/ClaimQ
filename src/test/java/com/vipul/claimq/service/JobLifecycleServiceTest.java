package com.vipul.claimq.service;

import com.vipul.claimq.IntegrationTestBase;
import com.vipul.claimq.model.Job;
import com.vipul.claimq.model.JobStatus;
import com.vipul.claimq.model.RecoveredJob;
import com.vipul.claimq.repository.JobRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JobLifecycleServiceTest extends IntegrationTestBase {

    @Autowired
    JobRepository repository;

    @Autowired
    JobClaimService claimService;

    @Autowired
    JobLifecycleService lifecycle;

    // complete

    @Test
    void completeWithCurrentTokenFinishesJobAndClearsOwnership() {
        Job claimed = claimNewJob(3);

        assertThat(lifecycle.complete(claimed.id(), claimed.claimToken())).isTrue();

        Job job = reload(claimed);
        assertThat(job.status()).isEqualTo(JobStatus.COMPLETED);
        assertThat(job.lockedBy()).isNull();
        assertThat(job.lockedAt()).isNull();
        assertThat(job.claimToken()).isNull();
    }

    @Test
    void completeWithWrongTokenChangesNothing() {
        Job claimed = claimNewJob(3);

        assertThat(lifecycle.complete(claimed.id(), UUID.randomUUID())).isFalse();

        Job job = reload(claimed);
        assertThat(job.status()).isEqualTo(JobStatus.PROCESSING);
        assertThat(job.claimToken()).isEqualTo(claimed.claimToken());
    }

    // fail: retry and exhaustion

    @Test
    void failWithAttemptsLeftSchedulesRetryInTheFuture() {
        Job claimed = claimNewJob(3);

        Optional<JobStatus> result = lifecycle.fail(claimed.id(), claimed.claimToken(), "boom");

        assertThat(result).contains(JobStatus.PENDING);
        Job job = reload(claimed);
        assertThat(job.status()).isEqualTo(JobStatus.PENDING);
        assertThat(job.lastError()).isEqualTo("boom");
        assertThat(job.claimToken()).isNull();
        // first retry waits base-delay x 1 = 5 seconds
        assertThat(job.scheduledAt()).isAfter(Instant.now().plusSeconds(3));
        assertThat(job.scheduledAt()).isBefore(Instant.now().plusSeconds(8));
        // and nobody can claim it until then
        assertThat(claimService.claimNext("worker-b")).isEmpty();
    }

    @Test
    void failOnLastAttemptMarksJobFailed() {
        Job claimed = claimNewJob(1);

        Optional<JobStatus> result = lifecycle.fail(claimed.id(), claimed.claimToken(), "boom");

        assertThat(result).contains(JobStatus.FAILED);
        Job job = reload(claimed);
        assertThat(job.status()).isEqualTo(JobStatus.FAILED);
        assertThat(job.lastError()).isEqualTo("boom");
        assertThat(job.claimToken()).isNull();
        assertThat(claimService.claimNext("worker-b")).isEmpty();
    }

    // stale worker

    @Test
    void staleWorkerCannotOverwriteNewerClaim() {
        Job first = claimNewJob(3);

        // worker-a stalls, its lease expires, recovery requeues the job, worker-b claims it
        expireClaim(first);
        lifecycle.recoverExpiredClaims();
        Job second = claimService.claimNext("worker-b").orElseThrow();

        // worker-a wakes up and reports using its old token
        assertThat(lifecycle.complete(first.id(), first.claimToken())).isFalse();
        assertThat(lifecycle.fail(first.id(), first.claimToken(), "late failure")).isEmpty();

        Job current = reload(first);
        assertThat(current.status()).isEqualTo(JobStatus.PROCESSING);
        assertThat(current.lockedBy()).isEqualTo("worker-b");
        assertThat(current.claimToken()).isEqualTo(second.claimToken());

        // the real owner can still finish
        assertThat(lifecycle.complete(second.id(), second.claimToken())).isTrue();
    }

    // recovery

    @Test
    void recoveryReturnsExpiredClaimToTheQueue() {
        Job claimed = claimNewJob(3);
        expireClaim(claimed);

        List<RecoveredJob> recovered = lifecycle.recoverExpiredClaims();

        assertThat(recovered).extracting(RecoveredJob::id).containsExactly(claimed.id());
        assertThat(recovered).extracting(RecoveredJob::status).containsExactly(JobStatus.PENDING);
        Job job = reload(claimed);
        assertThat(job.status()).isEqualTo(JobStatus.PENDING);
        assertThat(job.claimToken()).isNull();
        assertThat(job.lastError()).contains("expired");
        // another worker can take it now, as attempt 2
        assertThat(claimService.claimNext("worker-b").orElseThrow().attempts()).isEqualTo(2);
    }

    @Test
    void recoveryFailsExpiredClaimWithNoAttemptsLeft() {
        Job claimed = claimNewJob(1);
        expireClaim(claimed);

        List<RecoveredJob> recovered = lifecycle.recoverExpiredClaims();

        assertThat(recovered).extracting(RecoveredJob::status).containsExactly(JobStatus.FAILED);
        assertThat(reload(claimed).status()).isEqualTo(JobStatus.FAILED);
        assertThat(claimService.claimNext("worker-b")).isEmpty();
    }

    @Test
    void recoveryLeavesFreshClaimsAlone() {
        Job claimed = claimNewJob(3);

        assertThat(lifecycle.recoverExpiredClaims()).isEmpty();

        Job job = reload(claimed);
        assertThat(job.status()).isEqualTo(JobStatus.PROCESSING);
        assertThat(job.claimToken()).isEqualTo(claimed.claimToken());
    }

    // helpers

    private Job claimNewJob(int maxAttempts) {
        repository.insert("demo", "{}", maxAttempts);
        return claimService.claimNext("worker-a").orElseThrow();
    }

    private Job reload(Job job) {
        return repository.findById(job.id()).orElseThrow();
    }

    private void expireClaim(Job job) {
        jdbc.update("UPDATE jobs SET locked_at = now() - interval '5 minutes' WHERE id = ?", job.id());
    }
}