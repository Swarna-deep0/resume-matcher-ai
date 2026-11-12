
package com.resumematcher.dto;

import com.resumematcher.entity.enums.BatchStatus;

import java.time.Instant;
import java.util.List;

public record BatchResultsResponse(
        Long batchId,
        Long jobDescriptionId,
        BatchStatus status,
        int totalResumes,
        int completedCount,
        int failedCount,
        Instant createdAt,
        Instant completedAt,
        List<BatchAnalysisResultResponse> results
) {
}
