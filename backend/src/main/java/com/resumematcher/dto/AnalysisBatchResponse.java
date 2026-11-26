package com.resumematcher.dto;

import com.resumematcher.entity.AnalysisBatch;
import com.resumematcher.entity.enums.BatchStatus;

import java.time.Instant;

public record AnalysisBatchResponse(
        Long id,
        Long jobDescriptionId,
        Long sessionId,
        BatchStatus status,
        int totalResumes,
        int completedCount,
        int failedCount,
        Instant createdAt,
        Instant completedAt
) {

    // The single place that maps a batch to its summary, so every endpoint
    // returns the same fields. sessionId is null for batches created without a session.
    public static AnalysisBatchResponse from(AnalysisBatch batch) {
        return new AnalysisBatchResponse(
                batch.getId(),
                batch.getJobDescription().getId(),
                batch.getSession() != null ? batch.getSession().getId() : null,
                batch.getStatus(),
                batch.getTotalResumes(),
                batch.getCompletedCount(),
                batch.getFailedCount(),
                batch.getCreatedAt(),
                batch.getCompletedAt()
        );
    }
}