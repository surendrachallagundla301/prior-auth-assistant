# Spec 002: PHI protection

## Goal
Treat patient identifiers as PHI from the first line of code, following HIPAA-style safeguards:
encryption at rest, minimum necessary disclosure, access control and audit.

## Requirements
1. **Encryption at rest** – patient name, member ID and date of birth are encrypted with
   AES-256-GCM (random IV per value) before they reach the database.
2. **Minimum necessary** – the API never returns full identifiers: names become initials,
   member IDs show only the last 4 digits, dates of birth are fully masked.
3. **No PHI in logs** – a Logback converter scrubs member IDs and dates from every log line.
4. **No PHI in errors** – validation errors name the field but never echo the value.
5. **Role-based access** – INTAKE creates, REVIEWER reads and checks, AUDITOR reads the audit trail.
   Anything not explicitly allowed is denied.
6. **Audit trail** – creating, viewing and checking a request each write an audit event with
   actor, action, request ID and timestamp, and no PHI.
7. **Infrastructure** – PHI storage uses SSE-KMS with key rotation, blocks public access, and
   keeps encrypted logs for 365 days. Enforced by OPA policies in CI.

## Acceptance criteria
- [x] Raw database values for PHI columns do not contain the plaintext (`PriorAuthControllerTest#phiIsEncryptedAtRest`).
- [x] Tampered ciphertext fails to decrypt (`PhiEncryptorTest`).
- [x] API responses contain masked values only.
- [x] Wrong role gets 403, no credentials gets 401 (`PriorAuthControllerTest#rolesAreEnforced`).
- [x] Validation errors do not echo submitted values.
- [x] Every action appears in `/api/audit` (`PriorAuthControllerTest#everyActionIsAudited`).
- [x] OPA policies pass on `infra/` and fail on `policy/testdata/insecure.tf` (CI).
