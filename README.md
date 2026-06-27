# AI Job-Application Tracker

Paste in a job description; the app extracts the key requirements/skills and
suggests tailored CV bullet points, and tracks applications through your hunt.
Backend portfolio project: Java 25 + Spring Boot 4 + PostgreSQL + the Gemini API.

## Tech stack

- **Java 25** (Amazon Corretto), built with **Maven**
- **Spring Boot 4.1** (Spring Web, Spring Data JPA) — Java 25 requires Spring Boot 4.x
- **PostgreSQL** (local, via Docker/OrbStack)
- **Google Gemini API** (free tier), model `gemini-2.5-flash` (swappable via `gemini.model`),
  called over REST with Spring's `RestClient` — no extra SDK dependency

## Architecture

Clean layered: `controller -> service -> repository`. The LLM call is isolated
behind an `LlmClient` interface, so the service layer (prompt assembly + response
parsing) is unit-tested with a stub — no network, no API key.

```
controller/AnalyzeController       POST /api/analyze
service/AnalysisService            prompt assembly + JSON parsing
service/LlmClient (interface)      seam over the LLM provider
service/GeminiLlmClient            real Gemini generateContent call
domain/Application                 JPA entity (tracked application)
repository/ApplicationRepository   Spring Data JPA
```

## Setup

1. **Get a free Gemini API key** at <https://aistudio.google.com/app/apikey>.

2. **Configure secrets** — copy the example env file and fill it in:
   ```sh
   cp .env.example .env      # then edit GEMINI_API_KEY and DB_PASSWORD
   ```
   `.env` is gitignored. Export the vars into your shell before running, e.g.:
   ```sh
   set -a && source .env && set +a
   ```

3. **Run tests** (no DB or API key needed — service layer is stubbed):
   ```sh
   mvn test
   ```

4. **Start PostgreSQL** (requires Docker/OrbStack):
   ```sh
   docker compose up -d
   ```

5. **Run the app**:
   ```sh
   mvn spring-boot:run
   ```

### Running from IntelliJ

The Run button does **not** read `.env`. In **Run → Edit Configurations…** for
`JobTrackerApplication`, add these under **Environment variables** (values from
your `.env`), and make sure the JDK is **Corretto 25**:

```
GEMINI_API_KEY=...;POSTGRES_DB=jobtracker;POSTGRES_USER=jobtracker;POSTGRES_PASSWORD=...;DB_PASSWORD=...
```

Postgres still needs to be up (`docker compose up -d`) before you click Run.

## API

### `POST /api/analyze`

Request/response JSON is **snake_case** on the wire.

Request:
```json
{ "job_description": "We're hiring a backend engineer with Java and AWS...",
  "company": "Acme",            // optional
  "role": "Backend Engineer" }  // optional
```

Response `200`:
```json
{ "requirements": ["Java", "AWS", "..."],
  "tailored_bullets": ["Built ...", "..."] }
```

Errors: `400` (blank `job_description`), `502` (Gemini call failed, rate-limited,
or unparseable response).

```sh
curl -s localhost:8080/api/analyze \
  -H 'Content-Type: application/json' \
  -d '{"job_description":"Backend engineer: Java, Spring, AWS, Postgres."}'
```

## Roadmap (next slices)

- Persist analyses as `Application` rows; CRUD endpoints for tracking.
- Spring Security (auth scheme TBD).
- Flyway migrations (replace `ddl-auto: update`).
- CI/CD (GitHub Actions), containerized deploy to AWS, Terraform IaC.
