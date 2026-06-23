# Project: AI Job-Application Tracker

## Context
Flagship portfolio project for a software engineer with 5 years of experience,
currently based in India, preparing to switch jobs to Europe (target country
open — Germany and the Netherlands are the leading options) within roughly
6 months. The project's purpose is to demonstrate strong backend engineering +
cloud + AI integration to European employers. It must read as real engineering,
not a thin wrapper around an LLM.

## What it does
Paste in a job description. The app extracts the key requirements/skills,
suggests tailored CV bullet points for them, and tracks applications
(company, role, status, applied date, follow-up dates). The engineer will
dogfood it during their own job hunt.

## Tech stack (decided)
- Java 25 (Amazon Corretto), built with Maven
- Spring Boot 4.x — Spring Web, Spring Data JPA. Java 25 requires Spring Boot 4.0+
  (built on Spring Framework 7); 3.5.x does NOT support Java 25. Pinned to 4.1.0.
  Spring Security is part of the target stack but DEFERRED past the first slice
  (no auth scheme designed yet — adding it now would only gate /api/analyze behind
  default basic auth). Add it as its own slice.
- PostgreSQL (run locally in a container)
- Claude API for the AI features (job-description parsing + CV bullet tailoring),
  via the official Anthropic Java SDK (com.anthropic:anthropic-java, 2.43.0).
  Model: claude-sonnet-4-6 — current Sonnet, verified against the Anthropic docs
  June 2026; keep it swappable via config (claude.model), don't hard-code at call
  sites. Read the API key from the ANTHROPIC_API_KEY env var.
  Handle Claude failures gracefully: a timeout/error maps to HTTP 502, never a
  raw 500 stack trace to the client; isolate the SDK behind an LlmClient interface
  so the service layer stays unit-testable without network calls.
- Containerized (OrbStack/Docker), CI/CD via GitHub Actions, deployed to AWS
- Infrastructure as code with Terraform (a later phase, not the first slice)

## Environment
- MacBook Air M5 (Apple Silicon).
- JDKs managed via SDKMAN; JAVA_HOME points to Corretto 25.
- AWS account already set up and available.

## Conventions
- Clean layered architecture: controller -> service -> repository.
- JUnit 5 tests for service-layer logic.
- Secrets (Claude API key, DB credentials) via environment variables / .env;
  never commit secrets. Add a .gitignore early.
- Conventional commit messages.

## API contract (first slice)
POST /api/analyze
- Request body:  { "jobDescription": "<string, required, non-blank>" }
  (company / role optional — accepted but not required to analyze.)
- 200 response:  { "requirements": ["..."], "tailoredBullets": ["..."] }
- 400: jobDescription missing or blank (bean-validation error).
- 502: Claude API call failed (timeout, error, or unparseable response).
The endpoint analyzes and returns; persisting the result as an Application row
is a later slice (the entity + repository exist now so the model is in place).

## Immediate task (first slice — build this now)
1. Scaffold a Spring Boot project (Maven, Java 25) with a sensible package layout.
2. Define the data model: Application entity with fields like
   id, company, role, jobDescription, status, appliedDate, followUpDate,
   extractedRequirements, tailoredBullets.
   Storage decision: extractedRequirements / tailoredBullets are stored as
   @ElementCollection string lists (clean relational, no in-DB JSON parsing).
   Revisit jsonb only if we later need to query inside them.
3. Build the first Claude-powered endpoint: POST /api/analyze that accepts a
   job description and returns extracted requirements + suggested CV bullets,
   by calling the Anthropic Messages API.
4. Get it running locally against a PostgreSQL container.

Briefly confirm the plan, then start building.
