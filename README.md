# AI Job-Application Tracker

Paste in a job description; the app extracts the key requirements/skills and
suggests tailored CV bullet points, and tracks applications through your hunt.
Backend portfolio project: Java 25 + Spring Boot 4 + PostgreSQL + the Claude API.

## Tech stack

- **Java 25** (Amazon Corretto), built with **Maven**
- **Spring Boot 4.1** (Spring Web, Spring Data JPA) — Java 25 requires Spring Boot 4.x
- **PostgreSQL** (local, via Docker/OrbStack)
- **Claude API** via the official Anthropic Java SDK (`com.anthropic:anthropic-java`),
  model `claude-sonnet-4-6` (swappable via `claude.model`)

## Architecture

Clean layered: `controller -> service -> repository`. The Claude SDK is isolated
behind an `LlmClient` interface, so the service layer (prompt assembly + response
parsing) is unit-tested with a stub — no network, no API key.

```
controller/AnalyzeController       POST /api/analyze
service/AnalysisService            prompt assembly + JSON parsing
service/LlmClient (interface)      seam over the LLM provider
service/AnthropicLlmClient         real Claude Messages API call
domain/Application                 JPA entity (tracked application)
repository/ApplicationRepository   Spring Data JPA
```

## Setup

1. **Configure secrets** — copy the example env file and fill it in:
   ```sh
   cp .env.example .env      # then edit ANTHROPIC_API_KEY and DB_PASSWORD
   ```
   `.env` is gitignored. Export the vars into your shell before running, e.g.:
   ```sh
   set -a && source .env && set +a
   ```

2. **Run tests** (no DB or API key needed — service layer is stubbed):
   ```sh
   ./mvnw test          # or: mvn test
   ```

3. **Start PostgreSQL** (requires Docker/OrbStack):
   ```sh
   docker compose up -d
   ```

4. **Run the app**:
   ```sh
   ./mvnw spring-boot:run
   ```

## API

### `POST /api/analyze`

Request:
```json
{ "jobDescription": "We're hiring a backend engineer with Java and AWS...",
  "company": "Acme",        // optional
  "role": "Backend Engineer" }  // optional
```

Response `200`:
```json
{ "requirements": ["Java", "AWS", "..."],
  "tailoredBullets": ["Built ...", "..."] }
```

Errors: `400` (blank `jobDescription`), `502` (Claude call failed / unparseable).

```sh
curl -s localhost:8080/api/analyze \
  -H 'Content-Type: application/json' \
  -d '{"jobDescription":"Backend engineer: Java, Spring, AWS, Postgres."}'
```

## Roadmap (next slices)

- Persist analyses as `Application` rows; CRUD endpoints for tracking.
- Spring Security (auth scheme TBD).
- Flyway migrations (replace `ddl-auto: update`).
- CI/CD (GitHub Actions), containerized deploy to AWS, Terraform IaC.
