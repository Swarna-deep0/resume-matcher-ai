package com.resumematcher.dto;

import java.time.Instant;
import java.util.List;

public record AnalysisSessionResponse(

        Long id,
        String name,

        JobDescriptionSummary jobDescription,

        List<ResumeSummary> resumes,

        List<AnalysisBatchResponse> batches,

        Instant createdAt,
        Instant updatedAt

) {

    public record JobDescriptionSummary(
            Long id,
            String title,
            String companyName,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record ResumeSummary(
            Long id,
            String originalFilename,
            String contentType,
            Long fileSizeBytes,
            Integer pageCount,
            Integer characterCount,
            Instant createdAt
    ) {
    }
}