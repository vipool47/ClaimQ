package com.vipul.claimq.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import tools.jackson.databind.JsonNode;

public record CreateJobRequest(

        @NotBlank
        @Size(max = 100)
        String taskType,

        @NotNull
        JsonNode payload,

        @Min(1)
        @Max(10)
        Integer maxAttempts
) {
}