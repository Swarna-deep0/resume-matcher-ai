package com.resumematcher.repository;

import com.resumematcher.entity.AnalysisBatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AnalysisBatchRepository
        extends JpaRepository<AnalysisBatch, Long> {

    Optional<AnalysisBatch> findByIdAndUser_Id(
            Long batchId, Long userId);

    List<AnalysisBatch> findAllByUser_IdOrderByCreatedAtDesc(
            Long userId);

    boolean existsByIdAndUser_Id(Long batchId, Long userId);

    boolean existsByJobDescription_Id(Long jobDescriptionId);
}