# Auth Sample: BYOK Ollama (Compact Context)

This sample shows BYOK with **local Ollama** and intentionally trims session context so it works better with smaller local models.

## What this sample does

1. Uses a custom provider pointed at Ollama (`http://localhost:11434/v1`)
2. Replaces the default system prompt with a short compact prompt
3. Sets `availableTools: []` to remove built-in tool definitions from model context
4. Sends a prompt and prints the response

This creates a small assistant profile suitable for constrained context windows.

## Prerequisites

- `copilot` binary (`COPILOT_CLI_PATH`, or auto-detected by SDK)
- Node.js 20+
- Ollama running locally (`ollama serve`)
- A local model pulled (for example: `ollama pull llama3.2:3b`)

## Run

```bash
cd typescript
npm install --ignore-scripts
npm run build
node dist/index.js
```

Optional environment variables:

- `OLLAMA_BASE_URL` (default: `http://localhost:11434/v1`)
- `OLLAMA_MODEL` (default: `llama3.2:3b`)

## Verify

```bash
./verify.sh
```

Build checks run by default. E2E run is optional and requires `BYOK_SAMPLE_RUN_E2E=1`.
