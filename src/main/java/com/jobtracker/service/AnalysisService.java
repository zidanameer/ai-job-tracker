package com.jobtracker.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobtracker.dto.AnalyzeRequest;
import com.jobtracker.dto.AnalyzeResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Orchestrates a single job-description analysis: assembles the prompt, calls
 * the LLM through {@link LlmClient}, and parses the structured result.
 *
 * <p>All LLM access goes through {@link LlmClient}, so this class — the part
 * worth testing (prompt building + response parsing + failure handling) — is
 * unit-testable with a stub and no network.
 */
@Service
public class AnalysisService {

    private static final String SYSTEM_PROMPT = """
            You are an expert technical recruiter and CV coach helping a software
            engineer tailor their applications.

            Given a job description, do two things:
            1. Extract the key requirements and skills the employer is looking for.
            2. Suggest concrete, results-oriented CV bullet points the candidate
               could use to address those requirements (use strong action verbs;
               leave realistic placeholders like [X]% where a metric belongs).

            Respond with ONLY a JSON object, no prose and no markdown fences, in
            exactly this shape:
            {
              "requirements": ["...", "..."],
              "tailoredBullets": ["...", "..."]
            }
            """;

    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;

    public AnalysisService(LlmClient llmClient, ObjectMapper objectMapper) {
        this.llmClient = llmClient;
        this.objectMapper = objectMapper;
    }

    public AnalyzeResponse analyze(AnalyzeRequest request) {
        String userPrompt = buildUserPrompt(request);
        String raw = llmClient.complete(SYSTEM_PROMPT, userPrompt);
        return parse(raw);
    }

    private String buildUserPrompt(AnalyzeRequest request) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(request.company())) {
            sb.append("Company: ").append(request.company().strip()).append('\n');
        }
        if (StringUtils.hasText(request.role())) {
            sb.append("Role: ").append(request.role().strip()).append('\n');
        }
        sb.append("Job description:\n").append(request.jobDescription().strip());
        return sb.toString();
    }

    /**
     * Parse Claude's response into the structured result. Tolerates the model
     * wrapping the JSON in prose or markdown fences by extracting the outermost
     * JSON object; anything genuinely unparseable becomes a 502 via
     * {@link AnalysisException}.
     */
    private AnalyzeResponse parse(String raw) {
        String json = extractJsonObject(raw);
        try {
            AnalyzeResponse response = objectMapper.readValue(json, AnalyzeResponse.class);
            if (response.requirements() == null || response.tailoredBullets() == null) {
                throw new AnalysisException("Claude response missing required fields");
            }
            return response;
        } catch (AnalysisException e) {
            throw e;
        } catch (Exception e) {
            throw new AnalysisException("Could not parse Claude response as JSON", e);
        }
    }

    private String extractJsonObject(String raw) {
        if (raw == null) {
            throw new AnalysisException("Claude response was empty");
        }
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new AnalysisException("No JSON object found in Claude response");
        }
        return raw.substring(start, end + 1);
    }
}
