# Stdio Transport Samples

Samples demonstrating the **stdio** transport model. The SDK spawns `copilot` as a child process and communicates over standard input/output using Content-Length-framed JSON-RPC 2.0 messages.

```
┌─────────────┐   stdin/stdout (JSON-RPC)   ┌──────────────┐
│  Your App   │ ──────────────────────────▶  │ Copilot CLI  │
│  (SDK)      │ ◀──────────────────────────  │ (child proc) │
└─────────────┘                              └──────────────┘
```

Each sample follows the same flow:

1. **Create a client** that spawns `copilot` automatically
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

## Verification

```bash
./verify.sh
```

Runs in two phases:

1. **Build** — installs dependencies and compiles each sample
2. **E2E Run** — executes each sample with a 60-second timeout and verifies it produces output
