package com.jobtracker.service;

/**
 * Raised when the analysis can't be completed because of the downstream Claude
 * call — a timeout, an API error, or an unparseable response. Surfaced to the
 * client as HTTP 502 (never a raw 500).
 */
public class AnalysisException extends RuntimeException {

    public AnalysisException(String message) {
        super(message);
    }

    public AnalysisException(String message, Throwable cause) {
        super(message, cause);
    }
}
