package com.resumematcher.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class AiServiceConfig {

    @Bean
    public RestClient aiRestClient(
            @Value("${app.ai-service.base-url}") String baseUrl,
            @Value("${app.ai-service.api-key}") String apiKey,
            @Value("${app.ai-service.connect-timeout-ms}") int connectTimeoutMs,
            @Value("${app.ai-service.read-timeout-ms}") int readTimeoutMs) {

        if (apiKey.length() < 32) {
            throw new IllegalStateException("AI_SERVICE_API_KEY must be at least 32 characters long");
        }

        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));

        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("X-Internal-Api-Key", apiKey)
                .requestFactory(requestFactory)
                .build();
    }
}