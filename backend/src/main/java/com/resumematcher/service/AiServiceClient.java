package com.resumematcher.service;

import com.resumematcher.dto.ExtractedResumeText;
import com.resumematcher.exception.AiServiceUnavailableException;
import com.resumematcher.exception.ResumeRejectedException;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Component
public class AiServiceClient {

    private final RestClient aiRestClient;

    public AiServiceClient(RestClient aiRestClient) {
        this.aiRestClient = aiRestClient;
    }

    public ExtractedResumeText extractText(
            MultipartFile file,
            String requestId) {

        byte[] fileBytes;

        try {
            fileBytes = file.getBytes();
        } catch (IOException ex) {
            throw new ResumeRejectedException(
                    HttpStatus.BAD_REQUEST,
                    "FILE_READ_FAILED",
                    "The uploaded file could not be read"
            );
        }

        String filename = file.getOriginalFilename();

        ByteArrayResource resource = new ByteArrayResource(fileBytes) {
            @Override
            public String getFilename() {
                return filename;
            }
        };

        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();

        bodyBuilder.part("file", resource)
                .filename(filename)
                .contentType(MediaType.APPLICATION_PDF);

        try {
            ExtractedResumeText result = aiRestClient.post()
                    .uri("/extract-text")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .header("X-Request-ID", requestId)
                    .body(bodyBuilder.build())
                    .retrieve()

                    .onStatus(
                            HttpStatusCode::is4xxClientError,
                            (request, response) -> {
                                HttpStatus status = HttpStatus.valueOf(
                                        response.getStatusCode().value()
                                );

                                throw new ResumeRejectedException(
                                        status,
                                        "EXTRACTION_REJECTED",
                                        "The PDF extraction service rejected the uploaded file"
                                );
                            }
                    )

                    .onStatus(
                            HttpStatusCode::is5xxServerError,
                            (request, response) -> {
                                throw new AiServiceUnavailableException();
                            }
                    )

                    .body(ExtractedResumeText.class);

            if (result == null || result.text() == null) {
                throw new AiServiceUnavailableException();
            }

            return result;

        } catch (RestClientException ex) {
            throw new AiServiceUnavailableException();
        }
    }
}