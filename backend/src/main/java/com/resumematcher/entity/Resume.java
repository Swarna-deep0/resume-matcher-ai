package com.resumematcher.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "resumes")
public class Resume {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "file_size_bytes", nullable = false)
    private Long fileSizeBytes;

    @Lob
    @Column(name = "extracted_text", nullable = false, columnDefinition = "LONGTEXT")
    private String extractedText;

    @Column(name = "page_count", nullable = false)
    private Integer pageCount;

    @Column(name = "character_count", nullable = false)
    private Integer characterCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Resume() {
    }

    public Resume(
            User user,
            String originalFilename,
            String contentType,
            Long fileSizeBytes,
            String extractedText,
            Integer pageCount,
            Integer characterCount
    ) {
        this.user = user;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.fileSizeBytes = fileSizeBytes;
        this.extractedText = extractedText;
        this.pageCount = pageCount;
        this.characterCount = characterCount;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getContentType() {
        return contentType;
    }

    public Long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public String getExtractedText() {
        return extractedText;
    }

    public Integer getPageCount() {
        return pageCount;
    }

    public Integer getCharacterCount() {
        return characterCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}