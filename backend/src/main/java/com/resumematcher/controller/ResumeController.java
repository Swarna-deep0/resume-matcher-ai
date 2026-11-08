package com.resumematcher.controller;

import com.resumematcher.dto.ExtractedResumeText;
import com.resumematcher.service.ResumeTextService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/resumes")
public class ResumeController {

    private final ResumeTextService resumeTextService;

    public ResumeController(ResumeTextService resumeTextService) {
        this.resumeTextService = resumeTextService;
    }

    @PostMapping(value = "/extract", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ExtractedResumeText extract(@RequestParam("file") MultipartFile file) {
        return resumeTextService.extractText(file);
    }
}