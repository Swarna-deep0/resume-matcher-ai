package com.resumematcher.service;

import com.resumematcher.config.RequestIdFilter;
import com.resumematcher.dto.AiError;
import com.resumematcher.dto.ExtractedResumeText;
import com.resumematcher.exception.AiServiceUnavailableException;
import com.resumematcher.exception.ResumeRejectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
public class ResumeTextService {

    private static final Logger log = LoggerFactory.getLogger(ResumeTextService.class);
    private static final byte[] PDF_SIGNATURE = "%PDF-".getBytes(StandardCharsets.US_ASCII);

    private final RestClient aiRestClient;
    private final long maxBytes;

    public ResumeTextService(RestClient aiRestClient,
                             @Value("${app.upload.max-bytes}") long maxBytes) {
        this.aiRestClient = aiRestClient;
        this.maxBytes = maxBytes;
    }

    public ExtractedResumeText extractText(MultipartFile file) {
        byte[] data = readAndValidate(file);
        long start = System.nanoTime();

        // Fixed file name: the user's own file name is never forwarded or stored
        ByteArrayResource resource = new ByteArrayResource(data) {
            @Override
            public String getFilename() {
                return "resume.pdf";
            }
        };
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", resource);

        RestClient.RequestBodySpec request = aiRestClient.post()
                .uri("/extract-text")
                .contentType(MediaType.MULTIPART_FORM_DATA);
        String requestId = MDC.get(RequestIdFilter.MDC_KEY);
        if (requestId != null) {
            request = request.header(RequestIdFilter.HEADER, requestId);
        }

        ExtractedResumeText result;
        try {
            result = request.body(body).exchange((req, response) -> {
                int status = response.getStatusCode().value();

                if (response.getStatusCode().is2xxSuccessful()) {
                    ExtractedResumeText parsed = response.bodyTo(ExtractedResumeText.class);
                    if (parsed == null || parsed.text() == null) {
                        log.error("AI service returned an empty or invalid response");
                        throw new AiServiceUnavailableException();
                    }
                    return parsed;
                }

                if (status == 400 || status == 413 || status == 415 || status == 422) {
                    AiError error = response.bodyTo(AiError.class);
                    String code = (error != null && error.code() != null) ? error.code() : "RESUME_REJECTED";
                    String message = (error != null && error.message() != null)
                            ? error.message() : "The resume could not be processed";
                    throw new ResumeRejectedException(HttpStatus.valueOf(status), code, message);
                }

                // Includes 401 from a key mismatch: a configuration problem, not the user's fault
                log.error("AI service returned unexpected status {}", status);
                throw new AiServiceUnavailableException();
            });
        } catch (ResourceAccessException e) {
            log.error("AI service unreachable or timed out: {}", e.getClass().getSimpleName());
            throw new AiServiceUnavailableException();
        }

        long millis = (System.nanoTime() - start) / 1_000_000;
        log.info("Resume text extracted: pages={}, chars={}, took={} ms",
                result.pageCount(), result.characterCount(), millis);
        return result;
    }

    private byte[] readAndValidate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResumeRejectedException(HttpStatus.BAD_REQUEST, "EMPTY_FILE", "The uploaded file is empty");
        }
        if (file.getSize() > maxBytes) {
            throw new ResumeRejectedException(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE",
                    "The PDF must be at most 5 MB");
        }
        byte[] data;
        try {
            data = file.getBytes();
        } catch (IOException e) {
            throw new ResumeRejectedException(HttpStatus.BAD_REQUEST, "UPLOAD_FAILED",
                    "The uploaded file could not be read");
        }
        if (!startsWithPdfSignature(data)) {
            throw new ResumeRejectedException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "NOT_A_PDF",
                    "The uploaded file is not a PDF");
        }
        return data;
    }

    private boolean startsWithPdfSignature(byte[] data) {
        if (data.length < PDF_SIGNATURE.length) {
            return false;
        }
        for (int i = 0; i < PDF_SIGNATURE.length; i++) {
            if (data[i] != PDF_SIGNATURE[i]) {
                return false;
            }
        }
        return true;
    }
}