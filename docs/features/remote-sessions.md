# Remote sessions

Session history can be exported to GitHub web and mobile via [Mission Control](https://github.com). This is read-only history export, not remote control of the SDK session.

For running sessions on GitHub-hosted compute, see [Cloud Sessions](./cloud-sessions.md).

## Prerequisites

* The user must be authenticated (GitHub token or logged-in user)
* The session's working directory must be a GitHub repository

## Enabling remote sessions

### On-demand (per-session toggle)

Use the experimental raw `session.rpc.remote.enable()` API with export mode to start read-only history export mid-session, and `session.rpc.remote.disable()` to stop it. Export mode does not enable AHP connections or remote steering; omitting the mode requests both export and remote steering.

<!-- tabs:start -->

#### TypeScript

<!-- docs-validate: skip -->
```typescript
const result = await session.rpc.remote.enable({ mode: "export" });
console.log("Remote URL:", result.url);

// Later: stop sharing
await session.rpc.remote.disable();
```

#### Python

<!-- docs-validate: hidden -->
```python
from copilot.generated.rpc import RemoteEnableRequest, RemoteSessionMode
from copilot.session import CopilotSession

async def main(session: CopilotSession):
    result = await session.rpc.remote.enable(RemoteEnableRequest(mode=RemoteSessionMode.EXPORT))
    print(f"Remote URL: {result.url}")

    # Later: stop sharing
    await session.rpc.remote.disable()
```
<!-- /docs-validate: hidden -->

```python
from copilot.generated.rpc import RemoteEnableRequest, RemoteSessionMode

result = await session.rpc.remote.enable(RemoteEnableRequest(mode=RemoteSessionMode.EXPORT))
print(f"Remote URL: {result.url}")

# Later: stop sharing
await session.rpc.remote.disable()
```

#### Go

<!-- docs-validate: skip -->
```go
mode := rpc.RemoteSessionModeExport
result, err := session.RPC.Remote.Enable(ctx, &rpc.RemoteEnableRequest{Mode: &mode})
if err != nil {
    log.Fatal(err)
}
if result.URL != nil {
    fmt.Println("Remote URL:", *result.URL)
}

// Later: stop sharing
_, err = session.RPC.Remote.Disable(ctx)
```

#### C#

<!-- docs-validate: skip -->
```csharp
var result = await session.Rpc.Remote.EnableAsync(mode: RemoteSessionMode.Export);
Console.WriteLine($"Remote URL: {result.Url}");

// Later: stop sharing
await session.Rpc.Remote.DisableAsync();
```

#### Rust

<!-- docs-validate: skip -->
```rust
use github_copilot_sdk::rpc::{RemoteEnableRequest, RemoteSessionMode};

let result = session
    .rpc()
    .remote()
    .enable(RemoteEnableRequest {
        mode: Some(RemoteSessionMode::Export),
    })
    .await?;
if let Some(url) = &result.url {
    println!("Remote URL: {url}");
}

// Later: stop sharing
session.rpc().remote().disable().await?;
```

<!-- tabs:end -->

## QR code generation

The remote URL can be rendered as a QR code for easy mobile access. The SDK provides the URL—use your preferred QR code library:

* **TypeScript**: [qrcode](https://www.npmjs.com/package/qrcode)
* **Python**: [qrcode](https://pypi.org/project/qrcode/)
* **Go**: [go-qrcode](https://github.com/skip2/go-qrcode)
* **C#**: [QRCoder](https://www.nuget.org/packages/QRCoder)
* **Rust**: [qrcode](https://crates.io/crates/qrcode)

## Notes

* If the working directory is not a GitHub repository, on-demand export returns an error.
* Remote sessions require authentication. Ensure `gitHubToken` or `useLoggedInUser` is configured.
