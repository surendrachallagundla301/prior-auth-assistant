# Spec 001: Pre-submission documentation check

## User story
As an **intake coordinator**, I want to check a prior-authorization request against the payer's
documentation rules before I submit it, so that I can attach anything missing and avoid a denial.

## Scope
- Rules are data (`documentation-rules.json`): required documents per procedure code, plus
  payer-specific additions.
- Validate code formats: CPT (5 digits) or HCPCS Level II (letter + 4 digits); ICD-10-CM.
- Output: required documents, missing documents, denial risk, and a list of issues.

## Out of scope
- Medical-necessity judgement (clinical review stays with people).
- Submitting to a real payer.

## Denial risk
| Condition | Risk |
|---|---|
| Nothing missing, all codes valid | LOW |
| Exactly one required document missing | MEDIUM |
| Two or more missing, any invalid code, or unknown procedure | HIGH |

## Acceptance criteria
- [x] A complete request returns `readyForSubmission = true` and `LOW` risk.
- [x] One missing document returns `MEDIUM` and names the document.
- [x] Payer overrides add documents (e.g. ACME_HEALTH requires a medical necessity letter for 27447).
- [x] An invalid ICD-10 code returns `HIGH` even when all documents are attached.
- [x] An unknown procedure is flagged for manual review with `HIGH` risk.
- [x] Codes are accepted in lowercase and normalised.
- [x] Running a check updates the request status and writes an audit event.

Tests: `DocumentationRulesEngineTest`, `PriorAuthControllerIT#checkFlagsPayerSpecificMissingDocument`.
