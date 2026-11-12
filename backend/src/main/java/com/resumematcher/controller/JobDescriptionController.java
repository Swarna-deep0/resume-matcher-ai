package com.resumematcher.controller;

import com.resumematcher.dto.JobDescriptionRequest;
import com.resumematcher.dto.JobDescriptionResponse;
import com.resumematcher.service.JobDescriptionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/job-descriptions")
public class JobDescriptionController {

    private final JobDescriptionService jobDescriptionService;

    public JobDescriptionController(JobDescriptionService jobDescriptionService) {
        this.jobDescriptionService = jobDescriptionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public JobDescriptionResponse create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody JobDescriptionRequest request) {

        return jobDescriptionService.create(
                Long.valueOf(jwt.getSubject()), request
        );
    }

    @GetMapping
    public List<JobDescriptionResponse> getAll(
            @AuthenticationPrincipal Jwt jwt) {

        return jobDescriptionService.getAll(
                Long.valueOf(jwt.getSubject())
        );
    }

    @GetMapping("/{id}")
    public JobDescriptionResponse getById(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id) {

        return jobDescriptionService.getById(
                Long.valueOf(jwt.getSubject()), id
        );
    }

    @PutMapping("/{id}")
    public JobDescriptionResponse update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @Valid @RequestBody JobDescriptionRequest request) {

        return jobDescriptionService.update(
                Long.valueOf(jwt.getSubject()), id, request
        );
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id) {

        jobDescriptionService.delete(
                Long.valueOf(jwt.getSubject()), id
        );
    }
}