package com.vipul.claimq.service;

import com.vipul.claimq.IntegrationTestBase;
import com.vipul.claimq.model.Job;
import com.vipul.claimq.model.JobStatus;
import com.vipul.claimq.repository.JobRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class JobClaimServiceTest extends IntegrationTestBase {

    @Autowired
    JobClaimService claimService;

    @Autowired
    JobRepository repository;

    @Test
    void claimSetsStatusOwnerTokenAndAttempts() {
        Job inserted = repository.insert("demo", "{}", 3);

        Optional<Job> claimed = claimService.claimNext("test-worker");

        assertThat(claimed).isPresent();
        Job job = claimed.get();
        assertThat(job.id()).isEqualTo(inserted.id());
        assertThat(job.status()).isEqualTo(JobStatus.PROCESSING);
        assertThat(job.attempts()).isEqualTo(1);
        assertThat(job.lockedBy()).isEqualTo("test-worker");
        assertThat(job.lockedAt()).isNotNull();
        assertThat(job.claimToken()).isNotNull();
    }

    @Test
    void claimOnEmptyQueueReturnsEmpty() {
        assertThat(claimService.claimNext("test-worker")).isEmpty();
    }

    @Test
    void claimTakesOldestJobFirst() {
        Job first = repository.insert("demo", "{}", 3);
        Job second = repository.insert("demo", "{}", 3);

        assertThat(claimService.claimNext("w").orElseThrow().id()).isEqualTo(first.id());
        assertThat(claimService.claimNext("w").orElseThrow().id()).isEqualTo(second.id());
    }

    @Test
    void claimSkipsJobsScheduledInTheFuture() {
        Job job = repository.insert("demo", "{}", 3);
        jdbc.update("UPDATE jobs SET scheduled_at = now() + interval '1 hour' WHERE id = ?", job.id());

        assertThat(claimService.claimNext("w")).isEmpty();
    }

    @Test
    void claimedJobCannotBeClaimedAgain() {
        repository.insert("demo", "{}", 3);

        assertThat(claimService.claimNext("w-1")).isPresent();
        assertThat(claimService.claimNext("w-2")).isEmpty();
    }

    @Test
    void everyClaimGetsItsOwnToken() {
        repository.insert("demo", "{}", 3);
        repository.insert("demo", "{}", 3);

        Job a = claimService.claimNext("w").orElseThrow();
        Job b = claimService.claimNext("w").orElseThrow();

        assertThat(a.claimToken()).isNotEqualTo(b.claimToken());
    }
}