package com.resumematcher.service;

import com.resumematcher.dto.JobDescriptionRequest;
import com.resumematcher.dto.JobDescriptionResponse;
import com.resumematcher.entity.JobDescription;
import com.resumematcher.entity.User;
import com.resumematcher.repository.AnalysisBatchRepository;
import com.resumematcher.repository.JobDescriptionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class JobDescriptionService {

    private final JobDescriptionRepository jobDescriptionRepository;
    private final AnalysisBatchRepository analysisBatchRepository;
    private final UserService userService;

    public JobDescriptionService(
            JobDescriptionRepository jobDescriptionRepository,
            AnalysisBatchRepository analysisBatchRepository,
            UserService userService) {

        this.jobDescriptionRepository = jobDescriptionRepository;
        this.analysisBatchRepository = analysisBatchRepository;
        this.userService = userService;
    }

    @Transactional
    public JobDescriptionResponse create(Long userId, JobDescriptionRequest request) {

        User user = userService.getById(userId);

        JobDescription jobDescription = new JobDescription(
                user,
                request.title().trim(),
                normalizeCompany(request.companyName()),
                request.descriptionText().trim()
        );

        return toResponse(jobDescriptionRepository.save(jobDescription));
    }

    @Transactional(readOnly = true)
    public List<JobDescriptionResponse> getAll(Long userId) {

        return jobDescriptionRepository
                .findAllByUser_IdOrderByUpdatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public JobDescriptionResponse getById(Long userId, Long jobDescriptionId) {

        return toResponse(findOwnedJobDescription(userId, jobDescriptionId));
    }

    @Transactional
    public JobDescriptionResponse update(
            Long userId,
            Long jobDescriptionId,
            JobDescriptionRequest request) {

        JobDescription jobDescription =
                findOwnedJobDescription(userId, jobDescriptionId);

        jobDescription.updateDetails(
                request.title().trim(),
                normalizeCompany(request.companyName()),
                request.descriptionText().trim()
        );

        return toResponse(jobDescriptionRepository.save(jobDescription));
    }

    @Transactional
    public void delete(Long userId, Long jobDescriptionId) {

        JobDescription jobDescription =
                findOwnedJobDescription(userId, jobDescriptionId);

        if (analysisBatchRepository.existsByJobDescription_Id(jobDescriptionId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This job description is already used in an analysis batch"
            );
        }

        jobDescriptionRepository.delete(jobDescription);
    }

    private JobDescription findOwnedJobDescription(
            Long userId,
            Long jobDescriptionId) {

        return jobDescriptionRepository
                .findByIdAndUser_Id(jobDescriptionId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Job description not found"
                ));
    }

    private String normalizeCompany(String companyName) {
        if (companyName == null || companyName.isBlank()) {
            return null;
        }
        return companyName.trim();
    }

    private JobDescriptionResponse toResponse(JobDescription jobDescription) {

        return new JobDescriptionResponse(
                jobDescription.getId(),
                jobDescription.getTitle(),
                jobDescription.getCompanyName(),
                jobDescription.getDescriptionText(),
                jobDescription.getCreatedAt(),
                jobDescription.getUpdatedAt()
        );
    }
}