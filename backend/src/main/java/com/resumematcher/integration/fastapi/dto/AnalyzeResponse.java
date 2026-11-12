
package com.resumematcher.integration.fastapi.dto;

import java.util.List;

public record AnalyzeResponse(
        double score,
        List<ScoreComponent> components,
        List<MatchedSkill> matchedSkills,
        List<MissingSkill> missingSkills,
        List<String> additionalSkills,
        List<KeywordItem> keywords,
        List<StructureCheckItem> structureChecks,
        List<String> warnings,
        String disclaimer
) {

    public record ScoreComponent(
            String key,
            String label,
            double weight,
            double ratio,
            double points,
            boolean available,
            String detail
    ) {
    }

    public record MatchedSkill(
            String name,
            String category,
            int resumeCount,
            int jobCount
    ) {
    }

    public record MissingSkill(
            String name,
            String category,
            int jobCount
    ) {
    }

    public record KeywordItem(
            String term,
            boolean presentInResume
    ) {
    }

    public record StructureCheckItem(
            String name,
            boolean passed,
            double points,
            double maxPoints,
            String detail
    ) {
    }
}
