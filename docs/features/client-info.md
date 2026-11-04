# Client info

Client info identifies the application using the Copilot SDK and, when applicable, a specific integration within it. An integration is an identifiable sub-part of the application through which the SDK is used, such as an extension or plugin. Set the optional `clientInfo` client option to attribute runtime telemetry for that connection to your application instead of the runtime's own build.

## When to set client info

Set client info when your SDK application represents a distinct product, service, or integration whose runtime activity should be attributed consistently.

Leave client info unset for scripts, one-off tools, and jobs that do not represent a distinct application. The runtime then keeps its default attribution.

Client info has four optional string fields. Set the fields you know and omit the rest. The SDK includes client info in the `server.connect` handshake only when at least one field has a non-empty value.

| Field | Example | Meaning |
|---|---|---|
| `applicationName` | `"vscode"` | Name of the application using the SDK |
| `applicationVersion` | `"1.124.2"` | Version of the application using the SDK |
| `integrationName` | `"copilot-chat"` | Name of the extension, plugin, or other application sub-part using the SDK |
| `integrationVersion` | `"0.54.0"` | Version of that extension, plugin, or application sub-part |

For a standalone application without a distinct integration, set only the application fields. For example, a developer portal could set `applicationName` to `"acme-developer-portal"` and `applicationVersion` to `"2.4.0"`, leaving both integration fields unset.

The SDK sends client info once when it establishes the connection. The identity applies for the lifetime of that connection.

## Configure client info

Pass client info when you create the client:

<details open>
<summary><strong>TypeScript</strong></summary>

<!-- docs-validate: hidden -->
```typescript
import { CopilotClient } from "@github/copilot-sdk";

async function main() {
  const client = new CopilotClient({
    clientInfo: {
      applicationName: "vscode",
      applicationVersion: "1.124.2",
      integrationName: "copilot-chat",
      integrationVersion: "0.54.0",
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
    applicationName: "vscode",
    applicationVersion: "1.124.2",
    integrationName: "copilot-chat",
    integrationVersion: "0.54.0",
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
        "application_name": "vscode",
        "application_version": "1.124.2",
        "integration_name": "copilot-chat",
        "integration_version": "0.54.0",
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
			ApplicationName:    "vscode",
			ApplicationVersion: "1.124.2",
			IntegrationName:    "copilot-chat",
			IntegrationVersion: "0.54.0",
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
        ApplicationName:    "vscode",
        ApplicationVersion: "1.124.2",
        IntegrationName:    "copilot-chat",
        IntegrationVersion: "0.54.0",
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
        ApplicationName = "vscode",
        ApplicationVersion = "1.124.2",
        IntegrationName = "copilot-chat",
        IntegrationVersion = "0.54.0",
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
                .setApplicationName("vscode")
                .setApplicationVersion("1.124.2")
                .setIntegrationName("copilot-chat")
                .setIntegrationVersion("0.54.0"));

        var client = new CopilotClient(options);
        client.start().get();
    }
}
```
<!-- /docs-validate: hidden -->

```java
var options = new CopilotClientOptions()
    .setClientInfo(new ClientInfo()
        .setApplicationName("vscode")
        .setApplicationVersion("1.124.2")
        .setIntegrationName("copilot-chat")
        .setIntegrationVersion("0.54.0"));

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
                .with_application_name("vscode")
                .with_application_version("1.124.2")
                .with_integration_name("copilot-chat")
                .with_integration_version("0.54.0"),
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
            .with_application_name("vscode")
            .with_application_version("1.124.2")
            .with_integration_name("copilot-chat")
            .with_integration_version("0.54.0"),
    ),
)
.await?;
```

</details>

## Notes

* Client info is advisory. The runtime can ignore values that do not match the expected format, such as an invalid version string.
* Setting client info changes how the runtime attributes its telemetry. It does not change what the runtime records.
* If every field is unset or empty, the SDK omits client info from the handshake and the runtime keeps its default attribution.
