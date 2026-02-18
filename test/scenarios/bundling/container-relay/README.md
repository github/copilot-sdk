# Container-Relay Samples

Run copilot-core inside a Docker container with the built-in **relay** command on the host replacing the external proxy. This demonstrates the same deployment pattern as [container-proxy](../container-proxy/) but uses `copilot-core relay` instead of a separate `proxy.py` script.

```
  Host Machine
┌──────────────────────────────────────────────────────┐
│                                                      │
│  ┌─────────────┐                                     │
│  │  Your App   │   TCP :3000                         │
│  │  (SDK)      │ ────────────────┐                   │
│  └─────────────┘                 │                   │
│                                  ▼                   │
│                    ┌──────────────────────────┐       │
│                    │  Docker Container        │       │
│                    │  copilot-core            │       │
│                    │  --port 3000 --headless  │       │
│                    │  --bind 0.0.0.0          │       │
│                    └────────────┬─────────────┘       │
│                                │                     │
│                   HTTP to host.docker.internal:4000   │
│                                │                     │
│                    ┌───────────▼──────────────┐       │
│                    │  copilot-core relay      │       │
│                    │  --port 4000             │       │
│                    │  (authenticates with     │       │
│                    │   Copilot API)           │       │
│                    └──────────┬───────────────┘       │
│                               │                      │
│                    HTTPS + Bearer token               │
│                               │                      │
│                    ┌──────────▼───────────────┐       │
│                    │  api.githubcopilot.com   │       │
│                    └─────────────────────────-┘       │
│                                                      │
└──────────────────────────────────────────────────────┘
```

## Why This Pattern?

Same benefits as container-proxy, but with a first-party relay:

- **No secrets in the image** — safe to share, scan, deploy anywhere
- **No secrets at runtime** — the container never sees API keys
- **No external proxy needed** — `copilot-core relay` is built-in
- **Swap providers freely** — change `COPILOT_API_URL` on the relay without rebuilding
- **Centralized key management** — the relay manages authentication for all containers

## Prerequisites

- **Docker** with Docker Compose
- A pre-built `copilot-core` binary (or build from `runtime/`)
- **GitHub CLI** (`gh`) authenticated, or a valid GitHub token with Copilot access

## Setup

### 1. Start the relay on the host

```bash
GITHUB_TOKEN=$(gh auth token) copilot-core relay --port 4000
```

This starts the built-in HTTP relay on port 4000. It uses `gh auth token` to get your GitHub CLI token and forwards authenticated OpenAI-compatible requests to the Copilot API.

### 2. Start copilot-core in Docker

```bash
docker compose up -d --build
```

This builds copilot-core from source and starts it on port 3000. LLM requests go to `host.docker.internal:4000` — no API keys are passed into the container.

### 3. Run a client sample

**TypeScript**
```bash
cd typescript && npm install && npm run build && npm start
```

**Python**
```bash
cd python && pip install -r requirements.txt && python main.py
```

**Go**
```bash
cd go && go run main.go
```

All samples connect to `localhost:3000` by default. Override with `COPILOT_CLI_URL`.

## Verification

Run all samples end-to-end:

```bash
chmod +x verify.sh
./verify.sh
```

## Languages

| Directory | SDK / Approach | Language |
|-----------|---------------|----------|
| `typescript/` | `@github/copilot-sdk` | TypeScript (Node.js) |
| `python/` | `github-copilot-sdk` | Python |
| `go/` | `github.com/github/copilot-sdk/go` | Go |

## How It Works

1. **copilot-core relay** starts on the host with `GITHUB_TOKEN` — it authenticates with the Copilot API
2. **copilot-core** (server mode) starts in Docker with `COPILOT_API_URL=http://host.docker.internal:4000/v1` — pointing LLM calls at the relay
3. When the agent needs to call an LLM, the request flows: container → relay → Copilot API
4. The relay injects authentication headers and forwards responses (including SSE streams)
5. The container never sees or needs any API credentials

## Comparison with container-proxy

| Aspect | container-proxy | container-relay |
|--------|----------------|-----------------|
| Proxy | `proxy.py` (external) | `copilot-core relay` (built-in) |
| Auth | Manual (inject in proxy) | Automatic (GitHub token) |
| Streaming | Custom SSE handling | Native pass-through |
| Dependencies | Python 3 | None (same binary) |
