package com.jobtracker.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for {@code POST /api/analyze}. company/role are optional context
 * the engineer may supply; only the job description is required to analyze.
 */
public record AnalyzeRequest(
        @NotBlank(message = "jobDescription must not be blank")
        String jobDescription,
        String company,
        String role
) {
}
