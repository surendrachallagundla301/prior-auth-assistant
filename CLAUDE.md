# Agent instructions for prior-auth-assistant

Context for AI coding agents (Claude Code, Cursor, Copilot) working in this repo.
Read this before changing anything.

## What this is
A pre-submission checker for prior-authorization requests. It finds missing documentation
and invalid codes *before* a request goes to the payer, so it isn't denied later.

## Layout
- `backend/` – Java 17, Spring Boot 3, JPA, Spring Security. Rules live in
  `src/main/resources/rules/documentation-rules.json`.
- `frontend/` – React 18 + TypeScript (Vite). Jest for unit tests.
- `infra/` – Terraform for AWS. `policy/` – OPA/Rego guardrails run with conftest.
- `docs/specs/` – one spec per feature. Implement against the spec's acceptance criteria.

## Hard rules (PHI)
1. Every field that identifies a patient (name, member ID, date of birth) must use
   `@Convert(converter = EncryptedStringConverter.class)`.
2. Never put PHI in log messages, exception messages, audit `details`, or API error bodies.
   The API returns PHI masked via `PhiMasker` - never return raw identifiers.
3. Only synthetic data in tests and fixtures. No real names or member IDs.
4. New endpoints must be added to `SecurityConfig` with an explicit role. The default is `denyAll`.
5. Every state-changing action writes an `AuditEvent`.

## Conventions
- Constructor injection only. Records for DTOs.
- Business rules belong in `rules/`, not controllers.
- Every behaviour change needs a test: unit tests for rules, MockMvc tests for API and security.
- Infrastructure changes must pass `conftest test --parser hcl2 --policy policy infra/`.

## Commands
```bash
cd backend && mvn verify          # backend build + tests
cd frontend && npm test && npm run build
conftest test --parser hcl2 --policy policy infra/
```

## Definition of done
Acceptance criteria in the spec are met, tests pass in CI, and the change has been reviewed
against `docs/REVIEW_CHECKLIST.md`.
