# App-Direct-Server Samples

Samples that demonstrate the **app-direct-server** deployment architecture of the Copilot SDK. In this scenario the SDK connects to a **pre-running** `copilot` TCP server — the app does not spawn or manage the server process.

```
┌─────────────┐   TCP (JSON-RPC)   ┌──────────────┐
│  Your App   │ ─────────────────▶  │ Copilot CLI  │
│  (SDK)      │ ◀─────────────────  │ (TCP server) │
└─────────────┘                     └──────────────┘
```

Each sample follows the same flow:

1. **Connect** to a running `copilot` server via TCP
2. **Open a session** targeting the `gpt-4.1` model
3. **Send a prompt** ("What is the capital of France?")
4. **Print the response** and clean up

## Languages

| Directory | SDK / Approach | Language |
|-----------|---------------|----------|
| `typescript/` | `@github/copilot-sdk` | TypeScript (Node.js) |
| `python/` | `github-copilot-sdk` | Python |
| `go/` | `github.com/github/copilot-sdk/go` | Go |

## Prerequisites

- **Copilot CLI** — set `COPILOT_CLI_PATH`
- **Authentication** — set `GITHUB_TOKEN`, or run `gh auth login`
- **Node.js 20+** (TypeScript sample)
- **Python 3.10+** (Python sample)
- **Go 1.24+** (Go sample)

## Starting the Server

Start `copilot` as a TCP server before running any sample:

```bash
copilot --port 3000 --headless --auth-token-env GITHUB_TOKEN
```

## Quick Start

**TypeScript**
```bash
cd typescript
npm install && npm run build && npm start
```

**Python**
```bash
cd python
pip install -r requirements.txt
python main.py
```

**Go**
```bash
cd go
go run main.go
```

All samples default to `localhost:3000`. Override with the `COPILOT_CLI_URL` environment variable:

```bash
COPILOT_CLI_URL=localhost:8080 npm start
```

## Verification

A script is included that starts the server, builds, and end-to-end tests every sample:

```bash
./verify.sh
```

It runs in three phases:

1. **Server** — starts `copilot` on a random port (auto-detected from server output)
2. **Build** — installs dependencies and compiles each sample
3. **E2E Run** — executes each sample with a 60-second timeout and verifies it produces output

The server is automatically stopped when the script exits.
