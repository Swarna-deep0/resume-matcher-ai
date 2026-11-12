package com.resumematcher.dto;

import java.time.Instant;

public record JobDescriptionResponse(
        Long id,
        String title,
        String companyName,
        String descriptionText,
        Instant createdAt,
        Instant updatedAt
) {
}
