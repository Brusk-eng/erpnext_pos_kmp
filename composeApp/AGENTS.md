# AGENTS.md — composeApp (Shared KMP Core)

This directory contains the shared core of the POS:
business logic, UI, sync orchestration, local persistence, remote integrations,
and shared abstractions used across Android, iOS, and Desktop.

## Expectations

- Prefer shared implementations over platform-specific ones unless truly required.
- Do not leak Android/iOS/Desktop APIs into shared code.
- Keep business rules in domain/use case layers, not in UI composables.
- Keep repositories focused on orchestration and data access, not presentation concerns.
- Preserve offline-first guarantees.
- Protect consistency across targets: Android, iOS, Desktop.

## Change discipline

Before modifying logic, inspect the affected:

- use case
- repository
- mapper
- entity/DAO
- DTO/API
- sync path
- UI state holder

## High-risk zones inside composeApp

- `domain/sync/`
- `sync/`
- `remoteSource/oauth/`
- `localSource/entities/`
- `localSource/dao/`
- payment / invoice / billing flows
- printer/device configuration flows

## Non-negotiables

- No silent behavior changes in totals, sync, auth, or persistence.
- No broad architectural rewrites unless explicitly requested.
- No new dependencies without clear multiplatform justification.
- Prefer minimal diffs and existing repo patterns.
