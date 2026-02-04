# MCP Server Debugging Guide

This guide covers debugging techniques specific to MCP (Model Context Protocol) servers when using the Copilot SDK.

## Table of Contents

- [Quick Diagnostics](#quick-diagnostics)
- [Testing MCP Servers Independently](#testing-mcp-servers-independently)
- [Common Issues](#common-issues)
- [Platform-Specific Issues](#platform-specific-issues)
- [Advanced Debugging](#advanced-debugging)

---

## Quick Diagnostics

### Checklist

Before diving deep, verify these basics:

- [ ] MCP server executable exists and is runnable
- [ ] Command path is correct (use absolute paths when in doubt)
- [ ] Tools are enabled (`tools: ["*"]` or specific tool names)
- [ ] Server implements MCP protocol correctly (responds to `initialize`)
- [ ] No firewall/antivirus blocking the process (Windows)

### Enable MCP Debug Logging

Add environment variables to your MCP server config:

```typescript
mcpServers: {
  "my-server": {
    type: "local",
    command: "/path/to/server",
    args: [],
    env: {
      MCP_DEBUG: "1",
      DEBUG: "*",
      NODE_DEBUG: "mcp",  // For Node.js MCP servers
    },
  },
}
```

---

## Testing MCP Servers Independently

Always test your MCP server outside the SDK first.

### Manual Protocol Test

Send an `initialize` request via stdin:

```bash
# Unix/macOS
echo '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"test","version":"1.0"}}}' | /path/to/your/mcp-server

# Windows (PowerShell)
'{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"test","version":"1.0"}}}' | C:\path\to\your\mcp-server.exe
```

**Expected response:**
```json
{"jsonrpc":"2.0","id":1,"result":{"protocolVersion":"2024-11-05","capabilities":{"tools":{}},"serverInfo":{"name":"your-server","version":"1.0"}}}
```

### Test Tool Listing

After initialization, request the tools list:

```bash
echo '{"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}' | /path/to/your/mcp-server
```

**Expected response:**
```json
{"jsonrpc":"2.0","id":2,"result":{"tools":[{"name":"my_tool","description":"Does something","inputSchema":{...}}]}}
```

### Interactive Testing Script

Create a test script to interactively debug your MCP server:

```bash
#!/bin/bash
# test-mcp.sh

SERVER="$1"

# Initialize
echo '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"test","version":"1.0"}}}'

# Send initialized notification
echo '{"jsonrpc":"2.0","method":"notifications/initialized"}'

# List tools
echo '{"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}'

# Keep stdin open
cat
```

Usage:
```bash
./test-mcp.sh | /path/to/mcp-server
```

---

## Common Issues

### Server Not Starting

**Symptoms:** No tools appear, no errors in logs.

**Causes & Solutions:**

| Cause | Solution |
|-------|----------|
| Wrong command path | Use absolute path: `/usr/local/bin/server` |
| Missing executable permission | Run `chmod +x /path/to/server` |
| Missing dependencies | Check with `ldd` (Linux) or run manually |
| Working directory issues | Set `cwd` in config |

**Debug by running manually:**
```bash
# Run exactly what the SDK would run
cd /expected/working/dir
/path/to/command arg1 arg2
```

### Server Starts But Tools Don't Appear

**Symptoms:** Server process runs but no tools are available.

**Causes & Solutions:**

1. **Tools not enabled in config:**
   ```typescript
   mcpServers: {
     "server": {
       // ...
       tools: ["*"],  // Must be "*" or list of tool names
     },
   }
   ```

2. **Server doesn't expose tools:**
   - Test with `tools/list` request manually
   - Check server implements `tools/list` method

3. **Initialization handshake fails:**
   - Server must respond to `initialize` correctly
   - Server must handle `notifications/initialized`

### Tools Listed But Never Called

**Symptoms:** Tools appear in debug logs but model doesn't use them.

**Causes & Solutions:**

1. **Prompt doesn't clearly need the tool:**
   ```typescript
   // Too vague
   await session.sendAndWait({ prompt: "What's the weather?" });
   
   // Better - explicitly mentions capability
   await session.sendAndWait({ 
     prompt: "Use the weather tool to get the current temperature in Seattle" 
   });
   ```

2. **Tool description unclear:**
   ```typescript
   // Bad - model doesn't know when to use it
   { name: "do_thing", description: "Does a thing" }
   
   // Good - clear purpose
   { name: "get_weather", description: "Get current weather conditions for a city. Returns temperature, humidity, and conditions." }
   ```

3. **Tool schema issues:**
   - Ensure `inputSchema` is valid JSON Schema
   - Required fields must be in `required` array

### Timeout Errors

**Symptoms:** `MCP tool call timed out` errors.

**Solutions:**

1. **Increase timeout:**
   ```typescript
   mcpServers: {
     "slow-server": {
       // ...
       timeout: 300000,  // 5 minutes
     },
   }
   ```

2. **Optimize server performance:**
   - Add progress logging to identify bottleneck
   - Consider async operations
   - Check for blocking I/O

3. **For long-running tools**, consider streaming responses if supported.

### JSON-RPC Errors

**Symptoms:** Parse errors, invalid request errors.

**Common causes:**

1. **Server writes to stdout incorrectly:**
   - Debug output going to stdout instead of stderr
   - Extra newlines or whitespace
   
   ```typescript
   // Wrong - pollutes stdout
   console.log("Debug info");
   
   // Correct - use stderr for debug
   console.error("Debug info");
   ```

2. **Encoding issues:**
   - Ensure UTF-8 encoding
   - No BOM (Byte Order Mark)

3. **Message framing:**
   - Each message must be a complete JSON object
   - Newline-delimited (one message per line)

---

## Platform-Specific Issues

### Windows

#### .NET Console Apps / Tools

```csharp
// Correct configuration for .NET exe
["my-dotnet-server"] = new McpLocalServerConfig
{
    Type = "local",
    Command = @"C:\Tools\MyServer\MyServer.exe",  // Full path with .exe
    Args = new List<string>(),
    Cwd = @"C:\Tools\MyServer",  // Set working directory
    Tools = new List<string> { "*" },
}

// For dotnet tool (DLL)
["my-dotnet-tool"] = new McpLocalServerConfig
{
    Type = "local", 
    Command = "dotnet",
    Args = new List<string> { @"C:\Tools\MyTool\MyTool.dll" },
    Cwd = @"C:\Tools\MyTool",
    Tools = new List<string> { "*" },
}
```

#### NPX Commands

```csharp
// Windows needs cmd /c for npx
["filesystem"] = new McpLocalServerConfig
{
    Type = "local",
    Command = "cmd",
    Args = new List<string> { "/c", "npx", "-y", "@modelcontextprotocol/server-filesystem", "C:\\allowed\\path" },
    Tools = new List<string> { "*" },
}
```

#### Path Issues

- Use raw strings (`@"C:\path"`) or forward slashes (`"C:/path"`)
- Avoid spaces in paths when possible
- If spaces required, ensure proper quoting

#### Antivirus/Firewall

Windows Defender or other AV may block:
- New executables
- Processes communicating via stdin/stdout

**Solution:** Add exclusions for your MCP server executable.

### macOS

#### Gatekeeper Blocking

```bash
# If server is blocked
xattr -d com.apple.quarantine /path/to/mcp-server
```

#### Homebrew Paths

```typescript
// GUI apps may not have /opt/homebrew in PATH
mcpServers: {
  "my-server": {
    command: "/opt/homebrew/bin/node",  // Full path
    args: ["/path/to/server.js"],
  },
}
```

### Linux

#### Permission Issues

```bash
chmod +x /path/to/mcp-server
```

#### Missing Shared Libraries

```bash
# Check dependencies
ldd /path/to/mcp-server

# Install missing libraries
apt install libfoo  # Debian/Ubuntu
yum install libfoo  # RHEL/CentOS
```

---

## Advanced Debugging

### Capture All MCP Traffic

Create a wrapper script to log all communication:

```bash
#!/bin/bash
# mcp-debug-wrapper.sh

LOG="/tmp/mcp-debug-$(date +%s).log"
ACTUAL_SERVER="$1"
shift

echo "=== MCP Debug Session ===" >> "$LOG"
echo "Server: $ACTUAL_SERVER" >> "$LOG"
echo "Args: $@" >> "$LOG"
echo "=========================" >> "$LOG"

# Tee stdin/stdout to log file
tee -a "$LOG" | "$ACTUAL_SERVER" "$@" 2>> "$LOG" | tee -a "$LOG"
```

Use it:
```typescript
mcpServers: {
  "debug-server": {
    command: "/path/to/mcp-debug-wrapper.sh",
    args: ["/actual/server/path", "arg1", "arg2"],
  },
}
```

### Inspect with MCP Inspector

Use the official MCP Inspector tool:

```bash
npx @modelcontextprotocol/inspector /path/to/your/mcp-server
```

This provides a web UI to:
- Send test requests
- View responses
- Inspect tool schemas

### Protocol Version Mismatches

Check your server supports the protocol version the SDK uses:

```json
// In initialize response, check protocolVersion
{"result":{"protocolVersion":"2024-11-05",...}}
```

If versions don't match, update your MCP server library.

---

## Debugging Checklist

When opening an issue or asking for help, collect:

- [ ] SDK language and version
- [ ] CLI version (`copilot --version`)
- [ ] MCP server type (Node.js, Python, .NET, Go, etc.)
- [ ] Full MCP server configuration (redact secrets)
- [ ] Result of manual `initialize` test
- [ ] Result of manual `tools/list` test  
- [ ] Debug logs from SDK
- [ ] Any error messages

## See Also

- [MCP Overview](./overview.md) - Configuration and setup
- [General Debugging Guide](../debugging.md) - SDK-wide debugging
- [MCP Specification](https://modelcontextprotocol.io/) - Official protocol docs
