# Session limits

Session limits let an application set an AI Credits budget for a Copilot session. Use `sessionLimits` when creating or resuming a session to set a soft cap for the current accounting window.

## Configure a session limit

Set `maxAiCredits` to the AI Credits soft cap for the session's current accounting window. Usage is checked after model calls return, so one response can exceed the configured value before the runtime blocks the next model call. The SDK forwards this value to the Copilot CLI when it creates or resumes the session.

<details open>
<summary><strong>TypeScript</strong></summary>

<!-- docs-validate: strict -->
<!-- docs-validate: hidden -->
```typescript
import { approveAll } from "@github/copilot-sdk";

const session = await client.createSession({
    onPermissionRequest: approveAll,
    sessionLimits: {
        maxAiCredits: 30,
    },
});

const resumed = await client.resumeSession(session.sessionId, {
    onPermissionRequest: approveAll,
    sessionLimits: {
        maxAiCredits: 30,
    },
});
```
<!-- /docs-validate: hidden -->

```typescript
const session = await client.createSession({
    onPermissionRequest: approveAll,
    sessionLimits: {
        maxAiCredits: 30,
    },
});

const resumed = await client.resumeSession(session.sessionId, {
    onPermissionRequest: approveAll,
    sessionLimits: {
        maxAiCredits: 30,
    },
});
```

</details>
<details>
<summary><strong>Python</strong></summary>

<!-- docs-validate: hidden -->
```python
from copilot import CopilotClient
from copilot.session import PermissionHandler

async def main(client: CopilotClient):
    session = await client.create_session(
        on_permission_request=PermissionHandler.approve_all,
        session_limits={
            "max_ai_credits": 30,
        },
    )

    resumed = await client.resume_session(
        session.session_id,
        on_permission_request=PermissionHandler.approve_all,
        session_limits={
            "max_ai_credits": 30,
        },
    )
```
<!-- /docs-validate: hidden -->

```python
session = await client.create_session(
    on_permission_request=PermissionHandler.approve_all,
    session_limits={
        "max_ai_credits": 30,
    },
)

resumed = await client.resume_session(
    session.session_id,
    on_permission_request=PermissionHandler.approve_all,
    session_limits={
        "max_ai_credits": 30,
    },
)
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
	"github.com/github/copilot-sdk/go/rpc"
)

func main() {
	ctx := context.Background()
	client := copilot.NewClient(nil)

	session, err := client.CreateSession(ctx, &copilot.SessionConfig{
		OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		SessionLimits: &rpc.SessionLimitsConfig{
			MaxAiCredits: copilot.Float64(30),
		},
	})

	resumed, err := client.ResumeSession(ctx, session.SessionID, &copilot.ResumeSessionConfig{
		OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		SessionLimits: &rpc.SessionLimitsConfig{
			MaxAiCredits: copilot.Float64(30),
		},
	})
	_, _ = resumed, err
}
```
<!-- /docs-validate: hidden -->

```go
session, err := client.CreateSession(ctx, &copilot.SessionConfig{
	OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
	SessionLimits: &rpc.SessionLimitsConfig{
		MaxAiCredits: copilot.Float64(30),
	},
})

resumed, err := client.ResumeSession(ctx, session.SessionID, &copilot.ResumeSessionConfig{
	OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
	SessionLimits: &rpc.SessionLimitsConfig{
		MaxAiCredits: copilot.Float64(30),
	},
})
```

</details>
<details>
<summary><strong>.NET</strong></summary>

<!-- docs-validate: hidden -->
```csharp
await using var client = new CopilotClient();

#pragma warning disable GHCP001 // Session limits are experimental.
var session = await client.CreateSessionAsync(new SessionConfig
{
    OnPermissionRequest = PermissionHandler.ApproveAll,
    SessionLimits = new SessionLimitsConfig
    {
        MaxAiCredits = 30,
    },
});

var resumed = await client.ResumeSessionAsync(session.SessionId, new ResumeSessionConfig
{
    OnPermissionRequest = PermissionHandler.ApproveAll,
    SessionLimits = new SessionLimitsConfig
    {
        MaxAiCredits = 30,
    },
});
```
<!-- /docs-validate: hidden -->

```csharp
#pragma warning disable GHCP001 // Session limits are experimental.
var session = await client.CreateSessionAsync(new SessionConfig
{
    OnPermissionRequest = PermissionHandler.ApproveAll,
    SessionLimits = new SessionLimitsConfig
    {
        MaxAiCredits = 30,
    },
});

var resumed = await client.ResumeSessionAsync(session.SessionId, new ResumeSessionConfig
{
    OnPermissionRequest = PermissionHandler.ApproveAll,
    SessionLimits = new SessionLimitsConfig
    {
        MaxAiCredits = 30,
    },
});
```

The .NET `SessionLimits` property raises the `GHCP001` experimental diagnostic. Suppress it with `#pragma warning disable GHCP001` or a project-level `<NoWarn>GHCP001</NoWarn>`.

</details>
<details>
<summary><strong>Java</strong></summary>

<!-- docs-validate: hidden -->
```java
import com.github.copilot.CopilotClient;
import com.github.copilot.CopilotSession;
import com.github.copilot.generated.rpc.SessionLimitsConfig;
import com.github.copilot.rpc.*;

public class SessionLimitsExample {
    public static void main(String[] args) throws Exception {
        try (var client = new CopilotClient()) {
            client.start().get();

            CopilotSession session = client
                    .createSession(new SessionConfig()
                            .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                            .setSessionLimits(new SessionLimitsConfig(30.0)))
                    .get();

            CopilotSession resumed = client
                    .resumeSession(session.getSessionId(), new ResumeSessionConfig()
                            .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                            .setSessionLimits(new SessionLimitsConfig(30.0)))
                    .get();
        }
    }
}
```
<!-- /docs-validate: hidden -->

```java
import com.github.copilot.generated.rpc.SessionLimitsConfig;

CopilotSession session = client
        .createSession(new SessionConfig()
                .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                .setSessionLimits(new SessionLimitsConfig(30.0)))
        .get();

CopilotSession resumed = client
        .resumeSession(session.getSessionId(), new ResumeSessionConfig()
                .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                .setSessionLimits(new SessionLimitsConfig(30.0)))
        .get();
```

</details>
<details>
<summary><strong>Rust</strong></summary>

```rust
let limits = SessionLimitsConfig {
    max_ai_credits: Some(30.0),
};

let session = client
    .create_session(
        SessionConfig::default()
            .approve_all_permissions()
            .with_session_limits(limits.clone()),
    )
    .await?;

let resumed = client
    .resume_session(
        ResumeSessionConfig::new(session.id().clone())
            .approve_all_permissions()
            .with_session_limits(limits),
    )
    .await?;
```

</details>

## Observe budget events

Applications can subscribe to session events to update UI when the soft cap changes or the session reaches the exhausted-budget flow.

| Event type | When it is emitted | Important fields |
|---|---|---|
| `session.session_limits_changed` | Active session limits changed. A `null` `sessionLimits` value means no limits are active. | `sessionLimits.maxAiCredits?` |
| `session.usage_checkpoint` | The runtime records durable aggregate usage for resume and accounting. | `totalNanoAiu`, `totalPremiumRequests?` |
| `session_limits_exhausted.requested` | The session reached the exhausted-budget flow and needs a user decision before continuing. | `requestId`, `maxAiCredits`, `usedAiCredits` |
| `session_limits_exhausted.completed` | The exhausted-limit prompt was resolved. | `requestId`, `response.action`, `response.additionalAiCredits?`, `response.maxAiCredits?` |

`session_limits_exhausted.requested` is not like the first two events. The runtime emits it only when a consumer has registered interest in it, and subscribing with `session.on` does not count. Once it is emitted, the turn waits until the application answers with the `session.ui.handlePendingSessionLimitsExhausted` RPC, using the `requestId` from the event. The `response.action` is `"add"` (with `additionalAiCredits`), `"set"` (with `maxAiCredits`), `"unset"` or `"cancel"`.

Use the generated event types for the SDK language you are using. For example, TypeScript registers interest with `session.rpc.eventLog.registerInterest`, narrows by event type, and answers the request:

<!-- docs-validate: strict -->
<!-- docs-validate: hidden -->
```typescript
import { approveAll } from "@github/copilot-sdk";

const session = await client.createSession({ onPermissionRequest: approveAll });

await session.rpc.eventLog.registerInterest({
    eventType: "session_limits_exhausted.requested",
});

session.on("session_limits_exhausted.requested", async (event) => {
    // Ask the user here, then resolve the pending request.
    await session.rpc.ui.handlePendingSessionLimitsExhausted({
        requestId: event.data.requestId,
        response: { action: "add", additionalAiCredits: 10 },
    });
});
```
<!-- /docs-validate: hidden -->

```typescript
await session.rpc.eventLog.registerInterest({
    eventType: "session_limits_exhausted.requested",
});

session.on("session_limits_exhausted.requested", async (event) => {
    // Ask the user here, then resolve the pending request.
    await session.rpc.ui.handlePendingSessionLimitsExhausted({
        requestId: event.data.requestId,
        response: { action: "add", additionalAiCredits: 10 },
    });
});
```

`registerInterest` returns a `handle`. Pass it to `session.rpc.eventLog.releaseInterest` to stop the runtime from treating your application as a consumer of the event.
