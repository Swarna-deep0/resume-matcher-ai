package com.resumematcher.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(
    name = "session_resumes",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_session_resume",
            columnNames = {"session_id", "resume_id"}
        )
    },
    indexes = {
        @Index(name = "idx_session_resumes_session", columnList = "session_id"),
        @Index(name = "idx_session_resumes_resume", columnList = "resume_id")
    }
)
public class SessionResume {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private AnalysisSession session;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resume_id", nullable = false)
    private Resume resume;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected SessionResume() {
    }

    public SessionResume(AnalysisSession session, Resume resume) {
        this.session = session;
        this.resume = resume;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public AnalysisSession getSession() {
        return session;
    }

    public Resume getResume() {
        return resume;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
