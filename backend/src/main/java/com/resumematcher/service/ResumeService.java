package com.resumematcher.service;

import com.resumematcher.dto.ExtractedResumeText;
import com.resumematcher.dto.ResumeUploadResponse;
import com.resumematcher.entity.Resume;
import com.resumematcher.entity.User;
import com.resumematcher.repository.ResumeRepository;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ResumeService {

    private final ResumeFileValidator resumeFileValidator;
    private final ResumeTextService resumeTextService;
    private final ResumeRepository resumeRepository;
    private final UserService userService;

    public ResumeService(
            ResumeFileValidator resumeFileValidator,
            ResumeTextService resumeTextService,
            ResumeRepository resumeRepository,
            UserService userService) {

        this.resumeFileValidator = resumeFileValidator;
        this.resumeTextService = resumeTextService;
        this.resumeRepository = resumeRepository;
        this.userService = userService;
    }

    @Transactional
    public ResumeUploadResponse uploadResume(
            Long userId,
            MultipartFile file) {

        // 1. Validate the uploaded file.
        resumeFileValidator.validate(file);

        // 2. Resolve the authenticated user.
        User user = userService.getById(userId);

        // 3. Extract text through the existing FastAPI integration.
        ExtractedResumeText extracted =
                resumeTextService.extractText(file);

        // 4. Persist extracted text and metadata only.
        Resume resume = new Resume(
                user,
                file.getOriginalFilename().trim(),
                MediaType.APPLICATION_PDF_VALUE,
                file.getSize(),
                extracted.text(),
                extracted.pageCount(),
                extracted.characterCount()
        );

        Resume savedResume = resumeRepository.save(resume);

        // 5. Return the upload response.
        return new ResumeUploadResponse(
                savedResume.getId(),
                savedResume.getOriginalFilename(),
                savedResume.getContentType(),
                savedResume.getFileSizeBytes(),
                savedResume.getPageCount(),
                savedResume.getCharacterCount(),
                savedResume.getCreatedAt()
        );
    }
}