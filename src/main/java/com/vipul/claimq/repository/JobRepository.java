package com.vipul.claimq.repository;

import com.vipul.claimq.model.Job;
import com.vipul.claimq.model.JobStatus;
import com.vipul.claimq.model.RecoveredJob;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JobRepository {

    private static final String INSERT_SQL = """
            INSERT INTO jobs (task_type, payload, max_attempts)
            VALUES (?, ?::jsonb, ?)
            RETURNING *
            """;

    private static final String FIND_BY_ID_SQL = """
            SELECT * FROM jobs WHERE id = ?
            """;

    private static final String LIST_SQL = """
        SELECT * FROM jobs
        ORDER BY id DESC
        LIMIT ? OFFSET ?
        """;

    private static final String LIST_BY_STATUS_SQL = """
        SELECT * FROM jobs
        WHERE status = ?
        ORDER BY id DESC
        LIMIT ? OFFSET ?
        """;

    private static final String CLAIM_NEXT_SQL = """
        UPDATE jobs
        SET status = 'PROCESSING',
            attempts = attempts + 1,
            locked_by = ?,
            locked_at = now(),
            claim_token = gen_random_uuid(),
            updated_at = now()
        WHERE id = (
            SELECT id
            FROM jobs
            WHERE status = 'PENDING'
              AND scheduled_at <= now()
            ORDER BY scheduled_at, id
            LIMIT 1
            FOR UPDATE SKIP LOCKED
        )
        RETURNING *
        """;

    public Optional<Job> claimNext(String workerId) {
        List<Job> jobs = jdbcTemplate.query(CLAIM_NEXT_SQL, rowMapper, workerId);
        return jobs.stream().findFirst();
    }

    private static final String COMPLETE_SQL = """
        UPDATE jobs
        SET status = 'COMPLETED',
            locked_by = NULL,
            locked_at = NULL,
            claim_token = NULL,
            updated_at = now()
        WHERE id = ?
          AND claim_token = ?
          AND status = 'PROCESSING'
        """;

    public boolean complete(long id, UUID claimToken) {
        return jdbcTemplate.update(COMPLETE_SQL, id, claimToken) == 1;
    }

    private static final String FAIL_SQL = """
        UPDATE jobs
        SET status = CASE
                WHEN attempts >= max_attempts THEN 'FAILED'
                ELSE 'PENDING'
            END,
            scheduled_at = CASE
                WHEN attempts >= max_attempts THEN scheduled_at
                ELSE now() + LEAST(?::bigint * attempts, ?::bigint) * interval '1 millisecond'
            END,
            last_error = ?,
            locked_by = NULL,
            locked_at = NULL,
            claim_token = NULL,
            updated_at = now()
        WHERE id = ?
          AND claim_token = ?
          AND status = 'PROCESSING'
        RETURNING status
        """;

    public Optional<JobStatus> fail(long id, UUID claimToken, String error,
                                    long baseDelayMs, long maxDelayMs) {
        List<JobStatus> result = jdbcTemplate.query(
                FAIL_SQL,
                (rs, rowNum) -> JobStatus.valueOf(rs.getString("status")),
                baseDelayMs, maxDelayMs, error, id, claimToken);
        return result.stream().findFirst();
    }

    private static final String RECOVER_SQL = """
        UPDATE jobs
        SET status = CASE
                WHEN attempts >= max_attempts THEN 'FAILED'
                ELSE 'PENDING'
            END,
            last_error = CASE
                WHEN attempts >= max_attempts
                    THEN 'Claim by ' || locked_by || ' expired, attempts exhausted'
                ELSE 'Claim by ' || locked_by || ' expired'
            END,
            locked_by = NULL,
            locked_at = NULL,
            claim_token = NULL,
            updated_at = now()
        WHERE status = 'PROCESSING'
          AND locked_at < now() - ?::bigint * interval '1 millisecond'
        RETURNING id, status
        """;

    public List<RecoveredJob> recoverExpired(long leaseTimeoutMs) {
        return jdbcTemplate.query(
                RECOVER_SQL,
                (rs, rowNum) -> new RecoveredJob(
                        rs.getLong("id"),
                        JobStatus.valueOf(rs.getString("status"))),
                leaseTimeoutMs);
    }

    private final JdbcTemplate jdbcTemplate;
    private final JobRowMapper rowMapper;
    private static final String COUNT_SQL = "SELECT COUNT(*) FROM jobs";
    private static final String COUNT_BY_STATUS_SQL = "SELECT COUNT(*) FROM jobs WHERE status = ?";

    public JobRepository(JdbcTemplate jdbcTemplate, JobRowMapper rowMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.rowMapper = rowMapper;
    }

    public Job insert(String taskType, String payload, int maxAttempts) {
        return jdbcTemplate.queryForObject(INSERT_SQL, rowMapper, taskType, payload, maxAttempts);
    }

    public Optional<Job> findById(long id) {
        List<Job> jobs = jdbcTemplate.query(FIND_BY_ID_SQL, rowMapper, id);
        return jobs.stream().findFirst();
    }

    public List<Job> findAll(JobStatus status, int limit, long offset) {
        if (status == null) {
            return jdbcTemplate.query(LIST_SQL, rowMapper, limit, offset);
        }
        return jdbcTemplate.query(LIST_BY_STATUS_SQL, rowMapper, status.name(), limit, offset);
    }

    public long count(JobStatus status) {
        Long total = (status == null)
                ? jdbcTemplate.queryForObject(COUNT_SQL, Long.class)
                : jdbcTemplate.queryForObject(COUNT_BY_STATUS_SQL, Long.class, status.name());
        return total == null ? 0 : total;
    }
}