# Prior Authorization Assistant

[![CI](https://github.com/surendrachallagundla301/prior-auth-assistant/actions/workflows/ci.yml/badge.svg)](https://github.com/surendrachallagundla301/prior-auth-assistant/actions/workflows/ci.yml)
[![CodeQL](https://github.com/surendrachallagundla301/prior-auth-assistant/actions/workflows/codeql.yml/badge.svg)](https://github.com/surendrachallagundla301/prior-auth-assistant/actions/workflows/codeql.yml)

A healthcare SaaS prototype that checks **prior-authorization requests for missing documentation
and invalid codes before they are submitted to the payer**, so problems are fixed upstream instead
of coming back as claim denials.

> All data in this project is synthetic. Payers ("Acme Health Plan", "Summit Care") are fictional.

## Why
Missing documentation is one of the most common reasons prior-authorization requests are denied.
Each denial means rework, delayed care and lost revenue for the provider. Catching the gap before
submission is far cheaper than appealing afterwards.

## What it does
1. An intake coordinator enters a request: patient, payer, procedure code (CPT/HCPCS), diagnosis
   codes (ICD-10) and the documents attached.
2. The rules engine looks up what that payer needs for that procedure and reports:
   - missing documents
   - invalid or unknown codes
   - a **denial risk** (LOW / MEDIUM / HIGH)
3. The request is marked **Ready for submission** or **Needs documentation**, and every step is audited.

## Architecture
```
React + TypeScript (Vite)
        │  HTTPS, Basic auth (demo) → SSO in production
        ▼
Spring Boot 3 API ──► Rules engine (JSON rules per procedure + payer overrides)
        │
        ├─► JPA ──► H2 (dev) / PostgreSQL (prod)   PHI columns AES-256-GCM encrypted
        └─► Audit trail (no PHI)                   Logback converter scrubs PHI from logs

Terraform (AWS): S3 + SSE-KMS, KMS key rotation, encrypted CloudWatch logs
OPA/Rego policies (conftest) block non-compliant infrastructure in CI
```

## PHI safeguards
| Safeguard | How |
|---|---|
| Encryption at rest | `EncryptedStringConverter` encrypts name, member ID and DOB with AES-256-GCM |
| Minimum necessary | API returns masked values only (`J*** D**`, `********6789`) |
| No PHI in logs | `PhiMaskingConverter` scrubs member IDs and dates from every log line |
| No PHI in errors | Validation errors name the field, never the value |
| Access control | Roles: INTAKE, REVIEWER, AUDITOR. Everything else is denied |
| Audit | Create, view and check actions are recorded with actor and timestamp |
| Infrastructure | OPA policies require KMS encryption, key rotation, no public access, 365-day encrypted logs |

## How it was built: AI-first, spec-driven
This project was built by directing AI coding agents (Claude Code, Cursor) rather than writing most
code by hand:
1. Each feature starts as a spec with acceptance criteria in [`docs/specs/`](docs/specs).
2. [`CLAUDE.md`](CLAUDE.md) gives agents the project rules: PHI handling, layering, testing.
3. The agent implements against the spec; every change is reviewed with
   [`docs/REVIEW_CHECKLIST.md`](docs/REVIEW_CHECKLIST.md), which also lists the agent mistakes to
   watch for (e.g. logging a whole request object, which would leak PHI).
4. CI is the final gate: tests, CodeQL, OPA policies, Checkov and (optionally) Snyk.

## Run it locally
Requirements: Java 17+, Maven, Node 20+.

```bash
# API on http://localhost:8080
cd backend
mvn spring-boot:run

# UI on http://localhost:5173
cd frontend
npm install
npm run dev
```

Demo users (in-memory, for local use only):

| User | Password | Role |
|---|---|---|
| intake | intake-demo | INTAKE |
| reviewer | reviewer-demo | REVIEWER |
| auditor | auditor-demo | AUDITOR |

Try it with curl:
```bash
curl -u intake:intake-demo -H 'Content-Type: application/json' localhost:8080/api/requests -d '{
  "patientName": "Jane Doe", "memberId": "ABC123456789", "dateOfBirth": "1985-04-12",
  "payer": "ACME_HEALTH", "cptCode": "27447", "icd10Codes": ["M17.11"],
  "submittedDocuments": ["PHYSICIAN_ORDER", "CLINICAL_NOTES", "IMAGING_REPORT", "PRIOR_TREATMENT_HISTORY"]
}'
curl -u intake:intake-demo -X POST localhost:8080/api/requests/1/check
# -> readyForSubmission: false, missing MEDICAL_NECESSITY_LETTER, denialRisk: MEDIUM
```

Set `PHI_ENCRYPTION_KEY` (base64, 32 bytes) outside local development. Use the `postgres` Spring
profile with `DB_URL`, `DB_USER` and `DB_PASSWORD` for PostgreSQL.

## Tests
```bash
cd backend && mvn verify                 # JUnit 5, Mockito, MockMvc, Spring Security Test
cd frontend && npm test                  # Jest
conftest test --parser hcl2 --policy policy infra/   # OPA guardrails
```

## Tech stack
Java 17 · Spring Boot 3 · Spring Security · JPA/Hibernate · PostgreSQL/H2 · React 18 · TypeScript ·
Vite · Jest · JUnit 5 · Terraform · AWS (S3, KMS, CloudWatch) · OPA/Rego · conftest · Checkov ·
CodeQL · Snyk · GitHub Actions

## License
MIT
