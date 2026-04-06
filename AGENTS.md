# AGENTS.md — erpnext_pos_kmp (ERPNext POS KMP, Offline-first)

---

# 1. Mission & Non-Negotiable

This repository implements an offline-first POS built with Kotlin Multiplatform + Compose,
integrated with ERPNext/Frappe APIs.

Primary success criteria:

- Data integrity > UI polish
- Idempotent remote operations (no duplicate submits)
- Deterministic sync flow (push → pull)
- Multi-currency correctness (NIO/USD) with strict rounding discipline
- No secrets in logs
- No production data in fixtures
- Small, reviewable, low-risk changes
- Cross-platform reliability over platform-specific shortcuts

This project prioritizes reliability, predictability, and architectural clarity over speed of
feature delivery.

---

# 2. Language Policy

- Code, comments, commit messages, and technical plans MUST be written in English.
- If the user asks in Spanish, explanations may be provided in Spanish.
- Technical sections (plans, diffs, architecture decisions) remain in English.
- Domain-specific Nicaraguan business terminology may be documented in Spanish in `docs/` if
  required.

---

# 3. Repo Structure & Responsibility Boundaries

Primary work happens inside:

- `composeApp/` → shared business logic + UI + sync + data layers
- `androidApp/` → thin Android wrapper
- `iosApp/` → thin iOS wrapper
- `docs/` → ADRs, architectural notes
- `config/detekt/` → static analysis rules
- `.githooks/` → quality enforcement

Within `composeApp/src/commonMain/...`:

### Domain Layer (pure business logic)

- `domain/models/`
- `domain/usecases/`
- `domain/ports/`
- `domain/policy/`
- `domain/sync/`

Domain must NOT depend on:

- DTOs
- Database entities
- Ktor responses
- Platform APIs

### Data Layer (implementations)

- `data/repositories/`
- `data/mappers/`
- `data/adapters/`

### Local Layer

- `localSource/dao/`
- `localSource/entities/`
- `localSource/preferences/`
- `localSource/datasources/`

### Remote Layer

- `remoteSource/api/`
- `remoteSource/dto/`
- `remoteSource/oauth/`
- `remoteSource/sdk/`
- `remoteSource/paging/`

### Sync Layer

- `sync/`
- `domain/sync/`

This is a high-risk zone. All changes require strict validation.

---

# 4. Multiplatform & Form-Factor Discipline

Supported targets:

- Android
- iOS
- Desktop (Windows, Linux, macOS)

Supported form factors:

- Phone
- Tablet
- Desktop

Non-negotiables:

- Shared business logic must remain in shared code unless platform-specific behavior is truly required.
- Platform-specific implementations must stay behind explicit abstractions.
- Do not leak Android/iOS/Desktop APIs into shared code without an existing approved boundary.
- UI changes must consider compact, medium, and expanded layouts.
- Desktop interaction must consider keyboard, pointer, hover, and window resizing.
- Mobile interaction must consider touch ergonomics, orientation changes, and limited space.
- Avoid hardcoded dimensions that break on tablet or desktop layouts.
- Prefer adaptive layouts over platform forks unless behavior genuinely differs.

---

# 5. Working Agreement (Execution Protocol)

Before coding:

1. Identify affected modules (UI/domain/sync/local/remote/platform).
2. Provide a short technical plan (max 8 bullets).
3. Identify risks (money, auth, sync, schema, printing, platform compatibility).
4. Keep change scope minimal (KISS).
5. Search existing patterns before introducing a new one.

After coding:

1. Run mandatory checks.
2. Add or update tests for high-risk logic.
3. Provide:
  - Summary of changes
  - Verification steps
  - Risk assessment
  - Rollback notes when relevant

---

# 6. Build & Quality Gates

From repository root:

Install hooks:

- `./scripts/install-git-hooks.sh`

Build:

- `./gradlew clean build`

Tests:

- `./gradlew test`

Static analysis:

- `./gradlew detekt`

Lint (if configured):

- `./gradlew lint`

If unsure:

- `./gradlew tasks`

No change is complete unless relevant quality gates pass.

Prefer smallest valid verification scope first:

1. compile affected module/target
2. run focused tests
3. run detekt/lint if impacted
4. run broader build/test sweep only when risk justifies it

---

# 7. SOLID Enforcement Rules

Single Responsibility:

- No "God classes"
- Classes should have one reason to change

Open/Closed:

- Prefer extension via ports/interfaces
- Avoid modifying stable contracts

Liskov:

- Do not break substitutability
- Tests must validate behavior compatibility

Interface Segregation:

- Prefer multiple small ports over one large repository

Dependency Inversion:

- Domain depends only on abstractions
- Implementations live in data/local/remote

---

# 8. KISS Enforcement Rules

- Prefer smallest viable solution
- Avoid premature abstraction
- Avoid unnecessary generics
- Avoid over-architecting
- One concern per change
- No unrelated refactors

If existing pattern exists → extend it.
Do not invent new patterns unless justified.

---

# 9. Sync Rules (Offline-First Critical Section)

Canonical flow:

1. Validate session
2. Refresh token if needed
3. Push pending mutations
4. Confirm remote identifiers
5. Pull incrementals (paged)
6. Upsert local state
7. Emit explicit result (Success / Partial / Error)

Non-negotiables:

- All remote writes must be idempotent
- No fire-and-forget submits
- Capture ERPNext document ID/name
- Never delete local data without strategy (soft delete / tombstones)
- No silent conflict resolution
- Never assume network availability for critical POS flows
- Local persistence is the operational source of truth while offline

All sync modifications require:

- Retry reasoning
- Idempotency reasoning
- Explicit logging strategy
- Conflict handling reasoning
- Partial failure reasoning

---

# 10. Money & Multi-Currency Discipline

- Never use Double for monetary calculations
- Prefer integer minor units or precise decimal strategy already used by the project
- Apply rounding only at defined boundaries:
  - line
  - tax
  - total
- FX rate must be explicit
- Never mix currency contexts silently
- Printed totals must match persisted totals
- Payment allocation must remain deterministic

Any change touching:

- `payment/`
- `paymententry/`
- `billing/`
- `invoice/`

Requires:

- Unit test for rounding edge case
- FX scenario validation
- Verification that totals shown, persisted, synced, and printed remain aligned

---

# 11. Auth & OAuth Rules

- Never log tokens or secrets
- Always sanitize error logs
- Explicit token refresh handling
- Clear separation between:
  - Session validation
  - Token refresh
  - API call retry

Auth failures must:

- Provide safe user message
- Provide sanitized developer log

---

# 12. Database & Local Persistence Rules

For Room/SQLite changes:

- Inspect entities, DAO queries, mappers, and migrations
- Preserve schema compatibility or include a safe migration
- Evaluate nullability, defaults, and existing-installation impact
- Verify indexes for high-frequency lookup paths
- Avoid broad or expensive queries in hot POS flows
- Do not move heavy database work to the UI thread

Any schema-related change requires:

- Migration reasoning
- Data preservation reasoning
- Verification steps for existing local data

---

# 13. Networking & Remote Contract Rules

For Ktor/API changes:

- Preserve backward compatibility unless explicitly told otherwise
- Inspect DTOs, serializers, mappers, auth flow, and error handling
- Keep API contracts explicit
- Do not couple UI directly to raw API models
- Prefer mapping into domain or stable internal models before business use
- Handle offline, timeout, auth, and server failures explicitly

---

# 14. UI, State, and Performance Rules

- Keep business rules out of composables
- Keep UI state explicit and testable
- Avoid hidden side effects in presentation logic
- Protect startup time, cart responsiveness, search responsiveness, and checkout latency
- Avoid unnecessary recompositions, repeated DB queries, duplicated state, and large in-memory transforms in hot paths
- Be careful with Flow, coroutine scope, and state propagation
- Prefer adaptive layouts and reusable UI patterns already established in the repo

Any change affecting these screens is medium-to-high risk:

- product list
- cart
- checkout
- payments
- invoice detail
- sync status
- printer settings

---

# 15. Printing & Hardware Rules

Printing is a critical operational feature.

Any change affecting printers, tickets, or hardware integrations must verify:

- formatting consistency
- connection lifecycle
- reconnection behavior
- timeout handling
- persisted configuration compatibility
- platform compatibility across Android/Desktop where applicable
- safe fallback/error states

Non-negotiables:

- Do not tightly couple printing logic to one brand or SDK unless explicitly requested
- Keep receipt/ticket generation separate from transport concerns
- Preserve deterministic output for totals, taxes, currency, and line items

---

# 16. Logging & Observability

Logs must help diagnose:

- Session expiration
- Sync retries
- ERPNext server errors (sanitized)
- Mapping errors
- Printer failures
- Queue or reconciliation failures

Never log:

- OAuth tokens
- Refresh tokens
- Passwords
- Full PII
- Sensitive financial secrets beyond what is operationally necessary

---

# 17. Documentation & Commenting Policy

Goal: clarity without noise.

### Must document:

- Public APIs
- Ports
- Use cases
- Domain models with invariants
- Sync idempotency strategy
- Business rules (taxes, FX, rounding)
- Platform-specific adapters when behavior is non-obvious

### Comment the WHY, not the WHAT.

Do NOT:

- Comment obvious code
- Write narrative essays inside functions
- Duplicate logic explanation already clear from naming

Prefer:

- Small functions
- Clear naming
- Extracted helpers
- Sealed result types

Long explanations belong in:
`docs/adr/XXXX-title.md`

---

# 18. Risk Classification

Safe changes:

- UI improvements within one feature
- Localized bug fixes
- Small mapper corrections
- Non-behavioral cleanup in one file

Risky changes:

- `sync/`
- `domain/sync/`
- `remoteSource/oauth/`
- multi-currency flows
- schema changes
- database entities
- printer/device integrations
- shared abstractions that affect multiple targets

Risky changes require:

- Explicit reasoning
- Test coverage where feasible
- Verification steps
- Rollback notes when relevant

---

# 19. Output Format (Mandatory)

Responses must follow:

1. Plan (max 8 bullets)
2. Files changed + summary
3. Commands executed (or to execute)
4. Verification steps
5. Risks & rollback notes

Keep explanations concise.
Prefer action over narrative.

---

# 20. Shell & Repo Hygiene

Prefer RTK wrappers for noisy commands when available:

- `rtk git diff`
- `rtk grep`
- `rtk ls`

Do NOT:

- introduce new libraries without justification
- refactor unrelated code
- rename packages broadly
- change contracts without updating all callers
- hardcode environment URLs
- expose secrets
- commit, push, rebase, or delete branches unless explicitly requested

Prefer small, reviewable patches over broad rewrites.

---

# 21. Definition of Done

- Builds successfully for the impacted scope
- Tests pass for the impacted scope
- No new detekt violations introduced
- Critical flows validated
- Logs are meaningful and sanitized
- Change scope is minimal and justified
- Cross-platform behavior remains consistent for the affected feature

---

# 22. If Uncertain

- Search existing implementations first
- Follow existing patterns
- Ask for the smallest missing detail only if necessary
- Default to minimal change strategy
- Choose safety over speed in money, sync, auth, schema, and printing paths

---

End of AGENTS.md