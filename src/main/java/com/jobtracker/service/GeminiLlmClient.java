package com.jobtracker.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

@Component
public class GeminiLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiLlmClient.class);
    private static final String BASE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/";

    private final RestClient restClient;
    private final String model;
    private final String apiKey;

    /**
     * Bounds the number of in-flight Gemini calls. The web tier accepts requests
     * on unbounded virtual threads, so this is the explicit backpressure that
     * keeps outbound load under Gemini's rate limits and caps cost. Callers past
     * the limit park cheaply on their virtual thread until a permit frees; the
     * read timeout bounds how long any one permit can be held.
     */
    private final Semaphore inFlight;

    public GeminiLlmClient(
            @Value("${gemini.model}") String model,
            @Value("${gemini.api-key}") String apiKey,
            @Value("${gemini.connect-timeout-ms}") int connectTimeoutMs,
            @Value("${gemini.read-timeout-ms}") int readTimeoutMs,
            @Value("${gemini.max-concurrent-requests}") int maxConcurrentRequests) {
        this.model = model;
        this.apiKey = apiKey;
        this.inFlight = new Semaphore(maxConcurrentRequests);

        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        var body = Map.of(
                "system_instruction", Map.of(
                        "parts", List.of(Map.of("text", systemPrompt))
                ),
                "contents", List.of(Map.of(
                        "parts", List.of(Map.of("text", userPrompt))
                ))
        );

        try {
            inFlight.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AnalysisException("Interrupted while waiting for Gemini capacity", e);
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri(BASE_URL + model + ":generateContent?key=" + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            return extractText(response);
        } catch (RestClientException e) {
            log.warn("Gemini API call failed: {}", e.getMessage());
            throw new AnalysisException("Gemini API call failed", e);
        } finally {
            inFlight.release();
        }
    }

    @SuppressWarnings("unchecked")
    private String extractText(Map<String, Object> response) {
        try {
            var candidates = (List<Map<String, Object>>) response.get("candidates");
            var content = (Map<String, Object>) candidates.get(0).get("content");
            var parts = (List<Map<String, Object>>) content.get("parts");
            String text = (String) parts.get(0).get("text");
            if (text == null || text.isBlank()) {
                throw new AnalysisException("Gemini returned no text content");
            }
            return text;
        } catch (NullPointerException | IndexOutOfBoundsException | ClassCastException e) {
            throw new AnalysisException("Unexpected Gemini response structure", e);
        }
    }
}
