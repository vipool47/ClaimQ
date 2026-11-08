package com.vipul.claimq.repository;

import com.vipul.claimq.IntegrationTestBase;
import com.vipul.claimq.model.Job;
import com.vipul.claimq.model.JobStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JobRepositoryTest extends IntegrationTestBase {

    @Autowired
    JobRepository repository;

    @Test
    void insertReturnsJobWithDatabaseDefaults() {
        Job job = repository.insert("demo", "{\"msg\":\"hello\"}", 3);

        assertThat(job.id()).isNotNull();
        assertThat(job.taskType()).isEqualTo("demo");
        assertThat(job.status()).isEqualTo(JobStatus.PENDING);
        assertThat(job.attempts()).isZero();
        assertThat(job.maxAttempts()).isEqualTo(3);
        assertThat(job.lockedBy()).isNull();
        assertThat(job.lockedAt()).isNull();
        assertThat(job.claimToken()).isNull();
        assertThat(job.scheduledAt()).isNotNull();
        assertThat(job.createdAt()).isNotNull();
    }

    @Test
    void findByIdReturnsStoredJob() {
        Job inserted = repository.insert("demo", "{\"a\":1}", 3);

        Optional<Job> found = repository.findById(inserted.id());

        assertThat(found).isPresent();
        assertThat(found.get().id()).isEqualTo(inserted.id());
    }

    @Test
    void findByIdReturnsEmptyForUnknownId() {
        assertThat(repository.findById(999_999L)).isEmpty();
    }

    @Test
    void payloadIsStoredAsJson() {
        Job job = repository.insert("demo", "{\"a\":1}", 3);

        // jsonb normalises formatting, so compare content and not exact text
        assertThat(job.payload()).contains("\"a\"").contains("1");
    }

    @Test
    void findAllFiltersByStatusAndOrdersNewestFirst() {
        Job first = repository.insert("demo", "{}", 3);
        Job second = repository.insert("demo", "{}", 3);
        jdbc.update("UPDATE jobs SET status = 'COMPLETED' WHERE id = ?", first.id());

        List<Job> pending = repository.findAll(JobStatus.PENDING, 10, 0);
        List<Job> all = repository.findAll(null, 10, 0);

        assertThat(pending).extracting(Job::id).containsExactly(second.id());
        assertThat(all).extracting(Job::id).containsExactly(second.id(), first.id());
    }

    @Test
    void pagesDoNotOverlap() {
        for (int i = 0; i < 5; i++) {
            repository.insert("demo", "{}", 3);
        }

        List<Job> page0 = repository.findAll(null, 2, 0);
        List<Job> page1 = repository.findAll(null, 2, 2);
        List<Job> page2 = repository.findAll(null, 2, 4);

        assertThat(page0).hasSize(2);
        assertThat(page1).hasSize(2);
        assertThat(page2).hasSize(1);
        assertThat(Stream.of(page0, page1, page2).flatMap(List::stream).map(Job::id).toList())
                .doesNotHaveDuplicates()
                .hasSize(5);
    }

    @Test
    void countRespectsStatusFilter() {
        Job done = repository.insert("demo", "{}", 3);
        repository.insert("demo", "{}", 3);
        jdbc.update("UPDATE jobs SET status = 'COMPLETED' WHERE id = ?", done.id());

        assertThat(repository.count(null)).isEqualTo(2);
        assertThat(repository.count(JobStatus.PENDING)).isEqualTo(1);
        assertThat(repository.count(JobStatus.COMPLETED)).isEqualTo(1);
        assertThat(repository.count(JobStatus.FAILED)).isZero();
    }

    @Test
    void databaseRejectsProcessingJobWithoutOwner() {
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO jobs (task_type, payload, status) VALUES ('demo', '{}'::jsonb, 'PROCESSING')"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}