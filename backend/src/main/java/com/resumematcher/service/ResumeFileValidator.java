package com.resumematcher.service;

import com.resumematcher.exception.ResumeRejectedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

@Component
public class ResumeFileValidator {

    private static final byte[] PDF_SIGNATURE = {
            '%', 'P', 'D', 'F', '-'
    };

    private static final int MAX_FILENAME_LENGTH = 255;

    private final long maxBytes;

    public ResumeFileValidator(
            @Value("${app.upload.max-bytes}") long maxBytes) {
        this.maxBytes = maxBytes;
    }

    public void validate(MultipartFile file) {

        // 1. Check whether the file exists.
        if (file == null) {
            throw new ResumeRejectedException(
                    HttpStatus.BAD_REQUEST,
                    "FILE_REQUIRED",
                    "A PDF file is required"
            );
        }

        // 2. Reject empty files.
        if (file.isEmpty()) {
            throw new ResumeRejectedException(
                    HttpStatus.BAD_REQUEST,
                    "EMPTY_FILE",
                    "The uploaded file is empty"
            );
        }

        // 3. Enforce the configured upload limit.
        if (file.getSize() > maxBytes) {
            throw new ResumeRejectedException(
                    HttpStatus.PAYLOAD_TOO_LARGE,
                    "FILE_TOO_LARGE",
                    "The PDF must be at most 5 MB"
            );
        }

        // 4. Validate the original filename.
        String filename = file.getOriginalFilename();

        if (filename == null || filename.isBlank()) {
            throw new ResumeRejectedException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_FILENAME",
                    "The uploaded file must have a valid filename"
            );
        }

        filename = filename.trim();

        if (filename.length() > MAX_FILENAME_LENGTH
                || filename.equals(".")
                || filename.equals("..")
                || filename.contains("/")
                || filename.contains("\\")
                || filename.chars().anyMatch(Character::isISOControl)) {

            throw new ResumeRejectedException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_FILENAME",
                    "The uploaded filename is invalid"
            );
        }

        // 5. Validate the actual PDF signature.
        // Do not rely on the client-provided MIME type.
        try (InputStream inputStream = file.getInputStream()) {

            byte[] signature = inputStream.readNBytes(PDF_SIGNATURE.length);

            if (!matchesPdfSignature(signature)) {
                throw new ResumeRejectedException(
                        HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                        "NOT_A_PDF",
                        "The uploaded file is not a valid PDF"
                );
            }

        } catch (IOException ex) {
            throw new ResumeRejectedException(
                    HttpStatus.BAD_REQUEST,
                    "FILE_READ_FAILED",
                    "The uploaded file could not be read"
            );
        }
    }

    private boolean matchesPdfSignature(byte[] signature) {

        if (signature.length != PDF_SIGNATURE.length) {
            return false;
        }

        for (int i = 0; i < PDF_SIGNATURE.length; i++) {
            if (signature[i] != PDF_SIGNATURE[i]) {
                return false;
            }
        }

        return true;
    }
}