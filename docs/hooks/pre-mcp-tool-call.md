# Pre-MCP tool call hook

The `preMcpToolCall` hook runs before the runtime sends a tool call to an MCP server.
It sees the server name, the tool name and the arguments, and it can control the `_meta` object that travels with the outgoing MCP request.

Use it to attach request metadata, such as a tenant or trace identifier, to calls for particular servers.
The hook cannot allow, deny or change the call itself: to control whether a tool runs, use [`onPreToolUse`](./pre-tool-use.md) or the permission handler.

## Input and output

| Input field | Type | Description |
| --- | --- | --- |
| `sessionId` | string | Session reported by the hook input, not necessarily the callback owner's session |
| `timestamp` | date/time | Time the hook was invoked |
| `workingDirectory` | string | Current working directory |
| `toolCallId` | string (optional) | Tool call ID; matches the ID on `preToolUse` and `postToolUse` for the same call. Absent on older runtimes |
| `serverName` | string | Name of the MCP server being called |
| `toolName` | string | Name of the MCP tool being called |
| `arguments` | JSON value | Arguments for the MCP tool call |
| `_meta` | object (optional) | The `_meta` object the request would carry, when there is one. It includes the runtime's [tool call and turn IDs](#tool-call-and-turn-ids-in-_meta) when the call has them |

The output has one optional field, `metaToUse`, and its three states are different:

| Hook result | Effect on the outgoing request |
| --- | --- |
| No output, or `metaToUse` absent | Keep the current `_meta` |
| `metaToUse` set to an object | Use that object as `_meta` |
| `metaToUse` set to `null` | Send no hook-controlled `_meta` |

If the hook throws, returns an invalid `metaToUse` or times out, the runtime logs it and the call goes ahead with the current `_meta`.

## Tool call and turn IDs in `_meta`

The runtime adds these keys to `params._meta` on each `tools/call` request it sends for a model tool call, so an MCP server can match the request to the session event log.
Both keys are a stable contract: servers can rely on their names and meaning.

| Key | Value | Present when |
| --- | --- | --- |
| `com.github.copilot/toolCallId` | The `toolCallId` of the call's `tool.execution_start` event | The call has a tool call ID |
| `com.github.copilot/turnId` | The `clientTurnId` the client passed to `send` or `sendAndWait` for the turn the call belongs to | The turn was started by a send that set `clientTurnId` |

`clientTurnId` is opaque to the runtime: it carries the value without parsing it, so a host can pass the turn ID it publishes in its own event log.
The value applies to every model request and tool call the send drives:

* A send takes effect when it starts a turn. A send without `clientTurnId` clears the value.
  A slash command or model change that runs between turns leaves it unchanged.
* A send that steers the running turn (`mode: "immediate"`) replaces the value when the turn consumes it, from the next model request onward.
  A steering send without `clientTurnId` clears it.
  This matches a host that opens a successor turn when a user steers.
* Steering with a non-user `source` or an empty `displayPrompt` leaves the value unchanged.
* If the turn ends before taking a steering send, the send starts its own turn with its own value.
* Autopilot continuations keep the current value: that of the send that started the turn, or of the last steering send it consumed.
* A sub-agent's calls carry the value of the turn that started the sub-agent, even after that turn ends or is steered while a background sub-agent keeps running.
  An agent started with `session.tasks.startAgent` belongs to no turn and carries no turn ID.
* Turns the runtime starts itself, such as a background task notification or a scheduled prompt, carry no turn ID.

The `user.message` event for each message a send delivers echoes the send's value in `data.clientTurnId`, so a host can bind its turn from the event log instead of from the `messageId` that `send` returns.
The echo appears when the message is consumed: when it starts a turn, drains from the queue, or is taken as steering. Each message carries its own send's value, and the field is absent when that send set no `clientTurnId`.
Session events are persisted, so the echo survives a resume. The value the runtime stamps on `tools/call` is held in memory only and isn't restored when a session resumes; the next send sets it again.

The runtime adds both keys before the hooks run, so every hook's input `_meta` contains them, even after an earlier hook replaced `_meta` or set it to `null`.
A hook that sets a key keeps its own value. A hook can't remove a key: if `metaToUse` leaves it out or is `null`, the runtime adds it back.

Requests without a model tool call, such as a tool call an MCP App's UI starts, carry neither key.
Runtimes released before a key was added don't send it, and they reject a `send` that sets `clientTurnId`.

## Examples

Each example adds a tag to the `_meta` of calls to the `github` MCP server and leaves every other server's calls alone.

<details open>
<summary><strong>TypeScript</strong></summary>

<!-- docs-validate: strict -->
```typescript
const session = await client.createSession({
  hooks: {
    onPreMcpToolCall: async (input) => {
      if (input.serverName !== "github") {
        return;
      }
      return { metaToUse: { ...input._meta, "app/request-source": "docs-example" } };
    },
  },
});
```

</details>

<details>
<summary><strong>Python</strong></summary>

<!-- docs-validate: hidden -->
```python
from copilot import CopilotClient

async def main(client: CopilotClient):
    def tag_request(input_data, invocation):
        if input_data["serverName"] != "github":
            return None
        meta = dict(input_data.get("_meta") or {})
        meta["app/request-source"] = "docs-example"
        return {"metaToUse": meta}

    session = await client.create_session(hooks={"on_pre_mcp_tool_call": tag_request})
```
<!-- /docs-validate: hidden -->
```python
def tag_request(input_data, invocation):
    if input_data["serverName"] != "github":
        return None
    meta = dict(input_data.get("_meta") or {})
    meta["app/request-source"] = "docs-example"
    return {"metaToUse": meta}

session = await client.create_session(hooks={"on_pre_mcp_tool_call": tag_request})
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
	var client *copilot.Client
	ctx := context.Background()
	session, err := client.CreateSession(ctx, &copilot.SessionConfig{
		Hooks: &copilot.SessionHooks{
			OnPreMCPToolCall: func(input copilot.PreMCPToolCallHookInput, invocation copilot.HookInvocation) (*copilot.PreMCPToolCallHookOutput, error) {
				if input.ServerName != "github" {
					return nil, nil
				}
				meta := map[string]any{}
				if existing, ok := input.Meta.(map[string]any); ok {
					for key, value := range existing {
						meta[key] = value
					}
				}
				meta["app/request-source"] = "docs-example"
				return &copilot.PreMCPToolCallHookOutput{MetaToUse: meta}, nil
			},
		},
	})
	_, _ = session, err
}
```
<!-- /docs-validate: hidden -->
```go
session, err := client.CreateSession(ctx, &copilot.SessionConfig{
	Hooks: &copilot.SessionHooks{
		OnPreMCPToolCall: func(input copilot.PreMCPToolCallHookInput, invocation copilot.HookInvocation) (*copilot.PreMCPToolCallHookOutput, error) {
			if input.ServerName != "github" {
				return nil, nil
			}
			meta := map[string]any{}
			if existing, ok := input.Meta.(map[string]any); ok {
				for key, value := range existing {
					meta[key] = value
				}
			}
			meta["app/request-source"] = "docs-example"
			return &copilot.PreMCPToolCallHookOutput{MetaToUse: meta}, nil
		},
	},
})
```

</details>

<details>
<summary><strong>.NET</strong></summary>

<!-- docs-validate: hidden -->
```csharp
using System.Text.Json;
using GitHub.Copilot;

await using var client = new CopilotClient();

var session = await client.CreateSessionAsync(new SessionConfig
{
    Hooks = new SessionHooks
    {
        OnPreMcpToolCall = (input, invocation) =>
        {
            if (input.ServerName != "github")
            {
                return Task.FromResult<PreMcpToolCallHookOutput?>(null);
            }

            var meta = new Dictionary<string, object?>();
            foreach (var (key, value) in input.Meta ?? new Dictionary<string, JsonElement>())
            {
                meta[key] = value;
            }
            meta["app/request-source"] = "docs-example";

            return Task.FromResult<PreMcpToolCallHookOutput?>(new()
            {
                MetaToUse = JsonSerializer.SerializeToElement(meta),
            });
        },
    },
});
```
<!-- /docs-validate: hidden -->
```csharp
var session = await client.CreateSessionAsync(new SessionConfig
{
    Hooks = new SessionHooks
    {
        OnPreMcpToolCall = (input, invocation) =>
        {
            if (input.ServerName != "github")
            {
                return Task.FromResult<PreMcpToolCallHookOutput?>(null);
            }

            var meta = new Dictionary<string, object?>();
            foreach (var (key, value) in input.Meta ?? new Dictionary<string, JsonElement>())
            {
                meta[key] = value;
            }
            meta["app/request-source"] = "docs-example";

            return Task.FromResult<PreMcpToolCallHookOutput?>(new()
            {
                MetaToUse = JsonSerializer.SerializeToElement(meta),
            });
        },
    },
});
```

</details>

<details>
<summary><strong>Java</strong></summary>

<!-- docs-validate: hidden -->
```java
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.copilot.CopilotClient;
import com.github.copilot.rpc.*;
import java.util.concurrent.CompletableFuture;

public class PreMcpToolCallExample {
    public static void main(String[] args) throws Exception {
        try (var client = new CopilotClient()) {
            var hooks = new SessionHooks().setOnPreMcpToolCall((input, invocation) -> {
                if (!"github".equals(input.getServerName())) {
                    return CompletableFuture.completedFuture(null);
                }
                var meta = new ObjectMapper().createObjectNode();
                if (input.getMeta() != null) {
                    input.getMeta().forEach(meta::set);
                }
                meta.put("app/request-source", "docs-example");
                return CompletableFuture.completedFuture(PreMcpToolCallHookOutput.withMeta(meta));
            });

            var session = client.createSession(
                new SessionConfig()
                    .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                    .setHooks(hooks)
            ).get();
        }
    }
}
```
<!-- /docs-validate: hidden -->
```java
var hooks = new SessionHooks().setOnPreMcpToolCall((input, invocation) -> {
    if (!"github".equals(input.getServerName())) {
        return CompletableFuture.completedFuture(null);
    }
    var meta = new ObjectMapper().createObjectNode();
    if (input.getMeta() != null) {
        input.getMeta().forEach(meta::set);
    }
    meta.put("app/request-source", "docs-example");
    return CompletableFuture.completedFuture(PreMcpToolCallHookOutput.withMeta(meta));
});

var session = client.createSession(
    new SessionConfig()
        .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
        .setHooks(hooks)
).get();
```

</details>

<details>
<summary><strong>Rust</strong></summary>

<!-- docs-validate: hidden -->
```rust
use serde_json::{json, Value};

struct MyHooks;

#[async_trait]
impl SessionHooks for MyHooks {
    async fn on_pre_mcp_tool_call(
        &self,
        input: PreMcpToolCallInput,
        _ctx: HookContext,
    ) -> Option<PreMcpToolCallOutput> {
        if input.server_name != "github" {
            return None;
        }
        let mut meta = match input.meta {
            Some(Value::Object(existing)) => existing,
            _ => Default::default(),
        };
        meta.insert("app/request-source".into(), json!("docs-example"));
        Some(PreMcpToolCallOutput {
            meta_to_use: Some(Value::Object(meta)),
        })
    }
}

let session = client
    .create_session(SessionConfig::default().with_hooks(Arc::new(MyHooks)))
    .await?;
```
<!-- /docs-validate: hidden -->

```rust
#[async_trait]
impl SessionHooks for MyHooks {
    async fn on_pre_mcp_tool_call(
        &self,
        input: PreMcpToolCallInput,
        _ctx: HookContext,
    ) -> Option<PreMcpToolCallOutput> {
        if input.server_name != "github" {
            return None;
        }
        let mut meta = match input.meta {
            Some(Value::Object(existing)) => existing,
            _ => Default::default(),
        };
        meta.insert("app/request-source".into(), json!("docs-example"));
        Some(PreMcpToolCallOutput {
            meta_to_use: Some(Value::Object(meta)),
        })
    }
}

let session = client
    .create_session(SessionConfig::default().with_hooks(Arc::new(MyHooks)))
    .await?;
```

To remove the hook-controlled `_meta`, return `meta_to_use: Some(Value::Null)`.

</details>

In the other languages, remove it by returning an output whose `metaToUse` is `null` (Python `{"metaToUse": None}`, Go and .NET an output with a nil `MetaToUse`, Java `PreMcpToolCallHookOutput.removeMeta()`), as opposed to returning no output at all.
