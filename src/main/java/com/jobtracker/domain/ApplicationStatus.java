package com.jobtracker.domain;

/**
 * Lifecycle of a single job application as the engineer works through their hunt.
 */
public enum ApplicationStatus {
    DRAFT,
    APPLIED,
    INTERVIEWING,
    OFFER,
    REJECTED,
    WITHDRAWN
}
