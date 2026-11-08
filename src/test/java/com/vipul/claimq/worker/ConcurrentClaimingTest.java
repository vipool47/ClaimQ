package com.vipul.claimq.worker;

import com.vipul.claimq.IntegrationTestBase;
import com.vipul.claimq.repository.JobRepository;
import com.vipul.claimq.support.RecordingJobHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

class ConcurrentClaimingTest extends IntegrationTestBase {

    private static final int JOB_COUNT = 100;

    @Autowired
    JobRepository repository;

    @Autowired
    WorkerManager workerManager;

    @Autowired
    RecordingJobHandler handler;

    @BeforeEach
    void resetHandler() {
        handler.reset();
    }

    @AfterEach
    void stopWorkers() {
        workerManager.stop();
    }

    @Test
    void fiveWorkersProcessEveryJobExactlyOnce() throws Exception {
        for (int i = 1; i <= JOB_COUNT; i++) {
            repository.insert("recording", "{\"n\":" + i + "}", 3);
        }

        workerManager.start();
        waitUntilAllCompleted(Duration.ofSeconds(30));
        workerManager.stop();

        // every job ran exactly once
        Map<String, Integer> calls = handler.calls();
        assertThat(calls).hasSize(JOB_COUNT);
        assertThat(calls.values()).allMatch(count -> count == 1);

        // and the work really was shared between workers
        assertThat(handler.threadNames()).hasSizeGreaterThan(1);

        // the database agrees: 100 jobs, all COMPLETED, all on their first attempt
        List<Map<String, Object>> summary = jdbc.queryForList(
                "SELECT status, attempts, count(*) AS total FROM jobs GROUP BY status, attempts");
        assertThat(summary).hasSize(1);
        assertThat(summary.get(0))
                .containsEntry("status", "COMPLETED")
                .containsEntry("attempts", 1)
                .containsEntry("total", 100L);
    }

    private void waitUntilAllCompleted(Duration timeout) throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            Long completed = jdbc.queryForObject(
                    "SELECT count(*) FROM jobs WHERE status = 'COMPLETED'", Long.class);
            if (completed != null && completed == JOB_COUNT) {
                return;
            }
            Thread.sleep(100);
        }
        fail("Jobs did not all complete within " + timeout);
    }
}