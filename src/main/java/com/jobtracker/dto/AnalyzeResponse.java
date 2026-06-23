package com.jobtracker.dto;

import java.util.List;

/**
 * Result of analyzing a job description: the key requirements/skills Claude
 * extracted, and tailored CV bullet points addressing them. Doubles as the
 * JSON shape Claude is asked to return, so the service can deserialize directly.
 */
public record AnalyzeResponse(
        List<String> requirements,
        List<String> tailoredBullets
) {
}
