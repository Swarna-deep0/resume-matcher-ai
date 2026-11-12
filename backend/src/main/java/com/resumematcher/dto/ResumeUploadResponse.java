package com.resumematcher.dto;

import java.time.Instant;

public record ResumeUploadResponse(
        Long id,
        String originalFilename,
        String contentType,
        Long fileSizeBytes,
        int pageCount,
        int characterCount,
        Instant createdAt
) {
}
