package com.vipul.claimq.dto;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.vipul.claimq.model.Job;
import com.vipul.claimq.model.JobStatus;

import java.time.Instant;

public record JobResponse(
        Long id,
        String taskType,
        @JsonRawValue String payload,
        JobStatus status,
        int attempts,
        int maxAttempts,
        Instant scheduledAt,
        String lastError,
        Instant createdAt,
        Instant updatedAt
) {

    public static JobResponse from(Job job) {
        return new JobResponse(
                job.id(),
                job.taskType(),
                job.payload(),
                job.status(),
                job.attempts(),
                job.maxAttempts(),
                job.scheduledAt(),
                job.lastError(),
                job.createdAt(),
                job.updatedAt()
        );
    }
}