package com.resumematcher.repository;

import com.resumematcher.entity.Resume;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ResumeRepository extends JpaRepository<Resume, Long> {

    Optional<Resume> findByIdAndUser_Id(Long resumeId, Long userId);

    List<Resume> findAllByUser_IdOrderByCreatedAtDesc(Long userId);

    boolean existsByIdAndUser_Id(Long resumeId, Long userId);
}