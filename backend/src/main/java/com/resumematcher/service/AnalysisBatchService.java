package com.resumematcher.service;

import com.resumematcher.dto.AnalysisBatchRequest;
import com.resumematcher.dto.AnalysisBatchResponse;
import com.resumematcher.dto.BatchAnalysisResultResponse;
import com.resumematcher.dto.BatchResultsResponse;
import com.resumematcher.entity.Analysis;
import com.resumematcher.entity.AnalysisBatch;
import com.resumematcher.entity.AnalysisSession;
import com.resumematcher.entity.JobDescription;
import com.resumematcher.entity.Resume;
import com.resumematcher.entity.SessionResume;
import com.resumematcher.entity.User;
import com.resumematcher.integration.fastapi.dto.AnalyzeResponse;
import com.resumematcher.repository.AnalysisBatchRepository;
import com.resumematcher.repository.AnalysisRepository;
import com.resumematcher.repository.AnalysisSessionRepository;
import com.resumematcher.repository.JobDescriptionRepository;
import com.resumematcher.repository.ResumeRepository;
import com.resumematcher.repository.SessionResumeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
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
    private final AnalysisSessionRepository analysisSessionRepository;
    private final SessionResumeRepository sessionResumeRepository;
    private final TransactionTemplate transactionTemplate;

    public AnalysisBatchService(
            AnalysisBatchRepository analysisBatchRepository,
            JobDescriptionRepository jobDescriptionRepository,
            ResumeRepository resumeRepository,
            UserService userService,
            AnalysisService analysisService,
            AnalysisRepository analysisRepository,
            JsonMapper jsonMapper,
            AnalysisSessionRepository analysisSessionRepository,
            SessionResumeRepository sessionResumeRepository,
            PlatformTransactionManager transactionManager
    ) {
        this.analysisBatchRepository = analysisBatchRepository;
        this.jobDescriptionRepository = jobDescriptionRepository;
        this.resumeRepository = resumeRepository;
        this.userService = userService;
        this.analysisService = analysisService;
        this.analysisRepository = analysisRepository;
        this.jsonMapper = jsonMapper;
        this.analysisSessionRepository = analysisSessionRepository;
        this.sessionResumeRepository = sessionResumeRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Transactional(readOnly = true)
    public List<AnalysisBatchResponse> getUserBatches(Long userId) {

        return analysisBatchRepository
                .findAllByUser_IdOrderByCreatedAtDesc(userId)
                .stream()
                .map(AnalysisBatchResponse::from)
                .toList();
    }

    public AnalysisBatchResponse analyzeBatch(
            Long userId,
            AnalysisBatchRequest request
    ) {

        // Validate duplicate resume IDs.
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

        // Capture the job description text exactly once.
        String jobDescriptionSnapshot = jobDescription.getDescriptionText();

        // Verify ownership of all resumes before anything is created.
        List<Resume> resumes = new ArrayList<>();

        for (Long resumeId : request.resumeIds()) {
            Resume resume = resumeRepository
                    .findByIdAndUser_Id(resumeId, userId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Resume not found: " + resumeId
                    ));

            resumes.add(resume);
        }

        User user = userService.getById(userId);

        // Create the batch (and the session associations) in one short transaction.
        // It commits before any AI call is made.
        Long batchId = createBatch(
                userId,
                user,
                jobDescription,
                resumes,
                request.sessionId()
        );

        // Process every resume using the same JD snapshot.
        for (Long resumeId : request.resumeIds()) {
            analysisService.processResumeInBatch(
                    userId,
                    batchId,
                    resumeId,
                    jobDescriptionSnapshot
            );
        }

        // Retrieve the updated batch.
        AnalysisBatch completedBatch = analysisBatchRepository
                .findByIdAndUser_Id(batchId, userId)
                .orElseThrow(() -> new IllegalStateException(
                        "Analysis batch could not be retrieved"
                ));

        completedBatch.finish();

        AnalysisBatch savedBatch =
                analysisBatchRepository.save(completedBatch);

        return AnalysisBatchResponse.from(savedBatch);
    }

    // Creates the batch and, when a session is supplied, links the session to it.
    // A missing or foreign session throws 404 and rolls everything back, so no
    // batch exists and no AI analysis starts.
    private Long createBatch(
            Long userId,
            User user,
            JobDescription jobDescription,
            List<Resume> resumes,
            Long sessionId
    ) {
        return transactionTemplate.execute(status -> {

            AnalysisSession session = null;

            if (sessionId != null) {
                session = analysisSessionRepository
                        .findByIdAndUser_Id(sessionId, userId)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Analysis session not found"
                        ));
            }

            AnalysisBatch batch = analysisBatchRepository.save(
                    new AnalysisBatch(
                            user,
                            jobDescription,
                            session,
                            resumes.size()
                    )
            );

            if (session != null) {
                for (Resume resume : resumes) {
                    boolean alreadyAssociated = sessionResumeRepository
                            .existsBySession_IdAndResume_Id(
                                    session.getId(),
                                    resume.getId()
                            );

                    if (!alreadyAssociated) {
                        sessionResumeRepository.save(
                                new SessionResume(session, resume)
                        );
                    }
                }

                session.markUpdated();
            }

            return batch.getId();
        });
    }

    @Transactional(readOnly = true)
    public BatchResultsResponse getBatchResults(
            Long userId,
            Long batchId
    ) {

        // Verify batch ownership.
        AnalysisBatch batch = analysisBatchRepository
                .findByIdAndUser_Id(batchId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Analysis batch not found"
                ));

        // Retrieve all results for this batch.
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

    private BatchAnalysisResultResponse toBatchAnalysisResult(
            Analysis analysis
    ) {

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