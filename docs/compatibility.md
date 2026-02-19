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
| Destroy session | `destroy()` | Clean up resources |
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

| SDK Version | CLI Version | Protocol Version |
|-------------|-------------|------------------|
| Check `package.json` | `copilot --version` | `getStatus().protocolVersion` |

The SDK and CLI must have compatible protocol versions. The SDK will log warnings if versions are mismatched.

## See Also

- [Getting Started Guide](./getting-started.md)
- [Hooks Documentation](./hooks/overview.md)
- [MCP Servers Guide](./mcp/overview.md)
- [Debugging Guide](./debugging.md)
