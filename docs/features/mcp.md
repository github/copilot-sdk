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

## Server types

The SDK supports two types of MCP servers:

| Type | Description | Use Case |
|------|-------------|----------|
| **Local/Stdio** | Runs as a subprocess, communicates via stdin/stdout | Local tools, file access, custom scripts |
| **HTTP/SSE** | Remote server accessed via HTTP | Shared services, cloud-hosted tools |

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
| `oauthClientId` | `string` | No | Non-empty client ID for a statically configured OAuth client |
| `oauthScopes` | `string[]` | No | Non-empty array of valid RFC 6749 scope tokens. Requires `oauthClientId`; used when the server challenge omits scope or provides an empty scope, before protected-resource metadata fallback |
| `oauthPublicClient` | `boolean` | No | Whether the configured OAuth client is public and does not require a client secret |
| `oauthGrantType` | `string` | No | OAuth grant type for the configured client, such as `client_credentials` |
| `tools` | `string[]` | No | Tools to enable |
| `timeout` | `number` | No | Timeout in milliseconds |

## Session-scoped MCP diagnostics

Set `diagnostics: { sources: { mcp: { level: "debug" } } }` when you create or
resume a session to opt in to MCP diagnostics. Every source defaults to `off`.
The enabled levels are `error`, `warning`, `info`, `debug`, and `trace`. This
setting is separate from the client's process-level `logLevel`.

> [!WARNING]
> Diagnostic entries can contain user-provided or server-provided content at
> every enabled level: `warning` and above can include server stderr, while
> `debug` and `trace` can additionally include MCP payloads, tool arguments,
> and paths. Treat every entry as sensitive. Do not automatically upload entries
> as telemetry or export them without deliberate host action.

The generated `session.rpc.diagnostics` API lets hosts configure and read
session-scoped, bounded in-memory diagnostics. MCP is the first and currently
only supported source. Explicitly select it with `sources: ["mcp"]` when reading;
reading does not enable capture. Use one stoppable read loop
per output surface and retain its cursor. A read loop owns its cursor and
presentation; the runtime owns capture, filtering, retention, cursor expiry,
and wakeups.

```ts
import { approveAll, CopilotClient } from "@github/copilot-sdk";

const client = new CopilotClient();
const stop = new AbortController();
const appendLine = (line: string) => console.log(line);

await client.start();
const session = await client.createSession({
    onPermissionRequest: approveAll,
    diagnostics: { sources: { mcp: { level: "debug" } } },
});

try {
    let cursor: string | undefined;
    while (!stop.signal.aborted) {
        const page = await session.rpc.diagnostics.read({
            sources: ["mcp"],
            cursor,
            max: 100,
            waitMs: 30_000,
        });
        if (page.cursorStatus === "expired") {
            appendLine("MCP diagnostic entries were dropped.");
        }
        for (const entry of page.entries) {
            const detail = entry.details.data ? ` ${entry.details.data}` : "";
            appendLine(
                `${entry.timestamp} [${entry.level}] [${entry.source}:${entry.details.serverName}] ${entry.message}${detail}`,
            );
        }
        cursor = page.cursor;
    }
} finally {
    await session.disconnect();
    await client.stop();
}
```

The abort signal stops the loop after its current bounded read completes; it is
not passed to the RPC call. Disabling capture wakes an outstanding read.

MCP startup is lazy: enabling or reading diagnostics does not start a server.
Configure `mcpServers` on session creation, then either use the session normally
or call `session.rpc.mcp.startServer({ serverName })` for an installed server
to troubleshoot initialization without making a model request.

Each record has `source: "mcp"`, timestamp, severity, message, and optional
`agentId` for non-root agents. Its typed `details` contains the lifecycle,
protocol, HTTP, or stderr category, server name, a fresh `connectionId` for each
connection attempt, and optional protocol direction and diagnostic data.
MCP retention is capped at 1,024 records and 4 MiB per session. Individual encoded
records are capped at 16 KiB and carry `truncated` when shortened. Reads default
to 100 records, accept a maximum of 500, and do not consume other readers' data.
Use `droppedCount`, when present on an expired cursor, to show the known loss.
Every successful read returns a cursor and cursor status, including empty
reads. Keep the returned cursor even when no records arrive. A cursor belongs
to its source selection; start without a cursor when changing that selection.
Missing, empty, duplicate, or unsupported sources are rejected, as are malformed
cursors and invalid numeric bounds. Neither records nor capture settings are
saved in session history.

Call `session.rpc.diagnostics.configure({ sources: { mcp: { level: "off" } } })`
to disable MCP capture and clear its retained buffer. Configuration updates only
the explicitly named sources and returns the effective source levels. Empty or
unknown source configuration is rejected; adding another supported source in
the future will not implicitly opt an existing caller into it.

Stop the host's read loop too; reads while logging
is off return immediately rather than waiting. An outstanding long poll wakes
when logging is disabled. Omitting `diagnostics` during a resident resume
preserves its current configuration; a cold-loaded session requires a new opt-in.
Enabling diagnostics from the initial `off` state can begin capture for an
already-live MCP connection. After diagnostics have been disabled with
`configure({ sources: { mcp: { level: "off" } } })`, re-enabling can resume capture for the same
live connection and its existing connection ID. Records that were already in
flight when diagnostics were disabled are discarded.

The runtime removes configured and structurally recognized credentials from HTTP
diagnostics before entries reach this API, but this is not a guarantee that
arbitrary server-provided secrets are absent. It does not expose request or
response header maps. Only selected metadata, such as `Content-Type`, `Accept`,
and `MCP-Protocol-Version`, is eligible for capture, but its values are still
untrusted and can contain sensitive content. Authentication headers, cookies,
session IDs, query values, URL userinfo, and fragments are omitted. URL paths
are retained for troubleshooting, so do not place credentials in path segments.
Protocol payloads and stderr can also contain user content or arbitrary
server-provided secrets.

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
