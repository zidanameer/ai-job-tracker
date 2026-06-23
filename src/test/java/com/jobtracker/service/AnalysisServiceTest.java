package com.jobtracker.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobtracker.dto.AnalyzeRequest;
import com.jobtracker.dto.AnalyzeResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalysisServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** Capturing stub: records the prompts and returns a canned response. */
    private static final class StubLlmClient implements LlmClient {
        String capturedSystem;
        String capturedUser;
        final String response;

        StubLlmClient(String response) {
            this.response = response;
        }

        @Override
        public String complete(String systemPrompt, String userPrompt) {
            this.capturedSystem = systemPrompt;
            this.capturedUser = userPrompt;
            return response;
        }
    }

    @Test
    void parsesWellFormedJsonResponse() {
        StubLlmClient stub = new StubLlmClient(
                "{\"requirements\":[\"Java\",\"Spring Boot\"]," +
                        "\"tailoredBullets\":[\"Built X\",\"Shipped Y\"]}");
        AnalysisService service = new AnalysisService(stub, objectMapper);

        AnalyzeResponse result = service.analyze(
                new AnalyzeRequest("Looking for a Java engineer", null, null));

        assertEquals(java.util.List.of("Java", "Spring Boot"), result.requirements());
        assertEquals(java.util.List.of("Built X", "Shipped Y"), result.tailoredBullets());
    }

    @Test
    void toleratesJsonWrappedInProseAndFences() {
        StubLlmClient stub = new StubLlmClient(
                "Sure! Here is the analysis:\n```json\n" +
                        "{\"requirements\":[\"Kubernetes\"],\"tailoredBullets\":[\"Ran clusters\"]}" +
                        "\n```\nHope that helps.");
        AnalysisService service = new AnalysisService(stub, objectMapper);

        AnalyzeResponse result = service.analyze(
                new AnalyzeRequest("DevOps role", null, null));

        assertEquals(java.util.List.of("Kubernetes"), result.requirements());
        assertEquals(java.util.List.of("Ran clusters"), result.tailoredBullets());
    }

    @Test
    void includesCompanyAndRoleInPromptWhenProvided() {
        StubLlmClient stub = new StubLlmClient(
                "{\"requirements\":[],\"tailoredBullets\":[]}");
        AnalysisService service = new AnalysisService(stub, objectMapper);

        service.analyze(new AnalyzeRequest("JD body here", "Acme", "Backend Engineer"));

        assertTrue(stub.capturedUser.contains("Acme"), "prompt should include company");
        assertTrue(stub.capturedUser.contains("Backend Engineer"), "prompt should include role");
        assertTrue(stub.capturedUser.contains("JD body here"), "prompt should include the JD");
    }

    @Test
    void throwsAnalysisExceptionOnUnparseableResponse() {
        StubLlmClient stub = new StubLlmClient("the model rambled with no json at all");
        AnalysisService service = new AnalysisService(stub, objectMapper);

        assertThrows(AnalysisException.class, () ->
                service.analyze(new AnalyzeRequest("JD", null, null)));
    }

    @Test
    void throwsAnalysisExceptionWhenRequiredFieldsMissing() {
        StubLlmClient stub = new StubLlmClient("{\"requirements\":[\"Java\"]}");
        AnalysisService service = new AnalysisService(stub, objectMapper);

        assertThrows(AnalysisException.class, () ->
                service.analyze(new AnalyzeRequest("JD", null, null)));
    }
}
