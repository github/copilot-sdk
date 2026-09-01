# Client info

When your application embeds the SDK, the runtime emits its own telemetry for the work it does on your behalf. By default that telemetry is attributed to the runtime's own build, not to your host. If you ship the SDK inside an editor or a larger tool, declaring a **client info** identity lets the runtime attribute the telemetry it emits on a connection to a consistent surface: your host editor and its Copilot extension.

This guide explains what client info is, when to set it, and how to declare it in each language.

## When to set client info

Set client info when you embed the SDK in a host that already has its own identity, for example an editor, an IDE plugin, or a desktop application. Declaring it keeps the runtime's telemetry attributed to your host instead of the SDK build it happens to bundle.

You can leave it unset. When you do, the runtime keeps its default attribution, which is the right choice for scripts, one-off tools, and back-end jobs that don't represent a distinct host surface.

All four fields are optional and independent. Send the ones you know and omit the rest; an empty identity is dropped from the handshake entirely.

| Field | Example | Meaning |
|---|---|---|
| `editorName` | `"vscode"` | Name of the host editor. |
| `editorVersion` | `"1.124.2"` | Version of the host editor. |
| `extensionName` | `"copilot-chat"` | Name of the Copilot extension within the host. |
| `extensionVersion` | `"0.54.0"` | Version of the Copilot extension within the host. |

Client info is declared once, on the `server.connect` handshake, so it applies for the lifetime of the connection.

## Declaring client info

Pass client info in the client options. The SDK forwards it to the runtime when the connection is established.

<details open>
<summary><strong>Node.js / TypeScript</strong></summary>

<!-- docs-validate: hidden -->
```typescript
import { CopilotClient } from "@github/copilot-sdk";

async function main() {
  const client = new CopilotClient({
    clientInfo: {
      editorName: "JetBrains-IU",
      editorVersion: "2026.1",
      extensionName: "copilot-intellij",
      extensionVersion: "1.5.0",
    },
  });

  await client.start();
}

main();
```
<!-- /docs-validate: hidden -->

```typescript
import { CopilotClient } from "@github/copilot-sdk";

const client = new CopilotClient({
  clientInfo: {
    editorName: "JetBrains-IU",
    editorVersion: "2026.1",
    extensionName: "copilot-intellij",
    extensionVersion: "1.5.0",
  },
});

await client.start();
```

</details>

<details>
<summary><strong>Python</strong></summary>

<!-- docs-validate: wrap-async -->
```python
from copilot import CopilotClient

client = CopilotClient(
    client_info={
        "editor_name": "JetBrains-IU",
        "editor_version": "2026.1",
        "extension_name": "copilot-intellij",
        "extension_version": "1.5.0",
    },
)
await client.start()
```

</details>

<details>
<summary><strong>Go</strong></summary>

<!-- docs-validate: hidden -->
```go
package main

import (
	"context"

	copilot "github.com/github/copilot-sdk/go"
)

func main() {
	ctx := context.Background()
	client := copilot.NewClient(&copilot.ClientOptions{
		ClientInfo: &copilot.ClientInfo{
			EditorName:       "JetBrains-IU",
			EditorVersion:    "2026.1",
			ExtensionName:    "copilot-intellij",
			ExtensionVersion: "1.5.0",
		},
	})
	if err := client.Start(ctx); err != nil {
		return
	}
}
```
<!-- /docs-validate: hidden -->

```go
client := copilot.NewClient(&copilot.ClientOptions{
    ClientInfo: &copilot.ClientInfo{
        EditorName:       "JetBrains-IU",
        EditorVersion:    "2026.1",
        ExtensionName:    "copilot-intellij",
        ExtensionVersion: "1.5.0",
    },
})
if err := client.Start(ctx); err != nil {
    return err
}
```

</details>

<details>
<summary><strong>.NET</strong></summary>

```csharp
using GitHub.Copilot;

await using var client = new CopilotClient(new CopilotClientOptions
{
    ClientInfo = new CopilotClientInfo
    {
        EditorName = "JetBrains-IU",
        EditorVersion = "2026.1",
        ExtensionName = "copilot-intellij",
        ExtensionVersion = "1.5.0",
    },
});

await client.StartAsync();
```

</details>

<details>
<summary><strong>Java</strong></summary>

<!-- docs-validate: hidden -->
```java
import com.github.copilot.CopilotClient;
import com.github.copilot.rpc.ClientInfo;
import com.github.copilot.rpc.CopilotClientOptions;

public class ClientInfoExample {
    public static void main(String[] args) throws Exception {
        var options = new CopilotClientOptions()
            .setClientInfo(new ClientInfo()
                .setEditorName("JetBrains-IU")
                .setEditorVersion("2026.1")
                .setExtensionName("copilot-intellij")
                .setExtensionVersion("1.5.0"));

        var client = new CopilotClient(options);
        client.start().get();
    }
}
```
<!-- /docs-validate: hidden -->

```java
var options = new CopilotClientOptions()
    .setClientInfo(new ClientInfo()
        .setEditorName("JetBrains-IU")
        .setEditorVersion("2026.1")
        .setExtensionName("copilot-intellij")
        .setExtensionVersion("1.5.0"));

var client = new CopilotClient(options);
client.start().get();
```

</details>

<details>
<summary><strong>Rust</strong></summary>

<!-- docs-validate: hidden -->
```rust
use github_copilot_sdk::{Client, ClientInfo, ClientOptions};

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let _client = Client::start(
        ClientOptions::new().with_client_info(ClientInfo {
            editor_name: Some("JetBrains-IU".to_string()),
            editor_version: Some("2026.1".to_string()),
            extension_name: Some("copilot-intellij".to_string()),
            extension_version: Some("1.5.0".to_string()),
        }),
    )
    .await?;
    Ok(())
}
```
<!-- /docs-validate: hidden -->

```rust
use github_copilot_sdk::{Client, ClientInfo, ClientOptions};

let client = Client::start(
    ClientOptions::new().with_client_info(ClientInfo {
        editor_name: Some("JetBrains-IU".to_string()),
        editor_version: Some("2026.1".to_string()),
        extension_name: Some("copilot-intellij".to_string()),
        extension_version: Some("1.5.0".to_string()),
    }),
)
.await?;
```

</details>

## Notes

* Client info is advisory. The runtime may ignore fields that don't look like the value it expects (for example, a version string that isn't version-shaped).
* Setting client info does not change what the runtime records, only how the telemetry it already emits is attributed.
* Every field is optional. A client info with no fields set is treated the same as not setting it at all.
