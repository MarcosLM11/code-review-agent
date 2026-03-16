# Code Review Agent

> An AI-powered GitHub Pull Request review agent built on **Java 25**, **Spring Boot 4**, and **Embabel Agent** — automatically analyzes diffs, classifies issues by severity, and posts structured reviews directly to your PRs.

---

## Overview

Code Review Agent integrates with GitHub to provide automated, AI-driven code reviews. It fetches PR diffs, runs them through an LLM persona modeled after a principal engineer, and publishes a structured review comment back to the pull request — all without human intervention.

It supports three interaction modes:

| Mode | Mechanism | Use Case |
|------|-----------|----------|
| **Automatic** | GitHub Webhook | Triggered on PR open / push |
| **On-demand** | REST API | Manual trigger via HTTP |
| **Interactive** | Spring Shell | Local CLI exploration |

---

## Architecture

The project follows **Hexagonal Architecture** with strict layer isolation enforced at build time by ArchUnit.

```
┌─────────────────────────────────────────────────────────────────┐
│                         agent/                                  │
│   CodeReviewAgent  ──  4 @Action steps (GOAP orchestration)    │
│   ReviewPersonas   ──  LLM persona: Senior Code Reviewer        │
└────────────────────────────┬────────────────────────────────────┘
                             │
┌────────────────────────────▼────────────────────────────────────┐
│                         domain/                                 │
│   model/    PullRequestInput · PrDiff · FileChange              │
│             CodeAnalysis · CodeIssue · ReviewSuggestion         │
│             CompletedReview · IssueSeverity · IssueCategory     │
│   port/     PullRequestProvider (interface)                     │
│   service/  ReviewFormatter                                     │
└──────┬────────────────────────────────────────┬─────────────────┘
       │                                        │
┌──────▼──────────────────┐      ┌─────────────▼─────────────────┐
│    infraestructure/      │      │        application/           │
│  GitHubPullRequestAdapter│      │  ReviewController   POST /api │
│  GithubConfig            │      │  WebhookController  POST /wh  │
│  GithubProperties        │      └───────────────────────────────┘
└─────────────────────────┘
                                  ┌───────────────────────────────┐
                                  │           shell/              │
                                  │  ReviewCommands  fetch-pr     │
                                  └───────────────────────────────┘
```

**Dependency rules** (compile-time enforced):
- `domain` has **zero** dependencies on `infrastructure`, `agent`, Spring Web, or WebClient
- `infrastructure` has **zero** dependencies on `agent`

---

## Agent Pipeline

The `CodeReviewAgent` uses GOAP (Goal-Oriented Action Planning) via Embabel to execute four sequential actions:

```
UserInput
    │
    ▼
[1] parsePrInput       ──  LLM extracts owner / repo / prNumber from free text
    │
    ▼
[2] fetchPrDiff        ──  GitHub REST API: PR metadata + file list + raw diff
    │
    ▼
[3] analyzeCode        ──  LLM (CODE_REVIEWER persona) classifies issues and suggestions
    │
    ▼
[4] postReview         ──  Formats and posts review comment to the PR
```

The LLM persona is a **Senior Code Reviewer** — a principal engineer profile with 15 years of experience across Java, Kotlin, TypeScript, and Go, focused on clean architecture, security, and performance.

---

## Issue Classification

Every identified issue is classified along two dimensions:

**Severity**

| Level | Description |
|-------|-------------|
| `CRITICAL` | Bugs, security vulnerabilities — must fix before merge |
| `WARNING` | Performance issues, logic problems — should fix |
| `INFO` | Style, naming, maintainability — consider fixing |

**Category**

`BUG` · `SECURITY` · `PERFORMANCE` · `STYLE` · `MAINTAINABILITY`

---

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Language | Java 25 |
| Framework | Spring Boot 4.0.3 |
| Agent Orchestration | Embabel Agent 0.3.4 |
| AI / LLM | Spring AI (via Embabel) |
| HTTP | Spring WebFlux / WebClient |
| Shell | Spring Shell |
| Observability | Spring Boot Actuator |
| Architecture Tests | ArchUnit 1.4.1 |
| HTTP Mocking | WireMock 3.13.2 |
| Code Generation | Lombok |

---

## Prerequisites

- Java 25+
- Maven (wrapper included: `./mvnw`)
- A GitHub Personal Access Token with `repo` scope

---

## Configuration

Create a `.env` file at the project root (loaded automatically via `spring.config.import`):

```env
GITHUB_TOKEN=ghp_your_personal_access_token
GITHUB_WEBHOOK_SECRET=your_webhook_hmac_secret   # optional
OPENAI_API_KEY=open_ai_api_key
ANTHROPIC_API_KEY=anthropic_api_key
```

Or export as environment variables:

```bash
export GITHUB_TOKEN=ghp_your_personal_access_token
export GITHUB_WEBHOOK_SECRET=your_webhook_hmac_secret
export OPENAI_API_KEY=open_ai_api_key
export ANTHROPIC_API_KEY=anthropic_api_key
```

---

## Getting Started

### Build

```bash
./mvnw clean package -DskipTests
```

### Run

```bash
./mvnw spring-boot:run
```

### Run Tests

```bash
# All tests
./mvnw test

# Single test class
./mvnw test -Dtest=CodeReviewAgentTest
```

---

## Usage

### 1. REST API — Manual Review

Trigger a review for any pull request by sending a `POST` request:

```bash
curl -X POST http://localhost:8080/api/v1/reviews \
  -H "Content-Type: application/json" \
  -d '{
    "owner": "octocat",
    "repo":  "hello-world",
    "prNumber": 42
  }'
```

**Response:**

```json
{
  "owner": "octocat",
  "repo": "hello-world",
  "prNumber": 42,
  "title": "Fix null pointer in payment service",
  "filesChanged": 3,
  "reviewBody": "## PR Summary: Fix null pointer in payment service\n\n**Branch:** `fix/npe` → `main`\n\n### Files Changed (3)\n- `PaymentService.java` [modified] +12/-4\n...",
  "posted": true
}
```

---

### 2. GitHub Webhook — Automatic Review

Register a webhook in your GitHub repository settings:

```
Payload URL:   https://your-host/api/v1/webhook
Content type:  application/json
Events:        Pull requests
Secret:        <same value as GITHUB_WEBHOOK_SECRET>
```

The agent triggers automatically on `opened` and `synchronize` PR events. All other events are silently ignored. Signatures are verified via HMAC-SHA256.

---

### 3. Spring Shell — Interactive CLI

Start the application and use the interactive shell:

```bash
shell:> fetch-pr --owner octocat --repo hello-world --prNumber 42
```

**Output:**

```
--- PR #42: Fix null pointer in payment service ---
Description: Resolves NPE when payment amount is null
Branch: fix/npe → main
Files changed: 3

  src/main/java/PaymentService.java          [modified ] +12/-4
  src/test/java/PaymentServiceTest.java      [modified ] +20/-0
  src/main/resources/application.yml         [modified ] +2/-1
```

---

## Review Output Example

When the AI agent completes a full analysis, it posts a comment like this to the PR:

```markdown
## AI Code Review: Fix null pointer in payment service

### Overview
The changes address the NPE correctly. Input validation has been added in the
right layer. Two minor issues were found that should be addressed before merge.

### Issues Found
- [CRITICAL] **[SECURITY]** `PaymentService.java` (line ~34): User-supplied amount
  is passed to a format string without sanitization.
- [WARNING] **[PERFORMANCE]** `PaymentRepository.java` (line ~78): N+1 query pattern
  detected — consider using a JOIN fetch or batch loading.

### Suggestions
- `PaymentService.java`: Extract the validation logic into a dedicated
  `PaymentValidator` component to keep the service focused on orchestration.
  ```java
  // Before
  if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) { ... }

  // After
  paymentValidator.validate(amount);
  ```

---
*Generated by CODE Review Agent (Embabel + Spring AI)*
```

---

## API Reference

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/v1/reviews` | Trigger a manual review |
| `POST` | `/api/v1/webhook` | GitHub webhook receiver |
| `GET` | `/actuator/health` | Application health check |

---

## Project Structure

```
src/
├── main/java/com/marcos/codereviewagent/
│   ├── CodeReviewAgentApplication.java
│   ├── agent/
│   │   ├── CodeReviewAgent.java            # GOAP agent — 4 @Action methods
│   │   └── ReviewPersonas.java             # LLM persona definitions
│   ├── application/
│   │   ├── ReviewController.java           # POST /api/v1/reviews
│   │   ├── WebhookController.java          # POST /api/v1/webhook
│   │   ├── ReviewRequest.java
│   │   └── ReviewResponse.java
│   ├── domain/
│   │   ├── model/                          # Pure value objects (records)
│   │   ├── port/                           # PullRequestProvider interface
│   │   └── service/                        # ReviewFormatter
│   ├── infraestructure/
│   │   ├── GitHubPullRequestAdapter.java
│   │   ├── GithubConfig.java
│   │   └── GithubProperties.java
│   └── shell/
│       └── ReviewCommands.java             # fetch-pr shell command
└── test/
    ├── java/com/marcos/codereviewagent/
    │   ├── agent/                          # CodeReviewAgentTest
    │   ├── application/                    # ReviewControllerTest · WebhookControllerTest · WebhookSignatureTest
    │   ├── architecture/                   # ArchitectureTest (ArchUnit)
    │   ├── domain/                         # ReviewFormatterTest
    │   └── infraestructure/                # GitHubPullRequestAdapterTest
    └── testFixtures/java/.../fixtures/     # Shared test factories (Fixtures · TestUtils)
```

---

## Security

- Webhook payloads are verified with **HMAC-SHA256** using a shared secret
- Signature comparison uses `MessageDigest.isEqual` to prevent timing attacks
- Webhook verification is bypassed only when no secret is configured (development mode)

---

## License

MIT