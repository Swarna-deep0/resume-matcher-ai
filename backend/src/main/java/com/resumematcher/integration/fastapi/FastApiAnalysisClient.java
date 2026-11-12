
package com.resumematcher.integration.fastapi;

import com.resumematcher.config.RequestIdFilter;
import com.resumematcher.dto.ExtractedResumeText;
import com.resumematcher.exception.AiServiceUnavailableException;
import com.resumematcher.integration.fastapi.dto.AnalyzeRequest;
import com.resumematcher.integration.fastapi.dto.AnalyzeResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class FastApiAnalysisClient {

    private static final Logger log =
            LoggerFactory.getLogger(FastApiAnalysisClient.class);

    private final RestClient aiRestClient;

    public FastApiAnalysisClient(RestClient aiRestClient) {
        this.aiRestClient = aiRestClient;
    }

    public AnalyzeResponse analyze(
            ExtractedResumeText resume,
            String jobDescriptionText) {

        if (resume == null || resume.text() == null || resume.text().isBlank()) {
            throw new IllegalArgumentException("Extracted resume text must not be empty");
        }

        if (jobDescriptionText == null || jobDescriptionText.isBlank()) {
            throw new IllegalArgumentException("Job description text must not be empty");
        }

        AnalyzeRequest payload = new AnalyzeRequest(
                resume.text(),
                jobDescriptionText
        );

        RestClient.RequestBodySpec request = aiRestClient.post()
                .uri("/analyze")
                .contentType(MediaType.APPLICATION_JSON);

        String requestId = MDC.get(RequestIdFilter.MDC_KEY);

        if (requestId != null) {
            request = request.header(RequestIdFilter.HEADER, requestId);
        }

        try {
            AnalyzeResponse result = request
                    .body(payload)
                    .exchange((clientRequest, response) -> {

                        int status = response.getStatusCode().value();

                        if (!response.getStatusCode().is2xxSuccessful()) {
                            log.error(
                                    "AI analysis service returned HTTP status {}",
                                    status
                            );
                            throw new AiServiceUnavailableException();
                        }

                        AnalyzeResponse parsed =
                                response.bodyTo(AnalyzeResponse.class);

                        if (parsed == null) {
                            log.error("AI analysis service returned an empty response");
                            throw new AiServiceUnavailableException();
                        }

                        return parsed;
                    });

            log.info(
                    "AI analysis completed: score={}, matched={}, missing={}",
                    result.score(),
                    result.matchedSkills() != null
                            ? result.matchedSkills().size() : 0,
                    result.missingSkills() != null
                            ? result.missingSkills().size() : 0
            );

            return result;

        } catch (ResourceAccessException e) {
            log.error(
                    "AI analysis service unreachable or timed out: {}",
                    e.getClass().getSimpleName()
            );
            throw new AiServiceUnavailableException();

        } catch (RestClientException e) {
            log.error(
                    "AI analysis response could not be processed: {}",
                    e.getClass().getSimpleName()
            );
            throw new AiServiceUnavailableException();
        }
    }
}
