package com.resumematcher.dto;

import jakarta.validation.constraints.NotNull;

public record AnalysisRequest(

        @NotNull(message = "Resume ID is required")
        Long resumeId,

        @NotNull(message = "Job description ID is required")
        Long jobDescriptionId

) {
}