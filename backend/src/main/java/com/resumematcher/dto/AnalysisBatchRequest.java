package com.resumematcher.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AnalysisBatchRequest(

        @NotNull(message = "Job description ID is required")
        Long jobDescriptionId,

        @NotEmpty(message = "At least one resume ID is required")
        @Size(max = 5, message = "A batch can contain at most 5 resumes")
        List<@NotNull(message = "Resume ID cannot be null") Long> resumeIds

) {
}