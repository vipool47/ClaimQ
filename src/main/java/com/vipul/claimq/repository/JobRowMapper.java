package com.vipul.claimq.repository;

import com.vipul.claimq.model.Job;
import com.vipul.claimq.model.JobStatus;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

@Component
public class JobRowMapper implements RowMapper<Job> {

    @Override
    public Job mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Job(
                rs.getLong("id"),
                rs.getString("task_type"),
                rs.getString("payload"),
                JobStatus.valueOf(rs.getString("status")),
                rs.getInt("attempts"),
                rs.getInt("max_attempts"),
                toInstant(rs, "scheduled_at"),
                rs.getString("locked_by"),
                toInstant(rs, "locked_at"),
                rs.getObject("claim_token", UUID.class),
                rs.getString("last_error"),
                toInstant(rs, "created_at"),
                toInstant(rs, "updated_at")
        );
    }

    private Instant toInstant(ResultSet rs, String column) throws SQLException {
        OffsetDateTime value = rs.getObject(column, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }
}