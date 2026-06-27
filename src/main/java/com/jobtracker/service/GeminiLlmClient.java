package com.jobtracker.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

@Component
public class GeminiLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiLlmClient.class);
    private static final String BASE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/";

    private final RestClient restClient;
    private final String model;
    private final String apiKey;

    public GeminiLlmClient(
            @Value("${gemini.model}") String model,
            @Value("${gemini.api-key}") String apiKey) {
        this.model = model;
        this.apiKey = apiKey;
        this.restClient = RestClient.create();
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
