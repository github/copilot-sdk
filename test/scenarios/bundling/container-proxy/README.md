# Container-Proxy Samples

Run the Copilot CLI inside a Docker container with a simple proxy on the host that returns canned responses. This demonstrates the deployment pattern where an external service intercepts the agent's LLM calls — in production the proxy would add credentials and forward to a real provider; here it just returns a fixed reply as proof-of-concept.

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
│                    │  Copilot CLI             │       │
│                    │  --port 3000 --headless  │       │
│                    │  --bind 0.0.0.0          │       │
│                    │  --auth-token-env        │       │
│                    └────────────┬─────────────┘       │
│                                │                     │
│                   HTTP to host.docker.internal:4000   │
│                                │                     │
│                    ┌───────────▼──────────────┐       │
│                    │  proxy.py                │       │
│                    │  (port 4000)             │       │
│                    │  Returns canned response │       │
│                    └─────────────────────────-┘       │
│                                                      │
└──────────────────────────────────────────────────────┘
```

## Why This Pattern?

The agent runtime (Copilot CLI) has **no access to API keys**. All LLM traffic flows through a proxy on the host. In production you would replace `proxy.py` with a real proxy that injects credentials and forwards to OpenAI/Anthropic/etc. This means:

- **No secrets in the image** — safe to share, scan, deploy anywhere
- **No secrets at runtime** — even if the container is compromised, there are no tokens to steal
- **Swap providers freely** — change the proxy target without rebuilding the container
- **Centralized key management** — one proxy manages keys for all your agents/services

## Prerequisites

- **Docker** with Docker Compose
- **Python 3** (for the proxy — uses only stdlib, no pip install needed)

## Setup

### 1. Start the proxy

```bash
python3 proxy.py 4000
```

This starts a minimal OpenAI-compatible HTTP server on port 4000 that returns a canned "The capital of France is Paris." response for every request.

### 2. Start the Copilot CLI in Docker

```bash
docker compose up -d --build
```

This builds the Copilot CLI from source and starts it on port 3000. It sends LLM requests to `host.docker.internal:4000` — no API keys are passed into the container.

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

1. **Copilot CLI** starts in Docker with `COPILOT_API_URL=http://host.docker.internal:4000` — this overrides the default Copilot API endpoint to point at the proxy
2. When the agent needs to call an LLM, it sends a standard OpenAI-format request to the proxy
3. **proxy.py** receives the request and returns a canned response (in production, this would inject credentials and forward to a real provider)
4. The response flows back: proxy → Copilot CLI → your app

The container never sees or needs any API credentials.
