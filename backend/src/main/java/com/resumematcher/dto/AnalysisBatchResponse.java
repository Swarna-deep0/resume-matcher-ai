package com.resumematcher.dto;

import com.resumematcher.entity.enums.BatchStatus;
import java.time.Instant;

public record AnalysisBatchResponse(
        Long id,
        Long jobDescriptionId,
        BatchStatus status,
        int totalResumes,
        int completedCount,
        int failedCount,
        Instant createdAt,
        Instant completedAt
) {
}
