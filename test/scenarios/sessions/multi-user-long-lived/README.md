# Multi-User Long-Lived Sessions

Demonstrates a **production-like multi-user setup** where multiple clients share a single `copilot` server with **persistent, long-lived sessions** stored on disk.

## Architecture

```
┌──────────────────────┐
│    Copilot CLI       │  (headless TCP server)
│    (shared server)    │
└───┬──────┬───────┬───┘
    │      │       │   JSON-RPC over TCP (cliUrl)
    │      │       │
┌───┴──┐ ┌┴────┐ ┌┴─────┐
│ C1   │ │ C2  │ │  C3  │
│UserA │ │UserA│ │UserB │
│Sess1 │ │Sess1│ │Sess2 │
│      │ │(resume)│     │
└──────┘ └─────┘ └──────┘
```

## What This Demonstrates

1. **Shared server** — A single `copilot` instance serves multiple users and sessions over TCP.
2. **Per-user config isolation** — Each user gets their own `configDir` on disk (`tmp/user-a/`, `tmp/user-b/`), so configuration, logs, and state are fully separated.
3. **Session sharing across clients** — User A's Client 1 creates a session and teaches it a fact. Client 2 resumes the same session (by `sessionId`) and retrieves the fact — demonstrating cross-client session continuity.
4. **Session isolation between users** — User B operates in a completely separate session and cannot see User A's conversation history.
5. **Disk persistence** — Session state is written to a real `tmp/` directory, simulating production persistence (cleaned up after the run).

## What Each Client Does

| Client | User | Action |
|--------|------|--------|
| **C1** | A | Creates session `user-a-project-session`, teaches it a codename |
| **C2** | A | Resumes `user-a-project-session`, confirms it remembers the codename |
| **C3** | B | Creates separate session `user-b-solo-session`, verifies it has no knowledge of User A's data |

## Configuration

| Option | User A | User B |
|--------|--------|--------|
| `cliUrl` | Shared server | Shared server |
| `configDir` | `tmp/user-a/` | `tmp/user-b/` |
| `sessionId` | `user-a-project-session` | `user-b-solo-session` |
| `availableTools` | `[]` | `[]` |

## When to Use This Pattern

- **SaaS platforms** — Each tenant gets isolated config and persistent sessions
- **Team collaboration tools** — Multiple team members share sessions on the same project
- **IDE backends** — User opens the same project in multiple editors/tabs

## Run

```bash
./verify.sh
```

Requires the `copilot` binary (auto-detected or set `COPILOT_CLI_PATH`) and `GITHUB_TOKEN`.
