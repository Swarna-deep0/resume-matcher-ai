package com.resumematcher.repository;

import com.resumematcher.entity.SessionResume;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SessionResumeRepository
        extends JpaRepository<SessionResume, Long> {

    List<SessionResume> findAllBySession_IdOrderByCreatedAtAsc(
            Long sessionId
    );

    Optional<SessionResume> findBySession_IdAndResume_Id(
            Long sessionId,
            Long resumeId
    );

    boolean existsBySession_IdAndResume_Id(
            Long sessionId,
            Long resumeId
    );

    long countBySession_Id(Long sessionId);

    long deleteBySession_IdAndResume_Id(
            Long sessionId,
            Long resumeId
    );
}
