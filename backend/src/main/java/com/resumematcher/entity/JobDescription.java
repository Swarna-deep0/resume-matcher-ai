package com.resumematcher.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "job_descriptions")
public class JobDescription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(name = "company_name", length = 150)
    private String companyName;

    @Lob
    @Column(name = "description_text", nullable = false, columnDefinition = "LONGTEXT")
    private String descriptionText;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected JobDescription() {
    }

    public JobDescription(
            User user,
            String title,
            String companyName,
            String descriptionText
    ) {
        this.user = user;
        this.title = title;
        this.companyName = companyName;
        this.descriptionText = descriptionText;
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

    public void updateDetails(
            String title,
            String companyName,
            String descriptionText
    ) {
        this.title = title;
        this.companyName = companyName;
        this.descriptionText = descriptionText;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getTitle() {
        return title;
    }

    public String getCompanyName() {
        return companyName;
    }

    public String getDescriptionText() {
        return descriptionText;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}