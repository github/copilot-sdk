# Config Sample: MCP Servers

Demonstrates configuring the Copilot SDK with **MCP (Model Context Protocol) server** integration. This validates that the SDK correctly passes `mcpServers` configuration to the runtime for connecting to external tool providers via stdio.

## What Each Sample Does

1. Checks for `MCP_SERVER_CMD` environment variable
2. If set, configures an MCP server entry of type `stdio` in the session config
3. Creates a session with `availableTools: []` and optionally `mcpServers`
4. Sends: _"What is the capital of France?"_ as a fallback test prompt
5. Prints the response and whether MCP servers were configured

## Configuration

| Option | Value | Effect |
|--------|-------|--------|
| `mcpServers` | Map of server configs | Connects to external MCP servers that expose tools |
| `mcpServers.*.type` | `"stdio"` | Communicates with the MCP server via stdin/stdout |
| `mcpServers.*.command` | Executable path | The MCP server binary to spawn |
| `mcpServers.*.args` | String array | Arguments passed to the MCP server |
| `availableTools` | `[]` (empty array) | No built-in tools; MCP tools used if available |

## Environment Variables

| Variable | Required | Description |
|----------|----------|-------------|
| `COPILOT_CLI_PATH` | No | Path to `copilot` binary (auto-detected) |
| `GITHUB_TOKEN` | Yes | GitHub auth token (falls back to `gh auth token`) |
| `MCP_SERVER_CMD` | No | MCP server executable — when set, enables MCP integration |
| `MCP_SERVER_ARGS` | No | Space-separated arguments for the MCP server command |

## Run

```bash
# Without MCP server (build + basic integration test)
./verify.sh

# With a real MCP server
MCP_SERVER_CMD=npx MCP_SERVER_ARGS="@modelcontextprotocol/server-filesystem /tmp" ./verify.sh
```

Requires the `copilot` binary (auto-detected or set `COPILOT_CLI_PATH`) and `GITHUB_TOKEN`.
