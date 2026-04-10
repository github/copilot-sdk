# Local CLI Setup

Use a specific CLI binary instead of the SDK's bundled CLI. This is an advanced option — you supply the CLI path explicitly, and you are responsible for ensuring version compatibility with the SDK.

**Use when:** You need to pin a specific CLI version, or work with the Go SDK (which does not bundle a CLI).

## How It Works

By default, the Node.js, Python, and .NET SDKs include their own CLI dependency (see [Default Setup](./bundled-cli.md)). If you need to override this — for example, to use a system-installed CLI — you can use the `cliPath` option.

```mermaid
flowchart LR
    subgraph YourMachine["Your Machine"]
        App["Your App"] --> SDK["SDK Client"]
        SDK -- "cliPath" --> CLI["Copilot CLI<br/>(your own binary)"]
        CLI --> Keychain["🔐 System Keychain<br/>(stored credentials)"]
    end
    CLI -- "API calls" --> Copilot["☁️ GitHub Copilot"]

    style YourMachine fill:#0d1117,stroke:#58a6ff,color:#c9d1d9
```

**Key characteristics:**
- You explicitly provide the CLI binary path
- You are responsible for CLI version compatibility with the SDK
- Authentication uses the signed-in user's credentials from the system keychain (or env vars)
- Communication happens over stdio

## Configuration

### Using a local CLI binary

<details open>
<summary><strong>Node.js / TypeScript</strong></summary>

```typescript
import { CopilotClient } from "@github/copilot-sdk";

const client = new CopilotClient({
    cliPath: "/usr/local/bin/copilot",
});

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

client = CopilotClient({
    "cli_path": "/usr/local/bin/copilot",
})
await client.start()

session = await client.create_session(on_permission_request=PermissionHandler.approve_all, model="gpt-4.1")
response = await session.send_and_wait("Hello!")
print(response.data.content)

await client.stop()
```

</details>

<details>
<summary><strong>Go</strong></summary>

> **Note:** The Go SDK does not bundle a CLI, so you must always provide `CLIPath`.

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

	client := copilot.NewClient(&copilot.ClientOptions{
		CLIPath: "/usr/local/bin/copilot",
	})
	if err := client.Start(ctx); err != nil {
		log.Fatal(err)
	}
	defer client.Stop()

	session, _ := client.CreateSession(ctx, &copilot.SessionConfig{Model: "gpt-4.1"})
	response, _ := session.SendAndWait(ctx, copilot.MessageOptions{Prompt: "Hello!"})
	fmt.Println(*response.Data.Content)
}
```
<!-- /docs-validate: hidden -->

```go
client := copilot.NewClient(&copilot.ClientOptions{
    CLIPath: "/usr/local/bin/copilot",
})
if err := client.Start(ctx); err != nil {
    log.Fatal(err)
}
defer client.Stop()

session, _ := client.CreateSession(ctx, &copilot.SessionConfig{Model: "gpt-4.1"})
response, _ := session.SendAndWait(ctx, copilot.MessageOptions{Prompt: "Hello!"})
fmt.Println(*response.Data.Content)
```

</details>

<details>
<summary><strong>.NET</strong></summary>

```csharp
var client = new CopilotClient(new CopilotClientOptions
{
    CliPath = "/usr/local/bin/copilot",
});

await using var session = await client.CreateSessionAsync(
    new SessionConfig { Model = "gpt-4.1" });

var response = await session.SendAndWaitAsync(
    new MessageOptions { Prompt = "Hello!" });
Console.WriteLine(response?.Data.Content);
```

</details>

## Additional Options

```typescript
const client = new CopilotClient({
    cliPath: "/usr/local/bin/copilot",

    // Set log level for debugging
    logLevel: "debug",

    // Pass extra CLI arguments
    cliArgs: ["--log-dir=/tmp/copilot-logs"],

    // Set working directory
    cwd: "/path/to/project",
});
```

## Using Environment Variables

Instead of the keychain, you can authenticate via environment variables. This is useful for CI or when you don't want interactive login.

```bash
# Set one of these (in priority order):
export COPILOT_GITHUB_TOKEN="gho_xxxx"   # Recommended
export GH_TOKEN="gho_xxxx"               # GitHub CLI compatible
export GITHUB_TOKEN="gho_xxxx"           # GitHub Actions compatible
```

The SDK picks these up automatically — no code changes needed.

## Managing Sessions

Sessions default to ephemeral. To create resumable sessions, provide your own session ID:

```typescript
// Create a named session
const session = await client.createSession({
    sessionId: "my-project-analysis",
    model: "gpt-4.1",
});

// Later, resume it
const resumed = await client.resumeSession("my-project-analysis");
```

Session state is stored locally at `~/.copilot/session-state/{sessionId}/`.

## Limitations

| Limitation | Details |
|------------|---------|
| **Version compatibility** | You must ensure your CLI version is compatible with the SDK |
| **Single user** | Credentials are tied to whoever signed in to the CLI |
| **Local only** | The CLI runs on the same machine as your app |
| **No multi-tenant** | Can't serve multiple users from one CLI instance |

## Next Steps

- **[Default Setup](./bundled-cli.md)** — Use the SDK's built-in CLI (recommended for most use cases)
- **[Getting Started tutorial](../getting-started.md)** — Build a complete interactive app
- **[Authentication docs](../auth/index.md)** — All auth methods in detail
