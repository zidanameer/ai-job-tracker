package com.jobtracker.service;

/**
 * Thin seam over the LLM provider. Keeping the SDK behind this interface lets
 * the service layer (prompt assembly + response parsing) be unit-tested with a
 * stub, no network and no API key required.
 */
public interface LlmClient {

    /**
     * Send a system + user prompt and return the model's raw text response.
     *
     * @throws AnalysisException if the provider call fails (timeout, API error).
     */
    String complete(String systemPrompt, String userPrompt);
}
