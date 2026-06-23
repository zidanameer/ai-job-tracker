package com.jobtracker.dto;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the public API speaks snake_case on the wire while the DTOs keep
 * camelCase Java fields — driven entirely by {@code spring.jackson
 * .property-naming-strategy}, with no annotations on the DTOs. Uses the
 * application-configured Jackson mapper (the same one the HTTP layer uses).
 */
@JsonTest
class ApiJsonNamingTest {

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void responseSerializesToSnakeCase() {
        String json = objectMapper.writeValueAsString(
                new AnalyzeResponse(List.of("Java"), List.of("Built X")));

        assertTrue(json.contains("\"tailored_bullets\""), () -> "expected snake_case, got: " + json);
        assertFalse(json.contains("tailoredBullets"), () -> "should not emit camelCase: " + json);
    }

    @Test
    void requestDeserializesFromSnakeCase() {
        AnalyzeRequest request = objectMapper.readValue(
                "{\"job_description\":\"JD body\",\"company\":\"Acme\",\"role\":\"Backend Engineer\"}",
                AnalyzeRequest.class);

        assertEquals("JD body", request.jobDescription());
        assertEquals("Acme", request.company());
        assertEquals("Backend Engineer", request.role());
    }
}
