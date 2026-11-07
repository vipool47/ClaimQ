package com.vipul.claimq.repository;

import com.vipul.claimq.model.Job;
import com.vipul.claimq.model.JobStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

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