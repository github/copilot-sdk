# Auth Sample: BYOK Azure OpenAI

This sample shows how to use Copilot SDK in **BYOK** mode with an Azure OpenAI provider.

## What this sample does

1. Creates a session with a custom provider (`type: "azure"`)
2. Uses your Azure OpenAI endpoint and API key instead of GitHub auth
3. Configures the Azure-specific `apiVersion` field
4. Sends a prompt and prints the response

## Prerequisites

- `copilot` binary (`COPILOT_CLI_PATH`, or auto-detected by SDK)
- Node.js 20+
- An Azure OpenAI resource with a deployed model

## Run

```bash
cd typescript
npm install --ignore-scripts
npm run build
AZURE_OPENAI_ENDPOINT=https://your-resource.openai.azure.com AZURE_OPENAI_API_KEY=... node dist/index.js
```

### Environment variables

| Variable | Required | Default | Description |
|---|---|---|---|
| `AZURE_OPENAI_ENDPOINT` | Yes | — | Azure OpenAI resource endpoint URL |
| `AZURE_OPENAI_API_KEY` | Yes | — | Azure OpenAI API key |
| `AZURE_OPENAI_MODEL` | No | `gpt-4.1` | Deployment / model name |
| `AZURE_API_VERSION` | No | `2024-10-21` | Azure OpenAI API version |
| `COPILOT_CLI_PATH` | No | auto-detected | Path to `copilot` binary |

## Provider configuration

The key difference from standard OpenAI BYOK is the `azure` block in the provider config:

```typescript
provider: {
  type: "azure",
  baseUrl: endpoint,
  apiKey,
  azure: {
    apiVersion: "2024-10-21",
  },
}
```

## Verify

```bash
./verify.sh
```

Build checks run by default. E2E run requires `AZURE_OPENAI_ENDPOINT` and `AZURE_OPENAI_API_KEY` to be set.
