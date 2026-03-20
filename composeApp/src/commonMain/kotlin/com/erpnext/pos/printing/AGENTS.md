# AGENTS.md — Printing / Devices Critical Zone

This directory contains printer and device-sensitive logic.

## Primary goals

- deterministic ticket/receipt output
- safe connection lifecycle
- resilient transport handling
- preserved persisted printer configuration
- compatibility across supported platforms and transport modes

## Non-negotiables

- Keep receipt generation separate from transport code
- Do not tightly couple logic to a single vendor or SDK unless explicitly required
- Do not break existing Bluetooth/network/desktop-connected assumptions without explicit migration reasoning
- Do not let printer failures corrupt sales or payment persistence flows

## Every change here must evaluate

- formatting consistency
- timeout behavior
- reconnection handling
- transport-specific failure paths
- configuration persistence compatibility
- cross-platform behavior differences
- safe user-facing fallback messages

## Verification expectations

At minimum, validate:

- one happy path print flow
- one disconnected/failure path
- one persisted configuration load/use path
- one currency/totals formatting check on the rendered ticket
