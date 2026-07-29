# Server-to-server authentication

Use a short-lived installation access token when a service needs to make Copilot requests on behalf of an organization without a user's credentials. In GitHub Actions, use the built-in `GITHUB_TOKEN` instead.

## GitHub Actions

For workflows in an organization-owned repository, grant the built-in token permission to make Copilot requests:

```yaml
permissions:
  contents: read
  copilot-requests: write

jobs:
  copilot:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v6
      - run: your-application
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

The organization's **Allow use of Copilot CLI billed to the organization** policy must be enabled. This approach needs no GitHub App or stored authentication secret. For details, see [Using Copilot CLI in GitHub Actions with GITHUB_TOKEN](https://docs.github.com/en/copilot/how-tos/copilot-cli/use-copilot-cli-in-actions).

## Other services and CI systems

For services outside GitHub Actions:

1. Create a GitHub App with the **Copilot Requests** repository permission set to **Read & write**.
1. Install it on the organization that should be billed. The current Copilot permission check requires **All repositories** access.
1. [Create an installation access token](https://docs.github.com/en/apps/creating-github-apps/authenticating-with-a-github-app/generating-an-installation-access-token-for-a-github-app) with a repository ID and the Copilot permission:

   ```json
   {
     "repository_ids": [123456789],
     "permissions": {
       "copilot_requests": "write"
     }
   }
   ```

1. Pass the resulting `ghs_` token to the runtime as `COPILOT_GITHUB_TOKEN`.

The organization must be enabled for Copilot requests from GitHub App installations. Installation tokens expire after one hour.

> [!WARNING]
> Do not pass an installation token through the SDK's `gitHubToken`, `github_token`, or equivalent option. That option is for user tokens. Installation tokens must use the runtime environment authentication path.

## Configure the runtime

The following examples assume the minted token is in `INSTALLATION_TOKEN`. They pass it only to the child runtime and disable fallback to stored user credentials.

<details open>
<summary><strong>TypeScript</strong></summary>

```typescript
import { CopilotClient, RuntimeConnection } from "@github/copilot-sdk";

const token = process.env.INSTALLATION_TOKEN;
if (!token) throw new Error("INSTALLATION_TOKEN is required");

const client = new CopilotClient({
    connection: RuntimeConnection.forStdio(),
    env: {
        ...process.env,
        COPILOT_GITHUB_TOKEN: token,
    },
    useLoggedInUser: false,
});
```

</details>
<details>
<summary><strong>Python</strong></summary>

```python
import os

from copilot import CopilotClient, RuntimeConnection

client = CopilotClient(
    connection=RuntimeConnection.for_stdio(),
    env={**os.environ, "COPILOT_GITHUB_TOKEN": os.environ["INSTALLATION_TOKEN"]},
    use_logged_in_user=False,
)
```

</details>
<details>
<summary><strong>Go</strong></summary>

```go
package main

import (
	"log"
	"os"

	copilot "github.com/github/copilot-sdk/go"
)

func main() {
	token, ok := os.LookupEnv("INSTALLATION_TOKEN")
	if !ok {
		log.Fatal("INSTALLATION_TOKEN is required")
	}
	client := copilot.NewClient(&copilot.ClientOptions{
		Connection:      copilot.StdioConnection{},
		Env:             append(os.Environ(), "COPILOT_GITHUB_TOKEN="+token),
		UseLoggedInUser: copilot.Bool(false),
	})
	_ = client
}
```

</details>
<details>
<summary><strong>Rust</strong></summary>

```rust
use github_copilot_sdk::{ClientOptions, Transport};

fn main() {
    let token = std::env::var("INSTALLATION_TOKEN").expect("INSTALLATION_TOKEN is required");
    let options = ClientOptions::new()
        .with_transport(Transport::Stdio)
        .with_env([("COPILOT_GITHUB_TOKEN", token)])
        .with_use_logged_in_user(false);
    drop(options);
}
```

</details>
<details>
<summary><strong>.NET</strong></summary>

```csharp
using System.Collections;
using GitHub.Copilot;

var token = Environment.GetEnvironmentVariable("INSTALLATION_TOKEN")
    ?? throw new InvalidOperationException("INSTALLATION_TOKEN is required");
var environment = Environment.GetEnvironmentVariables()
    .Cast<DictionaryEntry>()
    .ToDictionary(entry => (string)entry.Key, entry => entry.Value?.ToString() ?? "");
environment["COPILOT_GITHUB_TOKEN"] = token;

await using var client = new CopilotClient(new CopilotClientOptions
{
    Connection = RuntimeConnection.ForStdio(),
    Environment = environment,
    UseLoggedInUser = false,
});
```

</details>
<details>
<summary><strong>Java</strong></summary>

```java
import com.github.copilot.CopilotClient;
import com.github.copilot.rpc.CopilotClientOptions;
import java.util.HashMap;
import java.util.Objects;

var environment = new HashMap<>(System.getenv());
var token = Objects.requireNonNull(
    System.getenv("INSTALLATION_TOKEN"), "INSTALLATION_TOKEN is required");
environment.put("COPILOT_GITHUB_TOKEN", token);

try (var client = new CopilotClient(new CopilotClientOptions()
        .setEnvironment(environment)
        .setUseLoggedInUser(false))) {
    // Use the client.
}
```

</details>

For in-process FFI, set `COPILOT_GITHUB_TOKEN` in the host environment before loading the runtime; per-client environment options are not supported. For an existing runtime URI, set it on that runtime process.

## Refresh tokens

Mint a new installation token before the current token expires. For a child process, restart the SDK client with the new environment. For an in-process or existing runtime, restart the host runtime with the new token.

## Troubleshooting

| Symptom | Check |
|---|---|
| `401 Unauthorized` | Confirm the organization supports GitHub App installation authentication for Copilot. |
| Error mentioning user information | Confirm the installation token is in `COPILOT_GITHUB_TOKEN`, not the SDK's explicit token option. |
| `403 Forbidden` | Confirm the app has **Copilot Requests: Read & write**, the installation covers all repositories, and the token request contains `repository_ids` and `copilot_requests: write`. |
| Wrong account billed | Confirm the installation belongs to the intended organization. |

## Further reading

* [Authenticate Copilot SDK](./authenticate.md): other authentication methods and priority
* [Generating an installation access token](https://docs.github.com/en/apps/creating-github-apps/authenticating-with-a-github-app/generating-an-installation-access-token-for-a-github-app): GitHub App token creation
