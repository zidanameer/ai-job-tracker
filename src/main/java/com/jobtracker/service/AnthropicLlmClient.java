package com.jobtracker.service;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.errors.AnthropicServiceException;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * {@link LlmClient} backed by the official Anthropic Java SDK (Claude Messages
 * API). The API key is read from the {@code ANTHROPIC_API_KEY} env var by the
 * SDK itself ({@link AnthropicOkHttpClient#fromEnv()}); the model and token
 * budget come from config so they're swappable without code changes.
 */
@Component
public class AnthropicLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(AnthropicLlmClient.class);

    private final AnthropicClient client;
    private final String model;
    private final long maxTokens;

    public AnthropicLlmClient(
            @Value("${claude.model}") String model,
            @Value("${claude.max-tokens}") long maxTokens) {
        this.client = AnthropicOkHttpClient.fromEnv();
        this.model = model;
        this.maxTokens = maxTokens;
    }

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        MessageCreateParams params = MessageCreateParams.builder()
                .model(model)
                .maxTokens(maxTokens)
                .system(systemPrompt)
                .addUserMessage(userPrompt)
                .build();

        try {
            Message response = client.messages().create(params);
            StringBuilder text = new StringBuilder();
            response.content().stream()
                    .flatMap(block -> block.text().stream())
                    .forEach(textBlock -> text.append(textBlock.text()));

            if (text.isEmpty()) {
                throw new AnalysisException("Claude returned no text content");
            }
            return text.toString();
        } catch (AnthropicServiceException e) {
            log.warn("Claude API call failed: {}", e.getMessage());
            throw new AnalysisException("Claude API call failed", e);
        }
    }
}
