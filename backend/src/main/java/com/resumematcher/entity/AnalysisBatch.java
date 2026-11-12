package com.resumematcher.entity;

import com.resumematcher.entity.enums.BatchStatus;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "analysis_batches")
public class AnalysisBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_description_id", nullable = false)
    private JobDescription jobDescription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BatchStatus status;

    @Column(name = "total_resumes", nullable = false)
    private Integer totalResumes;

    @Column(name = "completed_count", nullable = false)
    private Integer completedCount;

    @Column(name = "failed_count", nullable = false)
    private Integer failedCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected AnalysisBatch() {
    }

    public AnalysisBatch(
            User user,
            JobDescription jobDescription,
            Integer totalResumes
    ) {
        this.user = user;
        this.jobDescription = jobDescription;
        this.totalResumes = totalResumes;
        this.completedCount = 0;
        this.failedCount = 0;
        this.status = BatchStatus.PROCESSING;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public void recordCompletedAnalysis() {
        this.completedCount++;
    }

    public void recordFailedAnalysis() {
        this.failedCount++;
    }

    public void finish() {
        if (failedCount == totalResumes) {
            this.status = BatchStatus.FAILED;
        } else if (failedCount > 0) {
            this.status = BatchStatus.COMPLETED_WITH_ERRORS;
        } else {
            this.status = BatchStatus.COMPLETED;
        }

        this.completedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public JobDescription getJobDescription() {
        return jobDescription;
    }

    public BatchStatus getStatus() {
        return status;
    }

    public Integer getTotalResumes() {
        return totalResumes;
    }

    public Integer getCompletedCount() {
        return completedCount;
    }

    public Integer getFailedCount() {
        return failedCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }
}