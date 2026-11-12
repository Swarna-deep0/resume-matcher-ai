package com.resumematcher.controller;

import com.resumematcher.dto.AnalysisBatchRequest;
import com.resumematcher.dto.AnalysisBatchResponse;
import com.resumematcher.dto.BatchResultsResponse;
import com.resumematcher.service.AnalysisBatchService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/analyses/batches")
public class AnalysisBatchController {

    private final AnalysisBatchService analysisBatchService;

    public AnalysisBatchController(AnalysisBatchService analysisBatchService) {
        this.analysisBatchService = analysisBatchService;
    }

    @GetMapping
    public List<AnalysisBatchResponse> getUserBatches(
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = Long.valueOf(jwt.getSubject());

        return analysisBatchService.getUserBatches(userId);
    }

    @GetMapping("/{batchId}/results")
    public BatchResultsResponse getBatchResults(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long batchId
    ) {
        Long userId = Long.valueOf(jwt.getSubject());

        return analysisBatchService.getBatchResults(userId, batchId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AnalysisBatchResponse analyzeBatch(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AnalysisBatchRequest request
    ) {
        Long userId = Long.valueOf(jwt.getSubject());

        return analysisBatchService.analyzeBatch(userId, request);
    }
}