package com.resumematcher.controller;

import com.resumematcher.dto.AnalysisSessionCreateRequest;
import com.resumematcher.dto.AnalysisSessionResponse;
import com.resumematcher.service.AnalysisSessionService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/analyses/sessions")
public class AnalysisSessionController {

    private final AnalysisSessionService analysisSessionService;

    public AnalysisSessionController(
            AnalysisSessionService analysisSessionService
    ) {
        this.analysisSessionService = analysisSessionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AnalysisSessionResponse createSession(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AnalysisSessionCreateRequest request
    ) {
        Long userId = Long.valueOf(jwt.getSubject());

        return analysisSessionService.createSession(userId, request);
    }

    @GetMapping
    public List<AnalysisSessionResponse> getAllSessions(
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = Long.valueOf(jwt.getSubject());

        return analysisSessionService.getAllSessions(userId);
    }

    @GetMapping("/{sessionId}")
    public AnalysisSessionResponse getSessionById(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long sessionId
    ) {
        Long userId = Long.valueOf(jwt.getSubject());

        return analysisSessionService.getSessionById(userId, sessionId);
    }
}