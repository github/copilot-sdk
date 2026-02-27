# TCP Reconnection Sample

Tests that a **pre-running** `copilot` TCP server correctly handles **multiple sequential sessions**. The SDK connects, creates a session, exchanges a message, destroys the session, then repeats the process — verifying the server remains responsive across session lifecycles.

```
┌─────────────┐   TCP (JSON-RPC)   ┌──────────────┐
│  Your App   │ ─────────────────▶  │ Copilot CLI  │
│  (SDK)      │ ◀─────────────────  │ (TCP server) │
└─────────────┘                     └──────────────┘
     Session 1: create → send → destroy
     Session 2: create → send → destroy
```

## What This Tests

- The TCP server accepts a new session after a previous session is destroyed
- Server state is properly cleaned up between sessions
- The SDK client can reuse the same connection for multiple session lifecycles
- No resource leaks or port conflicts across sequential sessions

## Languages

| Directory | SDK / Approach | Language |
|-----------|---------------|----------|
| `typescript/` | `@github/copilot-sdk` | TypeScript (Node.js) |

> **TypeScript-only:** This scenario tests SDK-level session lifecycle over TCP. The reconnection behavior is an SDK concern, so only one language is needed to verify it.

## Prerequisites

- **Copilot CLI** — set `COPILOT_CLI_PATH`
- **Authentication** — set `GITHUB_TOKEN`, or run `gh auth login`
- **Node.js 20+** (TypeScript sample)

## Quick Start

Start the TCP server:

```bash
copilot --port 3000 --headless --auth-token-env GITHUB_TOKEN
```

Run the sample:

```bash
cd typescript
npm install && npm run build
COPILOT_CLI_URL=localhost:3000 npm start
```

## Verification

```bash
./verify.sh
```

Runs in three phases:

1. **Server** — starts `copilot` as a TCP server (auto-detects port)
2. **Build** — installs dependencies and compiles the TypeScript sample
3. **E2E Run** — executes the sample with a 120-second timeout, verifies both sessions complete and prints "Reconnect test passed"

The server is automatically stopped when the script exits.
