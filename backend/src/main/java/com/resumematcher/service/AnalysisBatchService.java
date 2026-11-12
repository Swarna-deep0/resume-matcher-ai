package com.resumematcher.service;

import com.resumematcher.dto.AnalysisBatchRequest;
import com.resumematcher.dto.AnalysisBatchResponse;
import com.resumematcher.dto.BatchAnalysisResultResponse;
import com.resumematcher.dto.BatchResultsResponse;
import com.resumematcher.entity.Analysis;
import com.resumematcher.entity.AnalysisBatch;
import com.resumematcher.entity.JobDescription;
import com.resumematcher.entity.Resume;
import com.resumematcher.entity.User;
import com.resumematcher.integration.fastapi.dto.AnalyzeResponse;
import com.resumematcher.repository.AnalysisBatchRepository;
import com.resumematcher.repository.AnalysisRepository;
import com.resumematcher.repository.JobDescriptionRepository;
import com.resumematcher.repository.ResumeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class AnalysisBatchService {

    private final AnalysisBatchRepository analysisBatchRepository;
    private final JobDescriptionRepository jobDescriptionRepository;
    private final ResumeRepository resumeRepository;
    private final UserService userService;
    private final AnalysisService analysisService;
    private final AnalysisRepository analysisRepository;
    private final JsonMapper jsonMapper;

    public AnalysisBatchService(AnalysisBatchRepository analysisBatchRepository,
            JobDescriptionRepository jobDescriptionRepository,
            ResumeRepository resumeRepository,
            UserService userService,
            AnalysisService analysisService,
            AnalysisRepository analysisRepository,
            JsonMapper jsonMapper) {
        this.analysisBatchRepository = analysisBatchRepository;
        this.jobDescriptionRepository = jobDescriptionRepository;
        this.resumeRepository = resumeRepository;
        this.userService = userService;
        this.analysisService = analysisService;
        this.analysisRepository = analysisRepository;
        this.jsonMapper = jsonMapper;
    }

    @Transactional(readOnly = true)
    public List<AnalysisBatchResponse> getUserBatches(Long userId) {

        return analysisBatchRepository
                .findAllByUser_IdOrderByCreatedAtDesc(userId)
                .stream()
                .map(batch -> new AnalysisBatchResponse(
                        batch.getId(),
                        batch.getJobDescription().getId(),
                        batch.getStatus(),
                        batch.getTotalResumes(),
                        batch.getCompletedCount(),
                        batch.getFailedCount(),
                        batch.getCreatedAt(),
                        batch.getCompletedAt()
                ))
                .toList();
    }

    public AnalysisBatchResponse analyzeBatch(
            Long userId,
            AnalysisBatchRequest request
    ) {
        // Reject duplicate resume IDs.
        Set<Long> uniqueResumeIds = new HashSet<>(request.resumeIds());

        if (uniqueResumeIds.size() != request.resumeIds().size()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Duplicate resume IDs are not allowed"
            );
        }

        // Verify job description ownership.
        JobDescription jobDescription = jobDescriptionRepository
                .findByIdAndUser_Id(request.jobDescriptionId(), userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Job description not found"
                ));

        // Verify ownership of every resume before starting.
        for (Long resumeId : request.resumeIds()) {
            resumeRepository.findByIdAndUser_Id(resumeId, userId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Resume not found: " + resumeId
                    ));
        }

        User user = userService.getById(userId);

        // Save the batch before processing individual resumes.
        AnalysisBatch batch = analysisBatchRepository.save(
                new AnalysisBatch(
                        user,
                        jobDescription,
                        request.resumeIds().size()
                )
        );

        // Process each resume in its own transaction.
        for (Long resumeId : request.resumeIds()) {
            analysisService.processResumeInBatch(
                    userId,
                    batch.getId(),
                    resumeId
            );
        }

        // Finish the batch after all individual analyses are recorded.
        AnalysisBatch completedBatch = analysisBatchRepository
                .findByIdAndUser_Id(batch.getId(), userId)
                .orElseThrow(() -> new IllegalStateException(
                        "Analysis batch could not be retrieved"
                ));

        completedBatch.finish();

        AnalysisBatch savedBatch = analysisBatchRepository.save(completedBatch);

        return new AnalysisBatchResponse(
                savedBatch.getId(),
                savedBatch.getJobDescription().getId(),
                savedBatch.getStatus(),
                savedBatch.getTotalResumes(),
                savedBatch.getCompletedCount(),
                savedBatch.getFailedCount(),
                savedBatch.getCreatedAt(),
                savedBatch.getCompletedAt()
        );
    }

    @Transactional(readOnly = true)
    public BatchResultsResponse getBatchResults(Long userId, Long batchId) {
        // Verify that the batch belongs to the authenticated user.
        AnalysisBatch batch = analysisBatchRepository
                .findByIdAndUser_Id(batchId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Analysis batch not found"
                ));

        // Retrieve all analyses for this batch, ordered by score.
        List<Analysis> analyses = analysisRepository
                .findAllByBatch_IdAndBatch_User_IdOrderByScoreDescAnalyzedAtAsc(
                        batchId,
                        userId
                );

        List<BatchAnalysisResultResponse> results = analyses.stream()
                .map(this::toBatchAnalysisResult)
                .toList();

        return new BatchResultsResponse(
                batch.getId(),
                batch.getJobDescription().getId(),
                batch.getStatus(),
                batch.getTotalResumes(),
                batch.getCompletedCount(),
                batch.getFailedCount(),
                batch.getCreatedAt(),
                batch.getCompletedAt(),
                results
        );
    }

    private BatchAnalysisResultResponse toBatchAnalysisResult(Analysis analysis) {
        AnalyzeResponse aiResult = null;

        if (analysis.getResultJson() != null
                && !analysis.getResultJson().isBlank()) {
            try {
                aiResult = jsonMapper.readValue(
                        analysis.getResultJson(),
                        AnalyzeResponse.class
                );
            } catch (JacksonException exception) {
                throw new IllegalStateException(
                        "Unable to deserialize analysis result for analysis ID: "
                                + analysis.getId(),
                        exception
                );
            }
        }

        return new BatchAnalysisResultResponse(
                analysis.getId(),
                analysis.getResume().getId(),
                analysis.getResume().getOriginalFilename(),
                analysis.getScore(),
                getScoreCategory(analysis.getScore()),
                analysis.getStatus(),
                aiResult != null ? aiResult.components() : null,
                aiResult != null ? aiResult.matchedSkills() : null,
                aiResult != null ? aiResult.missingSkills() : null,
                aiResult != null ? aiResult.additionalSkills() : null,
                aiResult != null ? aiResult.keywords() : null,
                aiResult != null ? aiResult.structureChecks() : null,
                aiResult != null ? aiResult.warnings() : null,
                aiResult != null ? aiResult.disclaimer() : null,
                analysis.getErrorCode(),
                analysis.getErrorMessage(),
                analysis.getAnalyzedAt(),
                analysis.getExpiresAt()
        );
    }

    private String getScoreCategory(Double score) {
        if (score == null) {
            return null;
        }

        if (score >= 75) {
            return "GREEN";
        }

        if (score >= 55) {
            return "YELLOW";
        }

        return "RED";
    }
}