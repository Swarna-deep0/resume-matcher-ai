package com.resumematcher.service;

import com.resumematcher.dto.AnalysisBatchResponse;
import com.resumematcher.dto.AnalysisSessionCreateRequest;
import com.resumematcher.dto.AnalysisSessionResponse;
import com.resumematcher.entity.AnalysisBatch;
import com.resumematcher.entity.AnalysisSession;
import com.resumematcher.entity.JobDescription;
import com.resumematcher.entity.Resume;
import com.resumematcher.entity.SessionResume;
import com.resumematcher.entity.User;
import com.resumematcher.repository.AnalysisBatchRepository;
import com.resumematcher.repository.AnalysisSessionRepository;
import com.resumematcher.repository.JobDescriptionRepository;
import com.resumematcher.repository.SessionResumeRepository;
import com.resumematcher.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class AnalysisSessionService {

    private final AnalysisSessionRepository analysisSessionRepository;
    private final SessionResumeRepository sessionResumeRepository;
    private final AnalysisBatchRepository analysisBatchRepository;
    private final UserRepository userRepository;

    public AnalysisSessionService(
            AnalysisSessionRepository analysisSessionRepository,
            SessionResumeRepository sessionResumeRepository,
            AnalysisBatchRepository analysisBatchRepository,
            UserRepository userRepository
    ) {
        this.analysisSessionRepository = analysisSessionRepository;
        this.sessionResumeRepository = sessionResumeRepository;
        this.analysisBatchRepository = analysisBatchRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public AnalysisSessionResponse createSession(
            Long userId,
            AnalysisSessionCreateRequest request
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));

        String sessionName = request.name().trim();

        AnalysisSession session = new AnalysisSession(user, sessionName);

        AnalysisSession savedSession =
                analysisSessionRepository.save(session);

        return toResponse(savedSession);
    }

    @Transactional(readOnly = true)
    public List<AnalysisSessionResponse> getAllSessions(Long userId) {

        return analysisSessionRepository
                .findAllByUser_IdOrderByUpdatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AnalysisSessionResponse getSessionById(
            Long userId,
            Long sessionId
    ) {
        AnalysisSession session = analysisSessionRepository
                .findByIdAndUser_Id(sessionId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Analysis session not found"
                ));

        return toResponse(session);
    }

    private AnalysisSessionResponse toResponse(AnalysisSession session) {

        AnalysisSessionResponse.JobDescriptionSummary jobDescription =
                toJobDescriptionSummary(session.getJobDescription());

        List<AnalysisSessionResponse.ResumeSummary> resumes =
                sessionResumeRepository
                        .findAllBySession_IdOrderByCreatedAtAsc(session.getId())
                        .stream()
                        .map(SessionResume::getResume)
                        .map(this::toResumeSummary)
                        .toList();

        List<AnalysisBatchResponse> batches =
                analysisBatchRepository
                        .findAllBySession_IdOrderByCreatedAtDesc(session.getId())
                        .stream()
                        .map(this::toBatchResponse)
                        .toList();

        return new AnalysisSessionResponse(
                session.getId(),
                session.getName(),
                jobDescription,
                resumes,
                batches,
                session.getCreatedAt(),
                session.getUpdatedAt()
        );
    }

    private AnalysisSessionResponse.JobDescriptionSummary
    toJobDescriptionSummary(JobDescription jobDescription) {

        if (jobDescription == null) {
            return null;
        }

        return new AnalysisSessionResponse.JobDescriptionSummary(
                jobDescription.getId(),
                jobDescription.getTitle(),
                jobDescription.getCompanyName(),
                jobDescription.getCreatedAt(),
                jobDescription.getUpdatedAt()
        );
    }

    private AnalysisSessionResponse.ResumeSummary
    toResumeSummary(Resume resume) {

        return new AnalysisSessionResponse.ResumeSummary(
                resume.getId(),
                resume.getOriginalFilename(),
                resume.getContentType(),
                resume.getFileSizeBytes(),
                resume.getPageCount(),
                resume.getCharacterCount(),
                resume.getCreatedAt()
        );
    }

    private AnalysisBatchResponse toBatchResponse(AnalysisBatch batch) {

        return new AnalysisBatchResponse(
                batch.getId(),
                batch.getJobDescription().getId(),
                batch.getStatus(),
                batch.getTotalResumes(),
                batch.getCompletedCount(),
                batch.getFailedCount(),
                batch.getCreatedAt(),
                batch.getCompletedAt()
        );
    }
}