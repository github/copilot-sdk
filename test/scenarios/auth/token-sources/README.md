# Auth Sample: Token Sources

This sample demonstrates how the Copilot SDK resolves authentication tokens from multiple sources, and the priority chain it follows.

## Token Priority Chain

The SDK resolves a GitHub token using the following priority (highest to lowest):

| Priority | Source | Description |
|----------|------|-------------|
| 1 | `githubToken` option | Explicit token passed to `CopilotClient` constructor |
| 2 | `COPILOT_GITHUB_TOKEN` | Environment variable set by Copilot extensions runtime |
| 3 | `GH_TOKEN` | Environment variable used by the GitHub CLI |
| 4 | `GITHUB_TOKEN` | Common environment variable (e.g. GitHub Actions) |
| 5 | `gh` CLI / stored OAuth | Falls back to `gh auth token` or stored OAuth credentials |

## What this sample does

1. Detects which token source is available
2. Passes the resolved token explicitly via `githubToken`
3. Creates a session and sends a prompt to verify auth works
4. Prints which source was used

## Prerequisites

- `copilot` binary (`COPILOT_CLI_PATH`, or auto-detected by SDK)
- Node.js 20+
- At least one token source configured (environment variable or `gh` CLI)

## Run

```bash
cd typescript
npm install --ignore-scripts
npm run build

# Using GH_TOKEN
GH_TOKEN=ghp_... node dist/index.js

# Using GITHUB_TOKEN
GITHUB_TOKEN=ghp_... node dist/index.js

# Using gh CLI (no env vars needed)
node dist/index.js
```

## Verify

```bash
./verify.sh
```

Build checks run by default. E2E run is optional and requires `BYOK_SAMPLE_RUN_E2E=1`.
