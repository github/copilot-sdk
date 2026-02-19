# Auth Sample: BYOK Anthropic

This sample shows how to use Copilot SDK in **BYOK** mode with an Anthropic provider.

## What this sample does

1. Creates a session with a custom provider (`type: "anthropic"`)
2. Uses your `ANTHROPIC_API_KEY` instead of GitHub auth
3. Sends a prompt and prints the response

## Prerequisites

- `copilot` binary (`COPILOT_CLI_PATH`, or auto-detected by SDK)
- Node.js 20+
- `ANTHROPIC_API_KEY`

## Run

```bash
cd typescript
npm install --ignore-scripts
npm run build
ANTHROPIC_API_KEY=sk-ant-... node dist/index.js
```

Optional environment variables:

- `ANTHROPIC_BASE_URL` (default: `https://api.anthropic.com`)
- `ANTHROPIC_MODEL` (default: `claude-sonnet-4-20250514`)

## Verify

```bash
./verify.sh
```

Build checks run by default. E2E run is optional and requires both `BYOK_SAMPLE_RUN_E2E=1` and `ANTHROPIC_API_KEY`.
