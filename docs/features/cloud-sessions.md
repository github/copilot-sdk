# Cloud sessions

Cloud sessions run Copilot work on GitHub-hosted compute through Mission Control. Use them when your app should create a session that executes remotely instead of starting a local Copilot CLI session on the user's machine or your server.

## Prerequisites

Before creating a cloud session, make sure:

* The user has Copilot access with cloud-agent entitlement.
* The session can authenticate to GitHub, either with a user token or a logged-in Copilot CLI identity.
* You can associate the session with a GitHub repository. This is optional in the SDK type, but recommended so Mission Control and the cloud agent have repository context.
* Organization policies allow remote control and viewing sessions from cloud surfaces.

## Creating a cloud session

Set the create-session `cloud` option to create a cloud session. You can include repository metadata to associate the cloud session with a GitHub repository.

<!-- tabs:start -->

### TypeScript

<!-- docs-validate: skip -->
```typescript
import { CopilotClient } from "@github/copilot-sdk";

const client = new CopilotClient();
await client.start();

const session = await client.createSession({
  onPermissionRequest: async () => ({ kind: "approve-once" }),
  cloud: {
    repository: {
      owner: "github",
      name: "copilot-sdk",
      branch: "main",
    },
  },
});
```

### Python

<!-- docs-validate: skip -->
```python
from copilot import CopilotClient, CloudSessionOptions, CloudSessionRepository
from copilot.session import PermissionHandler

client = CopilotClient()
await client.start()

session = await client.create_session(
    on_permission_request=PermissionHandler.approve_all,
    cloud=CloudSessionOptions(
        repository=CloudSessionRepository(
            owner="github",
            name="copilot-sdk",
            branch="main",
        )
    ),
)
```

### Go

<!-- docs-validate: skip -->
```go
client := copilot.NewClient(nil)
if err := client.Start(ctx); err != nil {
    return err
}

session, err := client.CreateSession(ctx, &copilot.SessionConfig{
    Cloud: &copilot.CloudSessionOptions{
        Repository: &copilot.CloudSessionRepository{
            Owner:  "github",
            Name:   "copilot-sdk",
            Branch: "main",
        },
    },
    OnPermissionRequest: func(req copilot.PermissionRequest, inv copilot.PermissionInvocation) (rpc.PermissionDecision, error) {
        return &rpc.PermissionDecisionApproveOnce{}, nil
    },
})
_ = session
```

### .NET

<!-- docs-validate: skip -->
```csharp
await using var client = new CopilotClient();

var session = await client.CreateSessionAsync(new SessionConfig
{
    Cloud = new CloudSessionOptions
    {
        Repository = new CloudSessionRepository
        {
            Owner = "github",
            Name = "copilot-sdk",
            Branch = "main",
        },
    },
    OnPermissionRequest = (req, inv) =>
        Task.FromResult(PermissionDecision.ApproveOnce()),
});
```

### Java

<!-- docs-validate: skip -->
```java
import com.github.copilot.CopilotClient;
import com.github.copilot.rpc.*;

try (var client = new CopilotClient()) {
    client.start().get();

    var session = client.createSession(
        new SessionConfig()
            .setCloud(new CloudSessionOptions()
                .setRepository(new CloudSessionRepository()
                    .setOwner("github")
                    .setName("copilot-sdk")
                    .setBranch("main")))
            .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
    ).get();
}
```

### Rust

<!-- docs-validate: skip -->
```rust
use github_copilot_sdk::{CloudSessionOptions, CloudSessionRepository, SessionConfig};
use github_copilot_sdk::handler::PermissionResult;

let session = client.create_session(
    SessionConfig::default()
        .with_cloud(CloudSessionOptions::with_repository(
            CloudSessionRepository::new("github", "copilot-sdk").with_branch("main"),
        ))
        .with_permission_handler(|_req, _inv| async {
            Ok(PermissionResult::approve_once())
        }),
).await?;
```

<!-- tabs:end -->

## Repository association

The `cloud.repository` object associates the cloud session with a GitHub repository:

| Field | Required | Description |
|-------|----------|-------------|
| `owner` | Yes | Repository owner or organization. |
| `name` | Yes | Repository name. |
| `branch` | No | Branch to use for repository context. Omit it to let the runtime choose the default branch or current repository context. |

Repository association is optional in the SDK type, but include it whenever your app knows the target repository. It helps Mission Control display the session in the right context and gives the cloud agent a clearer starting point.

Use `branch` when the work should start from a specific branch. If your app is creating sessions from pull requests, issue triage flows, or deployment workflows, pass the branch that matches the user-visible task.

## Resuming a cloud session

The `cloud` option only applies when creating a new session. To resume an existing cloud session, use the standard resume API for the SDK language:

<!-- docs-validate: skip -->
```typescript
const session = await client.resumeSession("session-id");
```

Do not pass `cloud` again on resume. The saved session metadata determines that the session is cloud-backed, and resume follows the normal session resume path.

## Org policies and entitlements

Cloud session creation can fail when the user or organization is not entitled to cloud-agent execution or when organization-level policies block the flow. In particular, policies for remote control or viewing sessions from cloud surfaces can prevent Mission Control from creating the cloud task.

When this happens, the runtime reports a `"policy_blocked"` failure reason for cloud task creation. Treat this as an authorization or policy outcome, not as a transient infrastructure failure.

In TypeScript, check for the reason before retrying:

<!-- docs-validate: skip -->
```typescript
try {
  await client.createSession({ cloud: { repository } });
} catch (error) {
  if ((error as { reason?: string }).reason === "policy_blocked") {
    // Show an admin-facing message or link to org policy settings.
  }
  throw error;
}
```

In languages where SDK errors are represented differently, inspect the surfaced error reason or code and handle `"policy_blocked"` explicitly. Retrying without a policy change is not expected to succeed.

## Integration ID and routing

Cloud sessions are stamped with a `Copilot-Integration-Id` header derived from the `GITHUB_COPILOT_INTEGRATION_ID` environment variable. This integration ID is used by Mission Control for routing, attribution, and integration-specific behavior.

For multi-user server guidance and full integration ID details, see [Multi-tenancy](../setup/multi-tenancy.md).

Mission Control routes SDK-created cloud sessions to the `copilot-developer-sandbox` agent slug. The name is an internal routing slug for the cloud agent and does not mean the session uses the local Windows sandbox.

## Advanced: `COPILOT_MC_BASE_URL`

By default, the runtime derives the Mission Control base URL from the configured Copilot API URL. Set `COPILOT_MC_BASE_URL` only when you need to override that Mission Control endpoint.

This may be required for GitHub Enterprise Server deployments. Confirm the correct value and support status with your GitHub representative before relying on it in production.

<!-- docs-validate: skip -->
```shell
COPILOT_MC_BASE_URL="https://example.com/agents"
```

## Cloud sessions vs. remote sessions

| Capability | Remote sessions | Cloud sessions |
|------------|-----------------|----------------|
| Execution location | Local machine or your server | GitHub-hosted compute |
| Mission Control role | Shares a local session to GitHub web/mobile | Creates and routes the hosted session |
| SDK option | `remote: true` on the client or session | `cloud: { ... }` on create session |
| Resume path | Standard resume | Standard resume |
| Windows sandbox relation | Unrelated | Unrelated |

Use remote sessions when the session should execute where the SDK runtime is already running, but also be accessible from Mission Control. Use cloud sessions when the session should execute on GitHub-hosted compute.

## Troubleshooting

| Symptom | Likely cause | What to check |
|---------|--------------|---------------|
| Cloud session creation returns `"policy_blocked"` | Organization policy blocks remote control or view from cloud flows | Check org Copilot policies and user entitlement |
| Session creates without repository context | `cloud.repository` was omitted | Pass `owner`, `name`, and optionally `branch` |
| Resume ignores a new `cloud` option | `cloud` only applies to new sessions | Resume the existing session normally |
| Confusion with sandbox settings | Windows sandbox and cloud sessions are separate | Do not use `SANDBOX=true` for cloud execution |

## See also

* [Remote Sessions](./remote-sessions.md): share locally hosted sessions through Mission Control
* [Multi-tenancy](../setup/multi-tenancy.md): integration IDs and server deployment patterns
* [Authentication](../auth/index.md): configure GitHub authentication for SDK sessions
