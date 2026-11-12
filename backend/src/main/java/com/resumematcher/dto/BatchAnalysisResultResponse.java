
package com.resumematcher.dto;

import com.resumematcher.entity.enums.AnalysisStatus;
import com.resumematcher.integration.fastapi.dto.AnalyzeResponse;

import java.time.Instant;
import java.util.List;

public record BatchAnalysisResultResponse(
        Long analysisId,
        Long resumeId,
        String filename,
        Double score,
        String scoreCategory,
        AnalysisStatus status,

        List<AnalyzeResponse.ScoreComponent> components,
        List<AnalyzeResponse.MatchedSkill> matchedSkills,
        List<AnalyzeResponse.MissingSkill> missingSkills,
        List<String> additionalSkills,
        List<AnalyzeResponse.KeywordItem> keywords,
        List<AnalyzeResponse.StructureCheckItem> structureChecks,
        List<String> warnings,
        String disclaimer,

        String errorCode,
        String errorMessage,
        Instant analyzedAt,
        Instant expiresAt
) {
}
