package com.resumematcher.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnalysisSessionCreateRequest(

        @NotBlank(message = "Session name is required")
        @Size(max = 100, message = "Session name cannot exceed 100 characters")
        String name

) {
}