package com.resumematcher.repository;

import com.resumematcher.entity.JobDescription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JobDescriptionRepository
        extends JpaRepository<JobDescription, Long> {

    Optional<JobDescription> findByIdAndUser_Id(
            Long jobDescriptionId,
            Long userId
    );

    List<JobDescription> findAllByUser_IdOrderByUpdatedAtDesc(
            Long userId
    );

    boolean existsByIdAndUser_Id(
            Long jobDescriptionId,
            Long userId
    );
}