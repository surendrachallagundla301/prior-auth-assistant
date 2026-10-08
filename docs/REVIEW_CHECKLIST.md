# Review checklist for AI-generated changes

AI agents write most of the first draft here. A person reviews every change against this list
before it merges, and owns it once it ships.

## Correctness
- [ ] Does it meet every acceptance criterion in the spec - and nothing extra?
- [ ] Are edge cases handled (empty input, lowercase codes, unknown procedure, missing payer)?
- [ ] Do the tests actually assert behaviour, or just that code runs?

## Security & PHI
- [ ] New identifier fields use `EncryptedStringConverter`.
- [ ] No PHI in logs, exception messages, audit details or API errors.
- [ ] New endpoints are listed in `SecurityConfig` with the narrowest role.
- [ ] No secrets, keys or real patient data committed.
- [ ] Input is validated at the API boundary.

## Quality
- [ ] Business logic lives in `rules/` or `service/`, not controllers.
- [ ] Names are clear; no dead code or speculative abstractions the agent added "just in case".
- [ ] Dependencies added by the agent are necessary, current and from trusted sources.

## Agent mistakes to watch for
- Logging a whole request object, which would leak PHI. Log IDs only.
- Returning raw `memberId` in a response DTO. Always mask through `PhiMasker`.
- A read-only transaction around a method that also writes an audit row (caught in review of
  `PriorAuthService#get`).
- A validation handler that echoes rejected values back to the client.
