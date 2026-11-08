package com.vipul.claimq.model;

import java.time.Instant;
import java.util.UUID;

public record Job(
        Long id,
        String taskType,
        String payload,
        JobStatus status,
        int attempts,
        int maxAttempts,
        Instant scheduledAt,
        String lockedBy,
        Instant lockedAt,
        UUID claimToken,
        String lastError,
        Instant createdAt,
        Instant updatedAt
) {
}

