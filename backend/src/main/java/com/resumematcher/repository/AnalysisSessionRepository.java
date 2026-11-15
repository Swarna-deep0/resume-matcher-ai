package com.resumematcher.repository;

import com.resumematcher.entity.AnalysisSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AnalysisSessionRepository
        extends JpaRepository<AnalysisSession, Long> {

    Optional<AnalysisSession> findByIdAndUser_Id(
            Long sessionId,
            Long userId
    );

    List<AnalysisSession> findAllByUser_IdOrderByUpdatedAtDesc(
            Long userId
    );
}
