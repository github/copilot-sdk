# SDK and CLI Compatibility

This document outlines which Copilot CLI features are available through the SDK and which are CLI-only.

## Overview

The Copilot SDK communicates with the CLI via JSON-RPC protocol. Features must be explicitly exposed through this protocol to be available in the SDK. Many interactive CLI features are terminal-specific and not available programmatically.

## Feature Comparison

### ✅ Available in SDK

| Feature | SDK Method | Notes |
|---------|------------|-------|
| **Session Management** | | |
| Create session | `createSession()` | Full config support |
| Resume session | `resumeSession()` | With infinite session workspaces |
| Disconnect session | `disconnect()` | Release in-memory resources |
| Destroy session *(deprecated)* | `destroy()` | Use `disconnect()` instead |
| Delete session | `deleteSession()` | Remove from storage |
| List sessions | `listSessions()` | All stored sessions |
| Get last session | `getLastSessionId()` | For quick resume |
| **Messaging** | | |
| Send message | `send()` | With attachments |
| Send and wait | `sendAndWait()` | Blocks until complete |
| Get history | `getMessages()` | All session events |
| Abort | `abort()` | Cancel in-flight request |
| **Tools** | | |
| Register custom tools | `registerTools()` | Full JSON Schema support |
| Tool permission control | `onPreToolUse` hook | Allow/deny/ask |
| Tool result modification | `onPostToolUse` hook | Transform results |
| Available/excluded tools | `availableTools`, `excludedTools` config | Filter tools |
| **Models** | | |
| List models | `listModels()` | With capabilities |
| Set model | `model` in session config | Per-session |
| Reasoning effort | `reasoningEffort` config | For supported models |
| **Authentication** | | |
| Get auth status | `getAuthStatus()` | Check login state |
| Use token | `githubToken` option | Programmatic auth |
| **MCP Servers** | | |
| Local/stdio servers | `mcpServers` config | Spawn processes |
| Remote HTTP/SSE | `mcpServers` config | Connect to services |
| **Hooks** | | |
| Pre-tool use | `onPreToolUse` | Permission, modify args |
| Post-tool use | `onPostToolUse` | Modify results |
| User prompt | `onUserPromptSubmitted` | Modify prompts |
| Session start/end | `onSessionStart`, `onSessionEnd` | Lifecycle |
| Error handling | `onErrorOccurred` | Custom handling |
| **Events** | | |
| All session events | `on()`, `once()` | 40+ event types |
| Streaming | `streaming: true` | Delta events |
| **Advanced** | | |
| Custom agents | `customAgents` config | Load agent definitions |
| System message | `systemMessage` config | Append or replace |
| Custom provider | `provider` config | BYOK support |
| Infinite sessions | `infiniteSessions` config | Auto-compaction |
| Permission handler | `onPermissionRequest` | Approve/deny requests |
| User input handler | `onUserInputRequest` | Handle ask_user |
| Skills | `skillDirectories` config | Custom skills |

### ❌ Not Available in SDK (CLI-Only)

| Feature | CLI Command/Option | Reason |
|---------|-------------------|--------|
| **Session Export** | | |
| Export to file | `--share`, `/share` | Not in protocol |
| Export to gist | `--share-gist`, `/share gist` | Not in protocol |
| **Interactive UI** | | |
| Slash commands | `/help`, `/clear`, etc. | TUI-only |
| Agent picker dialog | `/agent` | Interactive UI |
| Diff mode dialog | `/diff` | Interactive UI |
| Feedback dialog | `/feedback` | Interactive UI |
| Theme picker | `/theme` | Terminal UI |
| **Terminal Features** | | |
| Color output | `--no-color` | Terminal-specific |
| Screen reader mode | `--screen-reader` | Accessibility |
| Rich diff rendering | `--plain-diff` | Terminal rendering |
| Startup banner | `--banner` | Visual element |
| **Path/Permission Shortcuts** | | |
| Allow all paths | `--allow-all-paths` | Use permission handler |
| Allow all URLs | `--allow-all-urls` | Use permission handler |
| YOLO mode | `--yolo` | Use permission handler |
| **Directory Management** | | |
| Add directory | `/add-dir`, `--add-dir` | Configure in session |
| List directories | `/list-dirs` | TUI command |
| Change directory | `/cwd` | TUI command |
| **Plugin/MCP Management** | | |
| Plugin commands | `/plugin` | Interactive management |
| MCP server management | `/mcp` | Interactive UI |
| **Account Management** | | |
| Login flow | `/login`, `copilot auth login` | OAuth device flow |
| Logout | `/logout`, `copilot auth logout` | Direct CLI |
| User info | `/user` | TUI command |
| **Session Operations** | | |
| Clear conversation | `/clear` | TUI-only |
| Compact context | `/compact` | Use `infiniteSessions` config |
| Plan view | `/plan` | TUI-only |
| **Usage & Stats** | | |
| Token usage | `/usage` | Subscribe to usage events |
| **Code Review** | | |
| Review changes | `/review` | TUI command |
| **Delegation** | | |
| Delegate to PR | `/delegate` | TUI workflow |
| **Terminal Setup** | | |
| Shell integration | `/terminal-setup` | Shell-specific |
| **Experimental** | | |
| Toggle experimental | `/experimental` | Runtime flag |

## Workarounds

### Session Export

The `--share` option is not available via SDK. Workarounds:

1. **Collect events manually** - Subscribe to session events and build your own export:
   ```typescript
   const events: SessionEvent[] = [];
   session.on((event) => events.push(event));
   // ... after conversation ...
   const messages = await session.getMessages();
   // Format as markdown yourself
   ```

2. **Use CLI directly for export** - Run the CLI with `--share` for one-off exports.

### Permission Control

The SDK uses a **deny-by-default** permission model. All permission requests (file writes, shell commands, URL fetches, etc.) are denied unless your app provides an `onPermissionRequest` handler.

Instead of `--allow-all-paths` or `--yolo`, use the permission handler:

```typescript
const session = await client.createSession({
  onPermissionRequest: approveAll,
});
```

### Token Usage Tracking

Instead of `/usage`, subscribe to usage events:

```typescript
session.on("assistant.usage", (event) => {
  console.log("Tokens used:", {
    input: event.data.inputTokens,
    output: event.data.outputTokens,
  });
});
```

### Context Compaction

Instead of `/compact`, configure automatic compaction:

```typescript
const session = await client.createSession({
  infiniteSessions: {
    enabled: true,
    backgroundCompactionThreshold: 0.80,  // Start background compaction at 80% context utilization
    bufferExhaustionThreshold: 0.95,      // Block and compact at 95% context utilization
  },
});
```

> **Note:** Thresholds are context utilization ratios (0.0-1.0), not absolute token counts.

## Protocol Limitations

The SDK can only access features exposed through the CLI's JSON-RPC protocol. If you need a CLI feature that's not available:

1. **Check for alternatives** - Many features have SDK equivalents (see workarounds above)
2. **Use the CLI directly** - For one-off operations, invoke the CLI
3. **Request the feature** - Open an issue to request protocol support

## Version Compatibility

| SDK Version | Min Protocol | Max Protocol | Notes |
|-------------|-------------|--------------|-------|
| 0.1.x | 2 | 2 | Exact match required |
| 0.2.x+ | 2 | 3 | Range-based negotiation |

The SDK and CLI negotiate a compatible protocol version at startup. The SDK advertises a supported range (`minVersion`–`version`) and the CLI reports its version via `ping`. If the CLI's version falls within the SDK's range, the connection succeeds; otherwise, the SDK throws an error.

You can check versions at runtime:

```typescript
const status = await client.ping();
console.log("Server protocol version:", status.protocolVersion);
```

## Protocol v3: Broadcast Events and Multi-Client Sessions

Protocol v3 changes how the SDK handles tool calls and permission requests internally. **No user-facing API changes are required** — existing code continues to work.

### What Changed

| Aspect | Protocol v2 | Protocol v3 |
|--------|-------------|-------------|
| **Tool calls** | CLI sends RPC request directly to the SDK | CLI broadcasts `external_tool.requested` event to all connected clients |
| **Permission requests** | CLI sends RPC request directly to the SDK | CLI broadcasts `permission.requested` event to all connected clients |
| **Multi-client** | One SDK client per CLI server | Multiple SDK clients can share a CLI server and session |

### How It Works

In v3, the CLI broadcasts tool and permission events to every connected client. Each client checks whether it has a matching handler:

- If the client has a handler for the requested tool, it executes the tool and sends the result back via `session.tools.handlePendingToolCall`.
- If the client doesn't have the handler, it responds with an "unsupported" result.
- Permission requests follow the same pattern via `session.permissions.handlePendingPermissionRequest`.

The SDK handles all of this automatically — you register tools and permission handlers the same way as before:

```typescript
import { CopilotClient, defineTool } from "@github/copilot-sdk";

const myTool = defineTool("my_tool", {
    description: "A custom tool",
    parameters: { type: "object", properties: { input: { type: "string" } }, required: ["input"] },
    handler: async (args: { input: string }) => {
        return { result: args.input.toUpperCase() };
    },
});

// Works identically on both v2 and v3
const session = await client.createSession({
    tools: [myTool],
    onPermissionRequest: approveAll,
});
```

### Multi-Client Sessions

With v3, multiple SDK clients can connect to the same CLI server (via `cliUrl`) and share sessions. Each client can register different tools, and the broadcast model routes tool calls to the client that has the matching handler.

See the [Multi-Client Session Sharing](./guides/session-persistence.md#multi-client-session-sharing) section in the Session Persistence guide for details and code samples.

## Upgrading from v2 to v3

Upgrading is straightforward — no code changes required:

1. **Update the SDK package** to the latest version
2. **Update the CLI** to a version that supports protocol v3
3. **That's it** — the SDK auto-negotiates the protocol version

The SDK remains backward-compatible with v2 CLI servers. If the CLI only supports v2, the SDK operates in v2 mode automatically. Multi-client session features are only available when both the SDK and CLI use v3.

| Step | TypeScript | Python | Go | .NET |
|------|-----------|--------|-----|------|
| Update SDK | `npm install @github/copilot-sdk@latest` | `pip install --upgrade copilot-sdk` | `go get github.com/github/copilot-sdk/go@latest` | Update `PackageReference` version |
| Update CLI | `npm install @github/copilot@latest` | Bundled with SDK | External install | Bundled with SDK |

## See Also

- [Getting Started Guide](./getting-started.md)
- [Session Persistence & Multi-Client](./guides/session-persistence.md)
- [Hooks Documentation](./hooks/overview.md)
- [MCP Servers Guide](./mcp/overview.md)
- [Debugging Guide](./debugging.md)
