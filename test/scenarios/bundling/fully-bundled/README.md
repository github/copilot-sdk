# Fully-Bundled Samples

Self-contained samples that demonstrate the **fully-bundled** deployment architecture of the Copilot SDK. In this scenario the SDK spawns `copilot` as a child process over stdio — no external server or container is required.

Each sample follows the same flow:

1. **Create a client** that spawns `copilot` automatically
2. **Open a session** targeting the `gpt-4.1` model
3. **Send a prompt** ("What is the capital of France?")
4. **Print the response** and clean up

## Languages

| Directory | SDK / Approach | Language |
|-----------|---------------|----------|
| `typescript/` | `@github/copilot-sdk` | TypeScript (Node.js) |
| `typescript-wasm/` | `@github/copilot-sdk` with WASM runtime | TypeScript (Node.js) |
| `python/` | `github-copilot-sdk` | Python |
| `go/` | `github.com/github/copilot-sdk/go` | Go |

## Prerequisites

- **Copilot CLI** — set `COPILOT_CLI_PATH`
- **Authentication** — set `GITHUB_TOKEN`, or run `gh auth login`
- **Node.js 20+** (TypeScript samples)
- **Python 3.10+** (Python sample)
- **Go 1.24+** (Go sample)

## Quick Start

**TypeScript**
```bash
cd typescript
npm install && npm run build && npm start
```

**TypeScript (WASM)**
```bash
cd typescript-wasm
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

A script is included to build and end-to-end test every sample:

```bash
./verify.sh
```

It runs in two phases:

1. **Build** — installs dependencies and compiles each sample
2. **E2E Run** — executes each sample with a 60-second timeout and verifies it produces output

Set `COPILOT_CLI_PATH` to point at your `copilot` binary if it isn't in the default location.
