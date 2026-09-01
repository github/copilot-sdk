# Client info

Client info identifies the editor or extension that hosts your Copilot SDK application. Set the optional `clientInfo` client option to attribute runtime telemetry for that connection to your host instead of the runtime's own build.

## When to set client info

Set client info when you embed the SDK in an editor, an extension, or another application with its own identity.

Leave client info unset for scripts, one-off tools, and back-end jobs that do not represent a distinct host. The runtime then keeps its default attribution.

Client info has four optional string fields. Set the fields you know and omit the rest. The SDK includes client info in the `server.connect` handshake only when at least one field has a non-empty value.

| Field | Example | Meaning |
|---|---|---|
| `editorName` | `"vscode"` | Name of the host editor |
| `editorVersion` | `"1.124.2"` | Version of the host editor |
| `extensionName` | `"copilot-chat"` | Name of the Copilot extension within the host |
| `extensionVersion` | `"0.54.0"` | Version of the Copilot extension within the host |

The SDK sends client info once when it establishes the connection. The identity applies for the lifetime of that connection.

## Configure client info

Pass client info when you create the client:

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
        ClientOptions::new().with_client_info(
            ClientInfo::new()
                .with_editor_name("JetBrains-IU")
                .with_editor_version("2026.1")
                .with_extension_name("copilot-intellij")
                .with_extension_version("1.5.0"),
        ),
    )
    .await?;
    Ok(())
}
```
<!-- /docs-validate: hidden -->

```rust
use github_copilot_sdk::{Client, ClientInfo, ClientOptions};

let client = Client::start(
    ClientOptions::new().with_client_info(
        ClientInfo::new()
            .with_editor_name("JetBrains-IU")
            .with_editor_version("2026.1")
            .with_extension_name("copilot-intellij")
            .with_extension_version("1.5.0"),
    ),
)
.await?;
```

</details>

## Notes

* Client info is advisory. The runtime can ignore values that do not match the expected format, such as an invalid version string.
* Setting client info changes how the runtime attributes its telemetry. It does not change what the runtime records.
* If every field is unset or empty, the SDK omits client info from the handshake and the runtime keeps its default attribution.
