package com.resumematcher.dto;

import com.resumematcher.entity.enums.AnalysisStatus;
import java.time.Instant;

public record AnalysisResponse(
        Long id,
        Long resumeId,
        Double score,
        AnalysisStatus status,
        String resultJson,
        String errorCode,
        String errorMessage,
        Instant analyzedAt,
        Instant expiresAt
) {
}
