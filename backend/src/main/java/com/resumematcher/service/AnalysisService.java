package com.resumematcher.service;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import com.resumematcher.dto.AnalysisResponse;
import com.resumematcher.dto.ExtractedResumeText;
import com.resumematcher.entity.Analysis;
import com.resumematcher.entity.AnalysisBatch;
import com.resumematcher.entity.JobDescription;
import com.resumematcher.entity.Resume;
import com.resumematcher.exception.AiServiceUnavailableException;
import com.resumematcher.integration.fastapi.FastApiAnalysisClient;
import com.resumematcher.integration.fastapi.dto.AnalyzeResponse;
import com.resumematcher.repository.AnalysisBatchRepository;
import com.resumematcher.repository.AnalysisRepository;
import com.resumematcher.repository.JobDescriptionRepository;
import com.resumematcher.repository.ResumeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AnalysisService {

    private final ResumeRepository resumeRepository;
    private final JobDescriptionRepository jobDescriptionRepository;
    private final AnalysisBatchRepository analysisBatchRepository;
    private final AnalysisRepository analysisRepository;
    private final FastApiAnalysisClient fastApiAnalysisClient;
    private final JsonMapper jsonMapper;

    public AnalysisService(
            ResumeRepository resumeRepository,
            JobDescriptionRepository jobDescriptionRepository,
            AnalysisBatchRepository analysisBatchRepository,
            AnalysisRepository analysisRepository,
            FastApiAnalysisClient fastApiAnalysisClient,
            JsonMapper jsonMapper
    ) {
        this.resumeRepository = resumeRepository;
        this.jobDescriptionRepository = jobDescriptionRepository;
        this.analysisBatchRepository = analysisBatchRepository;
        this.analysisRepository = analysisRepository;
        this.fastApiAnalysisClient = fastApiAnalysisClient;
        this.jsonMapper = jsonMapper;
    }

    // Existing single-resume analysis endpoint logic.
    @Transactional
    public AnalysisResponse analyzeResume(
            Long userId,
            Long resumeId,
            Long jobDescriptionId
    ) {
        Resume resume = resumeRepository
                .findByIdAndUser_Id(resumeId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Resume not found"
                ));

        JobDescription jobDescription = jobDescriptionRepository
                .findByIdAndUser_Id(jobDescriptionId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Job description not found"
                ));

        AnalysisBatch batch = analysisBatchRepository.save(
                new AnalysisBatch(resume.getUser(), jobDescription, 1)
        );

        AnalysisResponse response = processResume(
                batch,
                resume,
                jobDescription.getDescriptionText()
        );

        batch.finish();
        analysisBatchRepository.save(batch);

        return response;
    }

    // Processes one resume in an independently committed transaction.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AnalysisResponse processResumeInBatch(
            Long userId,
            Long batchId,
            Long resumeId
    ) {
        AnalysisBatch batch = analysisBatchRepository
                .findByIdAndUser_Id(batchId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Analysis batch not found"
                ));

        Resume resume = resumeRepository
                .findByIdAndUser_Id(resumeId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Resume not found"
                ));

        return processResume(
                batch,
                resume,
                batch.getJobDescription().getDescriptionText()
        );
    }

    // Shared processing logic for single and batch analysis.
    private AnalysisResponse processResume(
            AnalysisBatch batch,
            Resume resume,
            String jobDescriptionText
    ) {
        Analysis analysis = analysisRepository.save(
                new Analysis(batch, resume, jobDescriptionText)
        );

        try {
            ExtractedResumeText extractedResume = new ExtractedResumeText(
                    resume.getExtractedText(),
                    resume.getPageCount(),
                    resume.getCharacterCount()
            );

            AnalyzeResponse aiResult = fastApiAnalysisClient.analyze(
                    extractedResume,
                    jobDescriptionText
            );

            String resultJson = jsonMapper.writeValueAsString(aiResult);

            analysis.markCompleted(aiResult.score(), resultJson);
            batch.recordCompletedAnalysis();

        } catch (AiServiceUnavailableException exception) {

            analysis.markFailed(
                    "AI_SERVICE_UNAVAILABLE",
                    exception.getMessage()
            );

            batch.recordFailedAnalysis();

        } catch (JacksonException exception) {

            analysis.markFailed(
                    "RESULT_SERIALIZATION_FAILED",
                    "Unable to serialize the AI analysis result"
            );

            batch.recordFailedAnalysis();

        } catch (IllegalArgumentException exception) {

            analysis.markFailed(
                    "INVALID_ANALYSIS_INPUT",
                    "Resume or job description contains invalid analysis input"
            );

            batch.recordFailedAnalysis();
        }

        analysisRepository.save(analysis);
        analysisBatchRepository.save(batch);

        return toResponse(analysis);
    }

    private AnalysisResponse toResponse(Analysis analysis) {
        return new AnalysisResponse(
                analysis.getId(),
                analysis.getResume().getId(),
                analysis.getScore(),
                analysis.getStatus(),
                analysis.getResultJson(),
                analysis.getErrorCode(),
                analysis.getErrorMessage(),
                analysis.getAnalyzedAt(),
                analysis.getExpiresAt()
        );
    }
}