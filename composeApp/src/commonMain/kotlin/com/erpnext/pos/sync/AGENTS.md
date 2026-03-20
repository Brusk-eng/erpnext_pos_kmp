# AGENTS.md — Sync Critical Zone

This directory is a critical offline-first reconciliation boundary.

## Primary goals

- deterministic sync flow
- idempotent remote mutations
- safe retry behavior
- explicit conflict handling
- safe local upserts
- reliable remote identifier reconciliation

## Required sync sequence

1. validate session
2. refresh token if needed
3. push pending mutations
4. capture/confirm ERPNext identifiers
5. pull incremental remote changes
6. upsert local state
7. emit explicit result

## Non-negotiables

- Never introduce fire-and-forget remote writes
- Never silently discard unsynced local changes
- Never overwrite local pending data without explicit conflict rules
- Never remove local records without a defined deletion strategy
- Never weaken logging around retries, failures, or reconciliation

## Every change here must evaluate

- idempotency
- retries
- partial failure behavior
- resumability
- duplicate submit prevention
- timestamp / cursor correctness
- mapping between local IDs and remote IDs
- sanitized observability

## Verification expectations

At minimum, validate:

- one successful sync path
- one retry/failure path
- one partial failure or interrupted flow
- reconciliation of remote IDs after push
