# BYOK (Bring Your Own Key)

BYOK allows you to use the Copilot SDK with your own API keys from model providers, bypassing GitHub Copilot authentication. This is useful for enterprise deployments, custom model hosting, or when you want direct billing with your model provider.

## Supported Providers

| Provider | Type Value | Notes |
|----------|------------|-------|
| OpenAI | `"openai"` | OpenAI API and OpenAI-compatible endpoints |
| Azure OpenAI / Azure AI Foundry | `"azure"` | Azure-hosted models |
| Anthropic | `"anthropic"` | Claude models |
| Ollama | `"openai"` | Local models via OpenAI-compatible API |
| Other OpenAI-compatible | `"openai"` | vLLM, LiteLLM, etc. |

## Quick Start: Azure AI Foundry

Azure AI Foundry (formerly Azure OpenAI) is a common BYOK deployment target for enterprises. Here's a complete example:

<details open>
<summary><strong>Python</strong></summary>

```python
import asyncio
import os
from copilot import CopilotClient

FOUNDRY_MODEL_URL = "https://your-resource.openai.azure.com/openai/v1/"
# Set FOUNDRY_API_KEY environment variable

async def main():
    client = CopilotClient()
    await client.start()

    session = await client.create_session({
        "model": "gpt-5.2-codex",  # Your deployment name
        "provider": {
            "type": "openai",
            "base_url": FOUNDRY_MODEL_URL,
            "wire_api": "responses",  # Use "completions" for older models
            "api_key": os.environ["FOUNDRY_API_KEY"],
        },
    })

    done = asyncio.Event()

    def on_event(event):
        if event.type.value == "assistant.message":
            print(event.data.content)
        elif event.type.value == "session.idle":
            done.set()

    session.on(on_event)
    await session.send({"prompt": "What is 2+2?"})
    await done.wait()

    await session.destroy()
    await client.stop()

asyncio.run(main())
```

</details>

<details>
<summary><strong>Node.js / TypeScript</strong></summary>

```typescript
import { CopilotClient } from "@github/copilot-sdk";

const FOUNDRY_MODEL_URL = "https://your-resource.openai.azure.com/openai/v1/";

const client = new CopilotClient();
const session = await client.createSession({
    model: "gpt-5.2-codex",  // Your deployment name
    provider: {
        type: "openai",
        baseUrl: FOUNDRY_MODEL_URL,
        wireApi: "responses",  // Use "completions" for older models
        apiKey: process.env.FOUNDRY_API_KEY,
    },
});

session.on("assistant.message", (event) => {
    console.log(event.data.content);
});

await session.sendAndWait({ prompt: "What is 2+2?" });
await client.stop();
```

</details>

<details>
<summary><strong>Go</strong></summary>

```go
package main

import (
    "context"
    "fmt"
    "os"
    copilot "github.com/github/copilot-sdk/go"
)

func main() {
    ctx := context.Background()
    client := copilot.NewClient(nil)
    if err := client.Start(ctx); err != nil {
        panic(err)
    }
    defer client.Stop()

    session, err := client.CreateSession(ctx, &copilot.SessionConfig{
        Model: "gpt-5.2-codex",  // Your deployment name
        Provider: &copilot.ProviderConfig{
            Type:    "openai",
            BaseURL: "https://your-resource.openai.azure.com/openai/v1/",
            WireApi: "responses",  // Use "completions" for older models
            APIKey:  os.Getenv("FOUNDRY_API_KEY"),
        },
    })
    if err != nil {
        panic(err)
    }

    response, err := session.SendAndWait(ctx, copilot.MessageOptions{
        Prompt: "What is 2+2?",
    })
    if err != nil {
        panic(err)
    }

    fmt.Println(*response.Data.Content)
}
```

</details>

<details>
<summary><strong>.NET</strong></summary>

```csharp
using GitHub.Copilot.SDK;

await using var client = new CopilotClient();
await using var session = await client.CreateSessionAsync(new SessionConfig
{
    Model = "gpt-5.2-codex",  // Your deployment name
    Provider = new ProviderConfig
    {
        Type = "openai",
        BaseUrl = "https://your-resource.openai.azure.com/openai/v1/",
        WireApi = "responses",  // Use "completions" for older models
        ApiKey = Environment.GetEnvironmentVariable("FOUNDRY_API_KEY"),
    },
});

var response = await session.SendAndWaitAsync(new MessageOptions
{
    Prompt = "What is 2+2?",
});
Console.WriteLine(response?.Data.Content);
```

</details>

## Provider Configuration Reference

### ProviderConfig Fields

| Field | Type | Description |
|-------|------|-------------|
| `type` | `"openai"` \| `"azure"` \| `"anthropic"` | Provider type (default: `"openai"`) |
| `baseUrl` / `base_url` | string | **Required.** API endpoint URL |
| `apiKey` / `api_key` | string | API key (optional for local providers like Ollama) |
| `bearerToken` / `bearer_token` | string | Bearer token auth (takes precedence over apiKey) |
| `wireApi` / `wire_api` | `"completions"` \| `"responses"` | API format (default: `"completions"`) |
| `azure.apiVersion` / `azure.api_version` | string | Azure API version (default: `"2024-10-21"`) |

### Wire API Format

The `wireApi` setting determines which OpenAI API format to use:

- **`"completions"`** (default) - Chat Completions API (`/chat/completions`). Use for most models.
- **`"responses"`** - Responses API. Use for GPT-5 series models that support the newer responses format.

### Type-Specific Notes

**OpenAI (`type: "openai"`)**
- Works with OpenAI API and any OpenAI-compatible endpoint
- `baseUrl` should include the full path (e.g., `https://api.openai.com/v1`)

**Azure (`type: "azure"`)**
- Use for native Azure OpenAI endpoints
- `baseUrl` should be just the host (e.g., `https://my-resource.openai.azure.com`)
- Do NOT include `/openai/v1` in the URL—the SDK handles path construction

**Anthropic (`type: "anthropic"`)**
- For direct Anthropic API access
- Uses Claude-specific API format

## Example Configurations

### OpenAI Direct

```typescript
provider: {
    type: "openai",
    baseUrl: "https://api.openai.com/v1",
    apiKey: process.env.OPENAI_API_KEY,
}
```

### Azure OpenAI (Native Azure Endpoint)

Use `type: "azure"` for endpoints at `*.openai.azure.com`:

```typescript
provider: {
    type: "azure",
    baseUrl: "https://my-resource.openai.azure.com",  // Just the host
    apiKey: process.env.AZURE_OPENAI_KEY,
    azure: {
        apiVersion: "2024-10-21",
    },
}
```

### Azure AI Foundry (OpenAI-Compatible Endpoint)

For Azure AI Foundry deployments with `/openai/v1/` endpoints, use `type: "openai"`:

```typescript
provider: {
    type: "openai",
    baseUrl: "https://your-resource.openai.azure.com/openai/v1/",
    apiKey: process.env.FOUNDRY_API_KEY,
    wireApi: "responses",  // For GPT-5 series models
}
```

### Ollama (Local)

```typescript
provider: {
    type: "openai",
    baseUrl: "http://localhost:11434/v1",
    // No apiKey needed for local Ollama
}
```

### Anthropic

```typescript
provider: {
    type: "anthropic",
    baseUrl: "https://api.anthropic.com",
    apiKey: process.env.ANTHROPIC_API_KEY,
}
```

### Bearer Token Authentication

Some providers require bearer token authentication instead of API keys:

```typescript
provider: {
    type: "openai",
    baseUrl: "https://my-custom-endpoint.example.com/v1",
    bearerToken: process.env.MY_BEARER_TOKEN,  // Sets Authorization header
}
```

> **Note:** The `bearerToken` option accepts a **static token string** only. The SDK does not refresh this token automatically. If your token expires, requests will fail and you'll need to create a new session with a fresh token.

## Limitations

When using BYOK, be aware of these limitations:

### Identity Limitations

BYOK authentication uses **static credentials only**. The following identity providers are NOT supported:

- ❌ **Microsoft Entra ID (Azure AD)** - No support for Entra managed identities or service principals
- ❌ **Third-party identity providers** - No OIDC, SAML, or other federated identity
- ❌ **Managed identities** - Azure Managed Identity is not supported

You must use an API key or static bearer token that you manage yourself.

**Why not Entra ID?** While Entra ID does issue bearer tokens, these tokens are short-lived (typically 1 hour) and require automatic refresh via the Azure Identity SDK. The `bearerToken` option only accepts a static string—there is no callback mechanism for the SDK to request fresh tokens. For long-running workloads requiring Entra authentication, you would need to implement your own token refresh logic and create new sessions with updated tokens.

### Feature Limitations

Some Copilot features may behave differently with BYOK:

- **Model availability** - Only models supported by your provider are available
- **Rate limiting** - Subject to your provider's rate limits, not Copilot's
- **Usage tracking** - Usage is tracked by your provider, not GitHub Copilot
- **Premium requests** - Do not count against Copilot premium request quotas

### Provider-Specific Limitations

| Provider | Limitations |
|----------|-------------|
| Azure AI Foundry | No Entra ID auth; must use API keys |
| Ollama | No API key; local only; model support varies |
| OpenAI | Subject to OpenAI rate limits and quotas |

## Troubleshooting

### "Model not specified" Error

When using BYOK, the `model` parameter is **required**:

```typescript
// ❌ Error: Model required with custom provider
const session = await client.createSession({
    provider: { type: "openai", baseUrl: "..." },
});

// ✅ Correct: Model specified
const session = await client.createSession({
    model: "gpt-4",  // Required!
    provider: { type: "openai", baseUrl: "..." },
});
```

### Azure Endpoint Type Confusion

For Azure OpenAI endpoints (`*.openai.azure.com`), use the correct type:

<!-- docs-validate: skip -->
```typescript
// ❌ Wrong: Using "openai" type with native Azure endpoint
provider: {
    type: "openai",  // This won't work correctly
    baseUrl: "https://my-resource.openai.azure.com",
}

// ✅ Correct: Using "azure" type
provider: {
    type: "azure",
    baseUrl: "https://my-resource.openai.azure.com",
}
```

However, if your Azure AI Foundry deployment provides an OpenAI-compatible endpoint path (e.g., `/openai/v1/`), use `type: "openai"`:

<!-- docs-validate: skip -->
```typescript
// ✅ Correct: OpenAI-compatible Azure AI Foundry endpoint
provider: {
    type: "openai",
    baseUrl: "https://your-resource.openai.azure.com/openai/v1/",
}
```

### Connection Refused (Ollama)

Ensure Ollama is running and accessible:

```bash
# Check Ollama is running
curl http://localhost:11434/v1/models

# Start Ollama if not running
ollama serve
```

### Authentication Failed

1. Verify your API key is correct and not expired
2. Check the `baseUrl` matches your provider's expected format
3. For bearer tokens, ensure the full token is provided (not just a prefix)

## Next Steps

- [Authentication Overview](./index.md) - Learn about all authentication methods
- [Getting Started Guide](../getting-started.md) - Build your first Copilot-powered app
