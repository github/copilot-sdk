# Multi-User Short-Lived Sessions

Demonstrates a **stateless backend pattern** where multiple users interact with a shared `copilot` server through **ephemeral sessions** that are created and destroyed per request, with per-user virtual filesystems for isolation.

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
│(new) │ │(new)│ │(new) │
└──────┘ └─────┘ └──────┘

Each request → new session → destroy after response
Virtual FS per user (in-memory, not shared across users)
```

## What This Demonstrates

1. **Ephemeral sessions** — Each interaction creates a fresh session and destroys it immediately after. No state persists between requests on the server side.
2. **Per-user virtual filesystem** — Custom tools (`write_file`, `read_file`, `list_files`) backed by in-memory Maps. Each user gets their own isolated filesystem instance — User A's files are invisible to User B.
3. **Application-layer state** — While sessions are stateless, the application maintains state (the virtual FS) between requests for the same user. This mirrors real backends where session state lives in your database, not in the LLM session.
4. **Custom tools** — Uses `defineTool` with `availableTools: []` to replace all built-in tools with a controlled virtual filesystem.
5. **Multi-client isolation** — User A's two clients share the same virtual FS (same user), but User B's virtual FS is completely separate.

## What Each Client Does

| Client | User | Action |
|--------|------|--------|
| **C1** | A | Creates `notes.md` in User A's virtual FS |
| **C2** | A | Lists files and reads `notes.md` (sees C1's file because same user FS) |
| **C3** | B | Lists files in User B's virtual FS (empty — completely isolated) |

## Configuration

| Option | Value |
|--------|-------|
| `cliUrl` | Shared server |
| `availableTools` | `[]` (no built-in tools) |
| `tools` | `[write_file, read_file, list_files]` (per-user virtual FS) |
| `sessionId` | Auto-generated (ephemeral) |

## When to Use This Pattern

- **API backends** — Stateless request/response with no session persistence
- **Serverless functions** — Each invocation is independent
- **High-throughput services** — No session overhead between requests
- **Privacy-sensitive apps** — Conversation history never persists

## Run

```bash
./verify.sh
```

Requires the `copilot` binary (auto-detected or set `COPILOT_CLI_PATH`) and `GITHUB_TOKEN`.
