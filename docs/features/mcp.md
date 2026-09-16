# Using MCP servers with the GitHub Copilot SDK

The Copilot SDK can integrate with **MCP servers** (Model Context Protocol) to extend the assistant's capabilities with external tools. MCP servers run as separate processes and expose tools (functions) that Copilot can invoke during conversations.

> [!NOTE]
> This is an evolving feature. See [issue #36](https://github.com/github/copilot-sdk/issues/36) for ongoing discussion.

## What is MCP?

[Model Context Protocol (MCP)](https://modelcontextprotocol.io/) is an open standard for connecting AI assistants to external tools and data sources. MCP servers can:

* Execute code or scripts
* Query databases
* Access file systems
* Call external APIs
* And much more

## Server transports

The SDK supports two MCP transport families:

| Type | Description | Use Case |
|------|-------------|----------|
| **Local/Stdio** | Runs as a subprocess, communicates via stdin/stdout | Local tools, file access, custom scripts |
| **HTTP/SSE** | Remote server accessed via HTTP | Shared services, cloud-hosted tools |

Transport and configuration origin are separate concepts. A Connector MCP
endpoint uses the HTTP transport, but its configuration comes from the
Copilot Connectors service rather than user or workspace settings.

## Integrating Copilot Connectors

People use Copilot Connectors by choosing a Connector in the host application,
completing consent when required, and then using its tools. They do not enter an
MCP endpoint, configure authorization, or manage the underlying MCP server.

An SDK-based host implements that experience:

1. Retrieve the supported Connector catalog for the selected account.
1. Present Connector metadata and connection status, and perform Connect,
   Reconnect, or Disconnect through the Connector service.
1. Consume each service-advertised MCP URL without constructing or rewriting it.
1. Supply connected Connector endpoints to the session through
   `connectorMcpServers`.
1. Authorize calls to those endpoints for the same selected GitHub account
   through `onMcpHeadersRefresh`.

The SDK API described here is the session bridge for connected Connectors. It
does not fetch the catalog, render Connector UI, perform consent, or manage
service connections. Existing local stdio, HTTP, SSE, and OAuth MCP behavior
remains unchanged.

The integration boundary is:

| Responsibility | Owner |
| --- | --- |
| List available Connectors and connection state | Connector service and host application |
| Connect, Reconnect, Disconnect, browser consent, and polling | Connector service and host application |
| Select and pin the active account | Host application |
| Load connected Connector endpoints and supply service authorization | Copilot SDK |
| List server state and tools, enable or disable per session, and receive status events | Copilot SDK |
| Apply MCP policy, cache authorization, and retry once after a 401 | Connected Copilot runtime |

> [!NOTE]
> Connector session integration is experimental and only takes effect when
> enabled by the connected runtime. The host must re-supply the effective
> connected set on cold resume.

The host supplies each connected endpoint under a stable Connector server key:

<!-- docs-validate: skip -->

```typescript
const session = await client.createSession({
    connectorMcpServers: {
        [connector.serverKey]: {
            displayName: connector.displayName,
            url: connector.advertisedMcpUrl,
            tools: connector.tools,
            timeout: 30_000,
            authorizationCacheTtlMs: 60_000,
        },
    },
    onMcpHeadersRefresh: async ({ serverKey, serverUrl, reason }) => {
        const authorization = await getConnectorAuthorization({
            account: selectedAccount,
            serverKey,
            serverUrl,
            reason,
        });
        return {
            headers: {
                Authorization: ["Bearer", authorization.accessToken].join(" "),
            },
            ttlMs: authorization.expiresInMs,
        };
    },
});
```

The corresponding configuration and callback names are:

| SDK | Connector MCP servers | Header refresh callback |
| --- | --- | --- |
| Node.js | `connectorMcpServers` | `onMcpHeadersRefresh` |
| Python | `connector_mcp_servers` | `on_mcp_headers_refresh` |
| Go | `ConnectorMCPServers` | `OnMCPHeadersRefresh` |
| .NET | `ConnectorMcpServers` | `OnMcpHeadersRefresh` |
| Java | `setConnectorMcpServers(...)` | `setOnMcpHeadersRefresh(...)` |
| Rust | `with_connector_mcp_servers(...)` | `with_mcp_headers_handler(...)` |

The typed session MCP API supplies UI hooks for already loaded Connectors: list
server status and tools, enable or disable a server for the current session, and
subscribe to MCP loaded and status-change events. Connect, Reconnect,
Disconnect, browser consent, and catalog polling remain operations against the
Connector service.

> [!WARNING]
> The current public runtime contract cannot replace the authoritative
> Connector set on a running session. After a Connect, Reconnect, or Disconnect,
> create or cold-resume a session with the updated `connectorMcpServers` map.
> Do not treat the generic MCP `stopServer` operation as Connector Disconnect:
> an MCP reload can restore a server from the session's original configuration.

### Host responsibilities

SDK hosts must enforce these boundaries:

* **Trusted catalog injection**: Only inject server identities, display metadata,
  endpoints, and tool policy from a trusted catalog. Do not treat model output
  or untrusted content as catalog configuration.
* **Connector service authorization**: Supply the selected account's
  short-lived GitHub authorization only to the exact endpoint advertised by the
  Connector service. The Connector service, not the SDK host, owns downstream
  provider credentials such as Microsoft or Slack tokens.
* **Memory-only authorization**: Keep access tokens and derived authorization
  headers in memory. Do not place them in `connectorMcpServers`, session history,
  workspace state, or persistent MCP OAuth storage.
* **Account binding**: Fetch the catalog and authorization for the same
  authenticated account. Invalidate cached authorization before switching
  accounts so one account's access cannot be reused by another.
* **Expiry and revocation**: Set `ttlMs` to the remaining authorization lifetime.
  The runtime clamps it to `authorizationCacheTtlMs`. Throw from the callback
  when Connector authorization is denied, fails, or is revoked; the SDK
  forwards an explicit error without converting it to a successful empty
  response.
* **Cold resume**: Re-supply both the connected Connector endpoints and the
  header refresh callback when cold-resuming a session. Connector configuration
  and authorization are not recovered from persisted session state.

Returning no result from the callback reports that no dynamic headers are
available for the Connector endpoint. Existing static `headers`, arbitrary HTTP
servers configured through `mcpServers`, and MCP OAuth handlers continue to use
their current behavior.

## Configuration

### Node.js / TypeScript

```typescript
import { CopilotClient } from "@github/copilot-sdk";

const client = new CopilotClient();
const session = await client.createSession({
    model: "gpt-5",
    mcpServers: {
        // Local MCP server (stdio)
        "my-local-server": {
            type: "local",
            command: "node",
            args: ["./mcp-server.js"],
            env: { DEBUG: "true" },
            cwd: "./servers",
            tools: ["*"],  // "*" = all tools, [] = none, or list specific tools
            timeout: 30000,
        },
        // Remote MCP server (HTTP)
        "github": {
            type: "http",
            url: "https://api.githubcopilot.com/mcp/",
            headers: { "Authorization": "Bearer ${TOKEN}" },
            tools: ["*"],
        },
    },
});
```

### Python

```python
import asyncio
from copilot import CopilotClient
from copilot.session import PermissionHandler

async def main():
    client = CopilotClient()
    await client.start()

    session = await client.create_session(on_permission_request=PermissionHandler.approve_all, model="gpt-5", mcp_servers={
        # Local MCP server (stdio)
        "my-local-server": {
            "type": "local",
            "command": "python",
            "args": ["./mcp_server.py"],
            "env": {"DEBUG": "true"},
            "cwd": "./servers",
            "tools": ["*"],
            "timeout": 30000,
        },
        # Remote MCP server (HTTP)
        "github": {
            "type": "http",
            "url": "https://api.githubcopilot.com/mcp/",
            "headers": {"Authorization": "Bearer ${TOKEN}"},
            "tools": ["*"],
        },
    })

    response = await session.send_and_wait("List my recent GitHub notifications")
    print(response.data.content)

    await client.stop()

asyncio.run(main())
```

### Go

```go
package main

import (
    "context"
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

    session, err := client.CreateSession(ctx, &copilot.SessionConfig{
        Model: "gpt-5",
        MCPServers: map[string]copilot.MCPServerConfig{
            "my-local-server": copilot.MCPStdioServerConfig{
                Command: "node",
                Args:    []string{"./mcp-server.js"},
                Tools:   []string{"*"},
            },
        },
    })
    if err != nil {
        log.Fatal(err)
    }
    defer session.Disconnect()

    // Use the session...
}
```

### .NET

```csharp
using GitHub.Copilot;

await using var client = new CopilotClient();
await using var session = await client.CreateSessionAsync(new SessionConfig
{
    Model = "gpt-5",
    McpServers = new Dictionary<string, McpServerConfig>
    {
        ["my-local-server"] = new McpStdioServerConfig
        {
            Command = "node",
            Args = new List<string> { "./mcp-server.js" },
            Tools = new List<string> { "*" },
        },
    },
});
```

## Disabling configured servers per session

Set `disabledMcpServers` to exact MCP server names that must not run in a session.
The setting is scoped to the individual create or resume request; it does not
modify global MCP settings or the server configuration.

```typescript
const session = await client.createSession({
    mcpServers: {
        filesystem: { type: "local", command: "npx", args: ["-y", "@modelcontextprotocol/server-filesystem", "."] },
        github: { type: "http", url: "https://api.githubcopilot.com/mcp/" },
    },
    disabledMcpServers: ["github"],
});
```

| SDK | Configuration property |
| --- | --- |
| Node.js | `disabledMcpServers` |
| Python | `disabled_mcp_servers` |
| Go | `DisabledMCPServers` |
| .NET | `DisabledMcpServers` |
| Java | `setDisabledMcpServers(...)` |
| Rust | `with_disabled_mcp_servers(...)` |

On session creation and a **cold** resume, disabled servers are not started and
the runtime does not initiate their authentication. A resident resume cannot
undo a server that the runtime has already spawned. Names are matched exactly.

## Tool configuration

You can control which tools are available to an MCP server using the `tools` field.

### Allow all tools

Use `"*"` to enable all tools provided by the MCP server:

```typescript
tools: ["*"]
```

### Allow specific tools

Provide a list of tool names to restrict access:

```typescript
tools: ["bash", "edit"]
```

Only the listed tools will be available to the agent.

### Disable all tools

Use an empty array to disable all tools:

```typescript
tools: []
```

### Notes

* The `tools` field defines which tools are allowed.
* There is no separate `allow` or `disallow` configuration—tool access is controlled directly through this list.

## Quick start: filesystem MCP server

Here's a complete working example using the official [`@modelcontextprotocol/server-filesystem`](https://www.npmjs.com/package/@modelcontextprotocol/server-filesystem) MCP server:

```typescript
import { CopilotClient } from "@github/copilot-sdk";

async function main() {
    const client = new CopilotClient();

    // Create session with filesystem MCP server
    const session = await client.createSession({
        mcpServers: {
            filesystem: {
                type: "local",
                command: "npx",
                args: ["-y", "@modelcontextprotocol/server-filesystem", "/tmp"],
                tools: ["*"],
            },
        },
    });

    console.log("Session created:", session.sessionId);

    // The model can now use filesystem tools
    const result = await session.sendAndWait({
        prompt: "List the files in the allowed directory",
    });

    console.log("Response:", result?.data?.content);

    await session.disconnect();
    await client.stop();
}

main();
```

**Output:**
```
Session created: 18b3482b-bcba-40ba-9f02-ad2ac949a59a
Response: The allowed directory is `/tmp`, which contains various files
and subdirectories including temporary system files, log files, and
directories for different applications.
```

> [!TIP]
> You can use any MCP server from the [MCP Servers Directory](https://github.com/modelcontextprotocol/servers). Popular options include `@modelcontextprotocol/server-github`, `@modelcontextprotocol/server-sqlite`, and `@modelcontextprotocol/server-puppeteer`.

## Configuration options

### Local/stdio server

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `type` | `"local"` or `"stdio"` | No | Server type (defaults to local) |
| `command` | `string` | Yes | Command to execute |
| `args` | `string[]` | Yes | Command arguments |
| `env` | `object` | No | Environment variables |
| `cwd` | `string` | No | Working directory |
| `tools` | `string[]` | No | Tools to enable (`["*"]` for all, `[]` for none) |
| `timeout` | `number` | No | Timeout in milliseconds |

### Remote server (HTTP/SSE)

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `type` | `"http"` or `"sse"` | Yes | Server type |
| `url` | `string` | Yes | Server URL |
| `headers` | `object` | No | HTTP headers (e.g., for auth) |
| `tools` | `string[]` | No | Tools to enable |
| `timeout` | `number` | No | Timeout in milliseconds |

## Troubleshooting

### Tools not showing up or not being invoked

1. **Verify the MCP server starts correctly**
   * Check that the command and args are correct
   * Ensure the server process doesn't crash on startup
   * Look for error output in stderr

1. **Check tool configuration**
   * Make sure `tools` is set to `["*"]` or lists the specific tools you need
   * An empty array `[]` means no tools are enabled

1. **Verify connectivity for remote servers**
   * Ensure the URL is accessible
   * Check that authentication headers are correct

### Common issues

| Issue | Solution |
|-------|----------|
| "MCP server not found" | Verify the command path is correct and executable |
| "Connection refused" (HTTP) | Check the URL and ensure the server is running |
| "Timeout" errors | Increase the `timeout` value or check server performance |
| Tools work but aren't called | Ensure your prompt clearly requires the tool's functionality |

For detailed debugging guidance, see the **[MCP Debugging Guide](../troubleshooting/mcp-debugging.md)**.

## Related resources

* [Model Context Protocol Specification](https://modelcontextprotocol.io/)
* [MCP Servers Directory](https://github.com/modelcontextprotocol/servers) - Community MCP servers
* [GitHub MCP Server](https://github.com/github/github-mcp-server) - Official GitHub MCP server
* [Getting Started Guide](../getting-started.md) - SDK basics and custom tools
* [General Debugging Guide](../troubleshooting/debugging.md) - SDK-wide debugging

## See also

* [MCP Debugging Guide](../troubleshooting/mcp-debugging.md) - Detailed MCP troubleshooting
* [Issue #9](https://github.com/github/copilot-sdk/issues/9) - Original MCP tools usage question
* [Issue #36](https://github.com/github/copilot-sdk/issues/36) - MCP documentation tracking issue
