package com.resumematcher.controller;

import com.resumematcher.dto.ExtractedResumeText;
import com.resumematcher.dto.ResumeUploadResponse;
import com.resumematcher.service.ResumeService;
import com.resumematcher.service.ResumeTextService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/resumes")
public class ResumeController {

    private final ResumeTextService resumeTextService;
    private final ResumeService resumeService;

    public ResumeController(
            ResumeTextService resumeTextService,
            ResumeService resumeService) {

        this.resumeTextService = resumeTextService;
        this.resumeService = resumeService;
    }

    // Existing extraction endpoint; preserved.
    @PostMapping(
            value = "/extract",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ExtractedResumeText extract(
            @RequestParam("file") MultipartFile file) {

        return resumeTextService.extractText(file);
    }

    // New authenticated upload and persistence endpoint.
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ResumeUploadResponse upload(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam("file") MultipartFile file) {

        Long userId = Long.valueOf(jwt.getSubject());

        return resumeService.uploadResume(userId, file);
    }
}