# Default Setup (Bundled CLI)

The Node.js, Python, and .NET SDKs include the Copilot CLI as a dependency — your app ships with everything it needs, with no extra installation or configuration required.

**Best for:** Most applications — desktop apps, standalone tools, CLI utilities, prototypes, and more.

## How It Works

When you install the SDK, the Copilot CLI binary is included automatically. The SDK starts it as a child process and communicates over stdio. There's nothing extra to configure.

```mermaid
flowchart TB
    subgraph Bundle["Your Application"]
        App["Application Code"]
        SDK["SDK Client"]
        CLIBin["Copilot CLI Binary<br/>(included with SDK)"]
    end

    App --> SDK
    SDK --> CLIBin
    CLIBin -- "API calls" --> Copilot["☁️ GitHub Copilot"]

    style Bundle fill:#0d1117,stroke:#58a6ff,color:#c9d1d9
```

**Key characteristics:**
- CLI binary is included with the SDK — no separate install needed
- The SDK manages the CLI version to ensure compatibility
- Users authenticate through your app (or use env vars / BYOK)
- Sessions are managed per-user on their machine

## Quick Start

<details open>
<summary><strong>Node.js / TypeScript</strong></summary>

```typescript
import { CopilotClient } from "@github/copilot-sdk";

const client = new CopilotClient();

const session = await client.createSession({ model: "gpt-4.1" });
const response = await session.sendAndWait({ prompt: "Hello!" });
console.log(response?.data.content);

await client.stop();
```

</details>

<details>
<summary><strong>Python</strong></summary>

```python
from copilot import CopilotClient
from copilot.session import PermissionHandler

client = CopilotClient()
await client.start()

session = await client.create_session(on_permission_request=PermissionHandler.approve_all, model="gpt-4.1")
response = await session.send_and_wait("Hello!")
print(response.data.content)

await client.stop()
```

</details>

<details>
<summary><strong>Go</strong></summary>

> **Note:** The Go SDK does not bundle the CLI. You must install the CLI separately or set `CLIPath` to point to an existing binary. See [Local CLI Setup](./local-cli.md) for details.

<!-- docs-validate: hidden -->
```go
package main

import (
	"context"
	"fmt"
	"log"
	copilot "github.com/github/copilot-sdk/go"
)

func main() {
	ctx := context.Background()

	client := copilot.NewClient(nil)
	if err := client.Start(ctx); err != nil {
		log.Fatal(err)
	}
	defer client.Stop()

	session, _ := client.CreateSession(ctx, &copilot.SessionConfig{Model: "gpt-4.1"})
	response, _ := session.SendAndWait(ctx, copilot.MessageOptions{Prompt: "Hello!"})
	if d, ok := response.Data.(*copilot.AssistantMessageData); ok {
		fmt.Println(d.Content)
	}
}
```
<!-- /docs-validate: hidden -->

```go
client := copilot.NewClient(nil)
if err := client.Start(ctx); err != nil {
    log.Fatal(err)
}
defer client.Stop()

session, _ := client.CreateSession(ctx, &copilot.SessionConfig{Model: "gpt-4.1"})
response, _ := session.SendAndWait(ctx, copilot.MessageOptions{Prompt: "Hello!"})
if d, ok := response.Data.(*copilot.AssistantMessageData); ok {
    fmt.Println(d.Content)
}
```

</details>

<details>
<summary><strong>.NET</strong></summary>

```csharp
await using var client = new CopilotClient();
await using var session = await client.CreateSessionAsync(
    new SessionConfig { Model = "gpt-4.1" });

var response = await session.SendAndWaitAsync(
    new MessageOptions { Prompt = "Hello!" });
Console.WriteLine(response?.Data.Content);
```

</details>

<details>
<summary><strong>Java</strong></summary>

> **Note:** The Java SDK does not bundle or embed the Copilot CLI. You must install the CLI separately and configure its path via `cliPath` or the `COPILOT_CLI_PATH` environment variable.

```java
import com.github.copilot.sdk.CopilotClient;
import com.github.copilot.sdk.events.*;
import com.github.copilot.sdk.json.*;

var client = new CopilotClient(new CopilotClientOptions()
    // Point to the CLI binary installed on the system
    .setCliPath("/path/to/vendor/copilot")
);
client.start().get();

var session = client.createSession(new SessionConfig()
    .setModel("gpt-4.1")
    .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
).get();

var response = session.sendAndWait(new MessageOptions()
    .setPrompt("Hello!")).get();
System.out.println(response.getData().content());

client.stop().get();
```

</details>

## Authentication Strategies

You need to decide how your users will authenticate. Here are the common patterns:

```mermaid
flowchart TB
    App["Bundled App"]

    App --> A["User signs in to CLI<br/>(keychain credentials)"]
    App --> B["App provides token<br/>(OAuth / env var)"]
    App --> C["BYOK<br/>(your own API keys)"]

    A --> Note1["User runs 'copilot' once<br/>to authenticate"]
    B --> Note2["Your app handles login<br/>and passes token"]
    C --> Note3["No GitHub auth needed<br/>Uses your model provider"]

    style App fill:#0d1117,stroke:#58a6ff,color:#c9d1d9
```

### Option A: User's Signed-In Credentials (Simplest)

The user signs in to the CLI once, and your app uses those credentials. No extra code needed — this is the default behavior.

```typescript
const client = new CopilotClient();
// Default: uses signed-in user credentials
```

### Option B: Token via Environment Variable

Ship your app with instructions to set a token, or set it programmatically:

```typescript
const client = new CopilotClient({
    env: {
        COPILOT_GITHUB_TOKEN: getUserToken(),  // Your app provides the token
    },
});
```

### Option C: BYOK (No GitHub Auth Needed)

If you manage your own model provider keys, users don't need GitHub accounts at all:

```typescript
const client = new CopilotClient();

const session = await client.createSession({
    model: "gpt-4.1",
    provider: {
        type: "openai",
        baseUrl: "https://api.openai.com/v1",
        apiKey: process.env.OPENAI_API_KEY,
    },
});
```

See the **[BYOK guide](../auth/byok.md)** for full details.

## Session Management

Apps typically want named sessions so users can resume conversations:

```typescript
const client = new CopilotClient();

// Create a session tied to the user's project
const sessionId = `project-${projectName}`;
const session = await client.createSession({
    sessionId,
    model: "gpt-4.1",
});

// User closes app...
// Later, resume where they left off
const resumed = await client.resumeSession(sessionId);
```

Session state persists at `~/.copilot/session-state/{sessionId}/`.

## When to Move On

| Need | Next Guide |
|------|-----------|
| Users signing in with GitHub accounts | [GitHub OAuth](./github-oauth.md) |
| Run on a server instead of user machines | [Backend Services](./backend-services.md) |
| Use your own model keys | [BYOK](../auth/byok.md) |

## Next Steps

- **[BYOK guide](../auth/byok.md)** — Use your own model provider keys
- **[Session Persistence](../features/session-persistence.md)** — Advanced session management
- **[Getting Started tutorial](../getting-started.md)** — Build a complete app
