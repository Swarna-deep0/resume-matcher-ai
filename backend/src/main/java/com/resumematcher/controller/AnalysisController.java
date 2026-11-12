package com.resumematcher.controller;

import com.resumematcher.dto.AnalysisRequest;
import com.resumematcher.dto.AnalysisResponse;
import com.resumematcher.service.AnalysisService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analyses")
public class AnalysisController {

    private final AnalysisService analysisService;

    public AnalysisController(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AnalysisResponse analyzeResume(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AnalysisRequest request
    ) {
        Long userId = Long.valueOf(jwt.getSubject());

        return analysisService.analyzeResume(
                userId,
                request.resumeId(),
                request.jobDescriptionId()
        );
    }
}