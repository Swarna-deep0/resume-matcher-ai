
package com.resumematcher.integration.fastapi.dto;

public record AnalyzeRequest(
        String resumeText,
        String jobDescriptionText
) {
}
