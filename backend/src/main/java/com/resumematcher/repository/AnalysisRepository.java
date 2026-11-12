package com.resumematcher.repository;

import com.resumematcher.entity.Analysis;
import com.resumematcher.entity.enums.AnalysisStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AnalysisRepository
        extends JpaRepository<Analysis, Long> {

    Optional<Analysis> findByIdAndBatch_User_Id(
            Long analysisId,
            Long userId
    );

    List<Analysis> findAllByBatch_IdAndBatch_User_IdOrderByScoreDescAnalyzedAtAsc(
            Long batchId,
            Long userId
    );

    List<Analysis> findAllByBatch_IdAndBatch_User_IdAndStatusOrderByScoreDescAnalyzedAtAsc(
            Long batchId,
            Long userId,
            AnalysisStatus status
    );

    List<Analysis> findAllByBatch_User_IdAndExpiresAtBefore(
            Long userId,
            Instant expiryTime
    );

    long countByBatch_Id(Long batchId);

    long deleteAllByExpiresAtBefore(Instant expiryTime);
}