package com.resumematcher.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JobDescriptionRequest(

        @NotBlank(message = "Job title is required")
        @Size(max = 150, message = "Job title cannot exceed 150 characters")
        String title,

        @Size(max = 150, message = "Company name cannot exceed 150 characters")
        String companyName,

        @NotBlank(message = "Job description cannot be empty")
        @Size(max = 50000, message = "Job description cannot exceed 50000 characters")
        String descriptionText

) {
}
