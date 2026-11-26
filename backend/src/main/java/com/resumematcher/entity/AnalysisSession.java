package com.resumematcher.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(
        name = "analysis_sessions",
        indexes = {
                @Index(name = "idx_sessions_user_updated", columnList = "user_id, updated_at")
        }
)
public class AnalysisSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_description_id")
    private JobDescription jobDescription;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AnalysisSession() {
    }

    public AnalysisSession(User user, String name) {
        this.user = user;
        this.name = name;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public void rename(String name) {
        this.name = name;
    }

    public void attachJobDescription(JobDescription jobDescription) {
        this.jobDescription = jobDescription;
    }

    public void removeJobDescription() {
        this.jobDescription = null;
    }

    // Marks the session as recently used, so session lists sort by latest activity.
    public void markUpdated() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getName() {
        return name;
    }

    public JobDescription getJobDescription() {
        return jobDescription;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}