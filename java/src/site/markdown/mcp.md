# MCP Servers

Extend the AI's capabilities with external tools using the [Model Context Protocol](https://modelcontextprotocol.io/).

---

## Quick Start

Give the AI access to your filesystem.

```java
Map<String, Object> server = Map.of(
    "type", "local",
    "command", "npx",
    "args", List.of("-y", "@modelcontextprotocol/server-filesystem", "/tmp"),
    "tools", List.of("*")
);

var session = client.createSession(
    new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
        .setMcpServers(Map.of("filesystem", server))
).get();

var result = session.sendAndWait("List files in the directory").get();
System.out.println(result.getData().content());
```

> **Tip:** Browse the [MCP Servers Directory](https://github.com/modelcontextprotocol/servers) for community servers like GitHub, SQLite, and Puppeteer.

---

## Local Servers

Run a tool as a subprocess (stdin/stdout communication).

```java
var server = new HashMap<String, Object>();
server.put("type", "local");
server.put("command", "node");
server.put("args", List.of("./mcp-server.js"));
server.put("env", Map.of("DEBUG", "true"));
server.put("cwd", "./servers");
server.put("tools", List.of("*"));
```

| Option | Description |
|--------|-------------|
| `command` | Executable to run |
| `args` | Command-line arguments |
| `env` | Environment variables |
| `cwd` | Working directory |
| `tools` | `["*"]` for all, `[]` for none, or specific tool names |
| `timeout` | Timeout in milliseconds |

---

## Remote Servers

Connect to a cloud-hosted MCP server via HTTP.

```java
Map<String, Object> server = Map.of(
    "type", "http",
    "url", "https://api.githubcopilot.com/mcp/",
    "headers", Map.of("Authorization", "Bearer " + token),
    "tools", List.of("*")
);
```

| Option | Description |
|--------|-------------|
| `type` | `"http"` or `"sse"` |
| `url` | Server endpoint |
| `headers` | HTTP headers for authentication |
| `tools` | Tools to enable |
| `timeout` | Timeout in milliseconds |

---

## Multiple Servers

Combine tools from several sources.

```java
var session = client.createSession(
    new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
        .setMcpServers(Map.of(
            "filesystem", filesystemServer,
            "github", githubServer,
            "database", sqliteServer
        ))
).get();
```

---

## Troubleshooting

| Problem | Solution |
|---------|----------|
| Tools not invoked | Set `tools` to `["*"]` or list specific tool names |
| Server not found | Verify `command` path is correct and executable |
| Connection refused | Check URL and authentication headers |
| Timeout errors | Increase `timeout` or check server performance |

**Debug tips:**
- Test your MCP server independently before integrating
- Enable verbose logging in the server
- Start with a simple tool to verify the integration

---

## Resources

- [MCP Specification](https://modelcontextprotocol.io/)
- [MCP Servers Directory](https://github.com/modelcontextprotocol/servers)
- [GitHub MCP Server](https://github.com/github/github-mcp-server)

---

## Next Steps

- 📖 **[Documentation](documentation.html)** - Core concepts, events, session management
- 📖 **[Advanced Usage](advanced.html)** - Tools, BYOK, System Messages, Custom Agents
- 📖 **[Session Hooks](hooks.html)** - Intercept tool execution and session lifecycle events
- 📖 **[API Javadoc](apidocs/index.html)** - Complete API reference
