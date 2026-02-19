# App-Backend-to-Server Samples

Samples that demonstrate the **app-backend-to-server** deployment architecture of the Copilot SDK. In this scenario a web backend connects to a **pre-running** `copilot` TCP server and exposes a `POST /chat` HTTP endpoint. The HTTP server receives a prompt from the client, forwards it to Copilot CLI, and returns the response.

```
┌────────┐   HTTP POST /chat   ┌─────────────┐   TCP (JSON-RPC)   ┌──────────────┐
│ Client │ ──────────────────▶  │ Web Backend  │ ─────────────────▶  │ Copilot CLI  │
│ (curl) │ ◀──────────────────  │ (HTTP server)│ ◀─────────────────  │ (TCP server) │
└────────┘                      └─────────────┘                     └──────────────┘
```

Each sample follows the same flow:

1. **Start** an HTTP server with a `POST /chat` endpoint
2. **Receive** a JSON request `{ "prompt": "..." }`
3. **Connect** to a running `copilot` server via TCP
4. **Open a session** targeting the `gpt-4.1` model
5. **Forward the prompt** and collect the response
6. **Return** a JSON response `{ "response": "..." }`

## Languages

| Directory | SDK / Approach | Language | HTTP Framework |
|-----------|---------------|----------|----------------|
| `typescript/` | `@github/copilot-sdk` | TypeScript (Node.js) | Express |
| `python/` | `github-copilot-sdk` | Python | Flask |
| `go/` | `github.com/github/copilot-sdk/go` | Go | net/http |

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
npm install && npm run build
CLI_URL=localhost:3000 npm start
# In another terminal:
curl -X POST http://localhost:8080/chat \
  -H "Content-Type: application/json" \
  -d '{"prompt": "What is the capital of France?"}'
```

**Python**
```bash
cd python
pip install -r requirements.txt
CLI_URL=localhost:3000 python main.py
# In another terminal:
curl -X POST http://localhost:8080/chat \
  -H "Content-Type: application/json" \
  -d '{"prompt": "What is the capital of France?"}'
```

**Go**
```bash
cd go
CLI_URL=localhost:3000 go run main.go
# In another terminal:
curl -X POST http://localhost:8080/chat \
  -H "Content-Type: application/json" \
  -d '{"prompt": "What is the capital of France?"}'
```

All samples default to `localhost:3000` for the Copilot CLI and port `8080` for the HTTP server. Override with `CLI_URL` (or `COPILOT_CLI_URL`) and `PORT` environment variables:

```bash
CLI_URL=localhost:4000 PORT=9090 npm start
```

## Verification

A script is included that starts the server, builds, and end-to-end tests every sample:

```bash
./verify.sh
```

It runs in three phases:

1. **Server** — starts `copilot` on a random port
2. **Build** — installs dependencies and compiles each sample
3. **E2E Run** — starts each HTTP server, sends a `POST /chat` request via curl, and verifies it returns a response

The server is automatically stopped when the script exits.
