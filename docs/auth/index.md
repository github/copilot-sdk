# Authentication

The GitHub Copilot SDK supports multiple authentication methods to fit different use cases. Choose the method that best matches your deployment scenario.

## Authentication Methods

| Method | Use Case | Copilot Subscription Required |
|--------|----------|-------------------------------|
| [GitHub Signed-in User](#github-signed-in-user) | Interactive apps where users sign in with GitHub | Yes |
| [OAuth GitHub App](#oauth-github-app) | Apps acting on behalf of users via OAuth | Yes |
| [Environment Variables](#environment-variables) | CI/CD, automation, server-to-server | Yes |
| [BYOK (Bring Your Own Key)](./byok.md) | Using your own API keys (Azure AI Foundry, OpenAI, etc.) | No |

## GitHub Signed-in User

This is the default authentication method when running the Copilot CLI interactively. Users authenticate via GitHub OAuth device flow, and the SDK uses their stored credentials.

**How it works:**
1. User runs `copilot` CLI and signs in via GitHub OAuth
2. Credentials are stored securely in the system keychain
3. SDK automatically uses stored credentials

**SDK Configuration:**

<details open>
<summary><strong>Node.js / TypeScript</strong></summary>

```typescript
import { CopilotClient } from "@github/copilot-sdk";

// Default: uses logged-in user credentials
const client = new CopilotClient();
```

</details>

<details>
<summary><strong>Python</strong></summary>

```python
from copilot import CopilotClient

# Default: uses logged-in user credentials
client = CopilotClient()
await client.start()
```

</details>

<details>
<summary><strong>Go</strong></summary>

```go
import copilot "github.com/github/copilot-sdk/go"

// Default: uses logged-in user credentials
client := copilot.NewClient(nil)
```

</details>

<details>
<summary><strong>.NET</strong></summary>

```csharp
using GitHub.Copilot.SDK;

// Default: uses logged-in user credentials
await using var client = new CopilotClient();
```

</details>

**When to use:**
- Desktop applications where users interact directly
- Development and testing environments
- Any scenario where a user can sign in interactively

## OAuth GitHub App

Use an OAuth GitHub App to authenticate users through your application and pass their credentials to the SDK. This enables your application to make Copilot API requests on behalf of users who authorize your app.

**How it works:**
1. User authorizes your OAuth GitHub App
2. Your app receives a user access token (`gho_` or `ghu_` prefix)
3. Pass the token to the SDK via `githubToken` option

**SDK Configuration:**

<details open>
<summary><strong>Node.js / TypeScript</strong></summary>

```typescript
import { CopilotClient } from "@github/copilot-sdk";

const client = new CopilotClient({
    githubToken: userAccessToken,  // Token from OAuth flow
    useLoggedInUser: false,        // Don't use stored CLI credentials
});
```

</details>

<details>
<summary><strong>Python</strong></summary>

```python
from copilot import CopilotClient

client = CopilotClient({
    "github_token": user_access_token,  # Token from OAuth flow
    "use_logged_in_user": False,        # Don't use stored CLI credentials
})
await client.start()
```

</details>

<details>
<summary><strong>Go</strong></summary>

```go
import copilot "github.com/github/copilot-sdk/go"

client := copilot.NewClient(&copilot.ClientOptions{
    GithubToken:     userAccessToken,   // Token from OAuth flow
    UseLoggedInUser: copilot.Bool(false), // Don't use stored CLI credentials
})
```

</details>

<details>
<summary><strong>.NET</strong></summary>

```csharp
using GitHub.Copilot.SDK;

await using var client = new CopilotClient(new CopilotClientOptions
{
    GithubToken = userAccessToken,     // Token from OAuth flow
    UseLoggedInUser = false,           // Don't use stored CLI credentials
});
```

</details>

**Supported token types:**
- `gho_` - OAuth user access tokens
- `ghu_` - GitHub App user access tokens  
- `github_pat_` - Fine-grained personal access tokens

**Not supported:**
- `ghp_` - Classic personal access tokens (deprecated)

**When to use:**
- Web applications where users sign in via GitHub
- SaaS applications building on top of Copilot
- Any multi-user application where you need to make requests on behalf of different users

## Environment Variables

For automation, CI/CD pipelines, and server-to-server scenarios, you can authenticate using environment variables.

**Supported environment variables (in priority order):**
1. `COPILOT_GITHUB_TOKEN` - Recommended for explicit Copilot usage
2. `GH_TOKEN` - GitHub CLI compatible
3. `GITHUB_TOKEN` - GitHub Actions compatible

**How it works:**
1. Set one of the supported environment variables with a valid token
2. The SDK automatically detects and uses the token

**SDK Configuration:**

No code changes needed—the SDK automatically detects environment variables:

<details open>
<summary><strong>Node.js / TypeScript</strong></summary>

```typescript
import { CopilotClient } from "@github/copilot-sdk";

// Token is read from environment variable automatically
const client = new CopilotClient();
```

</details>

<details>
<summary><strong>Python</strong></summary>

```python
from copilot import CopilotClient

# Token is read from environment variable automatically
client = CopilotClient()
await client.start()
```

</details>

**When to use:**
- CI/CD pipelines (GitHub Actions, Jenkins, etc.)
- Automated testing
- Server-side applications with service accounts
- Development when you don't want to use interactive login

## BYOK (Bring Your Own Key)

BYOK allows you to use your own API keys from model providers like Azure AI Foundry, OpenAI, or Anthropic. This bypasses GitHub Copilot authentication entirely.

**Key benefits:**
- No GitHub Copilot subscription required
- Use enterprise model deployments
- Direct billing with your model provider
- Support for Azure AI Foundry, OpenAI, Anthropic, and OpenAI-compatible endpoints

**See the [BYOK documentation](./byok.md) for complete details**, including:
- Azure AI Foundry setup
- Provider configuration options
- Limitations and considerations
- Complete code examples

## Authentication Priority

When multiple authentication methods are available, the SDK uses them in this priority order:

1. **Explicit `githubToken`** - Token passed directly to SDK constructor
2. **HMAC key** - `CAPI_HMAC_KEY` or `COPILOT_HMAC_KEY` environment variables
3. **Direct API token** - `GITHUB_COPILOT_API_TOKEN` with `COPILOT_API_URL`
4. **Environment variable tokens** - `COPILOT_GITHUB_TOKEN` → `GH_TOKEN` → `GITHUB_TOKEN`
5. **Stored OAuth credentials** - From previous `copilot` CLI login
6. **GitHub CLI** - `gh auth` credentials

## Disabling Auto-Login

To prevent the SDK from automatically using stored credentials or `gh` CLI auth, use the `useLoggedInUser: false` option:

<details open>
<summary><strong>Node.js / TypeScript</strong></summary>

```typescript
const client = new CopilotClient({
    useLoggedInUser: false,  // Only use explicit tokens
});
```

</details>

<details>
<summary><strong>Python</strong></summary>

```python
client = CopilotClient({
    "use_logged_in_user": False,  # Only use explicit tokens
})
```

</details>

<details>
<summary><strong>Go</strong></summary>

```go
client := copilot.NewClient(&copilot.ClientOptions{
    UseLoggedInUser: copilot.Bool(false),  // Only use explicit tokens
})
```

</details>

<details>
<summary><strong>.NET</strong></summary>

```csharp
await using var client = new CopilotClient(new CopilotClientOptions
{
    UseLoggedInUser = false,  // Only use explicit tokens
});
```

</details>

## Next Steps

- [BYOK Documentation](./byok.md) - Learn how to use your own API keys
- [Getting Started Guide](../getting-started.md) - Build your first Copilot-powered app
- [MCP Servers](../mcp) - Connect to external tools
