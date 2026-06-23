package com.jobtracker.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * A tracked job application. Holds the source job description plus the
 * Claude-derived requirements and tailored CV bullets, and the status the
 * engineer moves it through during their hunt.
 *
 * <p>{@code extractedRequirements} and {@code tailoredBullets} are stored as
 * {@link ElementCollection} string lists — clean relational storage with no
 * in-DB JSON parsing. Revisit jsonb only if we later need to query inside them.
 */
@Entity
@Table(name = "applications")
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String company;

    private String role;

    @Lob
    @Column(columnDefinition = "text")
    private String jobDescription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus status = ApplicationStatus.DRAFT;

    private LocalDate appliedDate;

    private LocalDate followUpDate;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "application_requirements",
            joinColumns = @JoinColumn(name = "application_id"))
    @Column(name = "requirement", columnDefinition = "text")
    private List<String> extractedRequirements = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "application_bullets",
            joinColumns = @JoinColumn(name = "application_id"))
    @Column(name = "bullet", columnDefinition = "text")
    private List<String> tailoredBullets = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Application() {
        // JPA
    }

    public Long getId() {
        return id;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getJobDescription() {
        return jobDescription;
    }

    public void setJobDescription(String jobDescription) {
        this.jobDescription = jobDescription;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public void setStatus(ApplicationStatus status) {
        this.status = status;
    }

    public LocalDate getAppliedDate() {
        return appliedDate;
    }

    public void setAppliedDate(LocalDate appliedDate) {
        this.appliedDate = appliedDate;
    }

    public LocalDate getFollowUpDate() {
        return followUpDate;
    }

    public void setFollowUpDate(LocalDate followUpDate) {
        this.followUpDate = followUpDate;
    }

    public List<String> getExtractedRequirements() {
        return extractedRequirements;
    }

    public void setExtractedRequirements(List<String> extractedRequirements) {
        this.extractedRequirements = extractedRequirements;
    }

    public List<String> getTailoredBullets() {
        return tailoredBullets;
    }

    public void setTailoredBullets(List<String> tailoredBullets) {
        this.tailoredBullets = tailoredBullets;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
