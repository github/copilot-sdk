# Auth Sample: BYOK OpenAI

This sample shows how to use Copilot SDK in **BYOK** mode with an OpenAI-compatible provider.

## What this sample does

1. Creates a session with a custom provider (`type: "openai"`)
2. Uses your `OPENAI_API_KEY` instead of GitHub auth
3. Sends a prompt and prints the response

## Prerequisites

- `copilot` binary (`COPILOT_CLI_PATH`, or auto-detected by SDK)
- Node.js 20+
- `OPENAI_API_KEY`

## Run

```bash
cd typescript
npm install --ignore-scripts
npm run build
OPENAI_API_KEY=sk-... node dist/index.js
```

Optional environment variables:

- `OPENAI_BASE_URL` (default: `https://api.openai.com/v1`)
- `OPENAI_MODEL` (default: `gpt-4.1-mini`)

## Verify

```bash
./verify.sh
```

Build checks run by default. E2E run is optional and requires both `BYOK_SAMPLE_RUN_E2E=1` and `OPENAI_API_KEY`.
