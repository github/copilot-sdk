# Auth Sample: GitHub OAuth App (Scenario 1)

This scenario demonstrates how a packaged app can let end users sign in with GitHub using OAuth Device Flow, then use that user token to call Copilot with their own subscription.

## What this sample does

1. Starts GitHub OAuth Device Flow
2. Prompts the user to open the verification URL and enter the code
3. Polls for the access token
4. Fetches the signed-in user profile
5. Calls Copilot with that OAuth token (SDK clients in TypeScript/Python/Go)

## Prerequisites

- A GitHub OAuth App client ID (`GITHUB_OAUTH_CLIENT_ID`)
- `copilot` binary (`COPILOT_CLI_PATH`, or auto-detected by SDK)
- Node.js 20+
- Python 3.10+
- Go 1.24+

## Run

### TypeScript

```bash
cd typescript
npm install --ignore-scripts
npm run build
GITHUB_OAUTH_CLIENT_ID=Ivxxxxxxxxxxxx node dist/index.js
```

### Python

```bash
cd python
pip3 install -r requirements.txt --quiet
GITHUB_OAUTH_CLIENT_ID=Ivxxxxxxxxxxxx python3 main.py
```

### Go

```bash
cd go
go run main.go
```

## Verify

```bash
./verify.sh
```

`verify.sh` checks install/build for all languages. Interactive runs are skipped by default and can be enabled by setting both `GITHUB_OAUTH_CLIENT_ID` and `AUTH_SAMPLE_RUN_INTERACTIVE=1`.

To include this sample in the full suite, run `./verify.sh` from the `samples/` root.
