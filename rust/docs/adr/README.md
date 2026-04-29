# Architecture Decision Records — Rust SDK

Architecture Decision Records (ADRs) capture significant, hard-to-reverse
design decisions for the `github-copilot-sdk` crate. Each ADR documents the
context, alternatives considered, decision, and consequences.

When to write an ADR:

- New public traits, types, or modules in the crate's public API surface.
- Concurrency or threading-model choices (sync vs `async_trait`,
  per-session sequencing vs concurrent dispatch).
- New dependency patterns or module organization.
- Anything tagged "hard to reverse post-1.0".

Format: short, action-oriented, ASCII-only diagrams. Numbered sequentially.

## Index

| #    | Status   | Title                                          |
| ---- | -------- | ---------------------------------------------- |
| 0001 | Proposed | [SessionFsProvider trait and plumbing][0001]   |

[0001]: ./0001-session-fs-provider.md
