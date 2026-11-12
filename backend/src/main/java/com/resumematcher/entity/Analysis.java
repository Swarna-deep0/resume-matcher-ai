package com.resumematcher.entity;

import com.resumematcher.entity.enums.AnalysisStatus;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "analyses")
public class Analysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "batch_id", nullable = false)
    private AnalysisBatch batch;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resume_id", nullable = false)
    private Resume resume;

    @Lob
    @Column(name = "job_description_snapshot", nullable = false,
            columnDefinition = "LONGTEXT")
    private String jobDescriptionSnapshot;

    @Column(name = "score")
    private Double score;

    @Lob
    @Column(name = "result_json", columnDefinition = "LONGTEXT")
    private String resultJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AnalysisStatus status;

    @Column(name = "error_code", length = 100)
    private String errorCode;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "analyzed_at")
    private Instant analyzedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    protected Analysis() {
    }

    public Analysis(
            AnalysisBatch batch,
            Resume resume,
            String jobDescriptionSnapshot
    ) {
        this.batch = batch;
        this.resume = resume;
        this.jobDescriptionSnapshot = jobDescriptionSnapshot;
        this.status = AnalysisStatus.PENDING;
    }

    public void markCompleted(Double score, String resultJson) {
        this.score = score;
        this.resultJson = resultJson;
        this.status = AnalysisStatus.COMPLETED;
        this.analyzedAt = Instant.now();
        this.expiresAt = this.analyzedAt.plusSeconds(15L * 24 * 60 * 60);
        this.errorCode = null;
        this.errorMessage = null;
    }

    public void markFailed(String errorCode, String errorMessage) {
        this.status = AnalysisStatus.FAILED;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.analyzedAt = Instant.now();
        this.expiresAt = this.analyzedAt.plusSeconds(15L * 24 * 60 * 60);
    }

    public Long getId() {
        return id;
    }

    public AnalysisBatch getBatch() {
        return batch;
    }

    public Resume getResume() {
        return resume;
    }

    public String getJobDescriptionSnapshot() {
        return jobDescriptionSnapshot;
    }

    public Double getScore() {
        return score;
    }

    public String getResultJson() {
        return resultJson;
    }

    public AnalysisStatus getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Instant getAnalyzedAt() {
        return analyzedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}