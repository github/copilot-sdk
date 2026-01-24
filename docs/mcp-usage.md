# Using MCP servers with the Copilot SDK

This document shows how to configure MCP (Model Context Protocol) servers for sessions in each client SDK supported by this repository.

### What is MCP?
- MCP servers expose pre-built tools and resources (for example, GitHub MCP Server provides tools for issues, PRs, and repositories).
- You can configure local (stdio/local) or remote (HTTP/SSE) MCP servers and make them available to sessions.

### Common concepts
- `mcpServers` is the session-level configuration that maps server name to server configuration.
- Server config has two broad shapes:
  - **Local/stdio servers**: `type: "local" | "stdio"`, `command`, `args`, `env`, `cwd`
    - `type`: Defaults to `"local"` if not specified
    - `command`: The executable to run (e.g., `"node"`, `"python"`)
    - `args`: Arguments to pass to the command
    - `env`: Optional environment variables for the process
    - `cwd`: Optional working directory for the process
  - **Remote servers**: `type: "http" | "sse"`, `url`, `headers`
    - `type`: Either `"http"` or `"sse"` (Server-Sent Events)
    - `url`: The endpoint URL of the MCP server
    - `headers`: Optional HTTP headers for authentication
- `tools`: Array of tool names to expose from the server (use `["*"]` for all tools, `[]` for none)

### Examples

Below are examples for configuring both local and remote MCP servers:

<details>
<summary><strong>Node.js / TypeScript</strong></summary>

```ts
import { CopilotClient, type MCPLocalServerConfig, type MCPRemoteServerConfig } from "@github/copilot-sdk";

const client = new CopilotClient();

const session = await client.createSession({
  mcpServers: {
    "github": {
      type: "http",
      url: "https://api.githubcopilot.com/mcp/",
      tools: ["github-repos", "github-pull-requests"],
      headers: { Authorization: "Bearer <token>" },
    } as MCPRemoteServerConfig,
    "local-tools": {
      type: "local",
      command: "node",
      args: ["./mcp_server.js"],
      tools: ["*"],
      env: { "DEBUG": "1" },
    } as MCPLocalServerConfig,
  },
});

// Send a message and wait for completion
const response = await session.sendAndWait({ prompt: "Use the local-tools MCP Server to echo 'Hello from Copilot SDK!'." });

console.log(response?.data.content);

await client.stop();
process.exit(0);
```

</details>

<details>
<summary><strong>Python</strong></summary>

```python
import asyncio
from copilot import CopilotClient

async def main():
    client = CopilotClient()
    await client.start()

    session = await client.create_session({
        "mcp_servers": {
            "github": {
                "type": "http",
                "url": "https://api.githubcopilot.com/mcp/",
                "tools": ["github-repos", "github-pull-requests"],
                "headers": {"Authorization": "Bearer <token>"},
            },
            "local-tools": {
                "type": "local",
                "command": "python3",
                "args": ["./mcp_server.py"],
                "tools": ["*"],
                "env": {"DEBUG": "1"},
            }
        }
    })

    response = await session.send_and_wait({"prompt": "Use the local-tools MCP Server to echo 'Hello from Copilot SDK!'."})

    print(response.data.content)

    await session.destroy()
    await client.stop()

asyncio.run(main())
```

</details>


<details>
<summary><strong>Go</strong></summary>

```go
package main

import (
	"fmt"
	"log"

	copilot "github.com/github/copilot-sdk/go"
)

func main() {
	client := copilot.NewClient(nil)

	if err := client.Start(); err != nil {
		log.Fatal(err)
	}
	defer client.Stop()

	mcpServers := map[string]copilot.MCPServerConfig{
		"github": {
			"type":  "http",
			"url":   "https://api.githubcopilot.com/mcp/",
			"tools": []string{"github-repos", "github-pull-requests"},
			"headers": map[string]string{
				"Authorization": "Bearer <token>",
			},
		},
		"local-tools": {
			"type":    "local",
			"command": "go run",
			"args":    []string{"./mcp_server.go"},
			"tools":   []string{"*"},
			"env":     map[string]string{"DEBUG": "1"},
		},
	}

	session, err := client.CreateSession(&copilot.SessionConfig{
		MCPServers: mcpServers,
	})
	if err != nil {
		log.Fatalf("Failed to create session: %v", err)
	}
	defer session.Destroy()

	response, err := session.SendAndWait(copilot.MessageOptions{
		Prompt: "Use the local-tools MCP Server to echo 'Hello from Copilot SDK!'.",
	}, 0)
	if err != nil {
		log.Fatal(err)
	}

	fmt.Println(*response.Data.Content)
}
```

</details>

<details>
<summary><strong>.NET</strong></summary>

```csharp
using GitHub.Copilot.SDK;

await using var client = new CopilotClient();

var mcpServers = new Dictionary<string, object>
{
    ["github"] = new McpRemoteServerConfig
    {
        Type = "http",
        Url = "https://api.githubcopilot.com/mcp/",
        Tools = ["github-repos", "github-pull-requests"],
        Headers = new Dictionary<string, string>
        {
            ["Authorization"] = "Bearer <token>"
        }
    },
    ["local-tools"] = new McpLocalServerConfig
    {
        Type = "local",
        Command = "dotnet",
        Args = ["./mcp_server.dll"],
        Tools = ["*"],
        Env = new Dictionary<string, string> { ["DEBUG"] = "1" }
    }
};

await using var session = await client.CreateSessionAsync(new SessionConfig
{
    McpServers = mcpServers
});

var response = await session.SendAndWaitAsync(new MessageOptions { Prompt = "Use the local-tools MCP Server to echo 'Hello from Copilot SDK!'." });
Console.WriteLine(response?.Data.Content);
```

</details>

<br>

> Note that the **GitHub MCP server** is **already configured** as part of Copilot CLI and the SDK,
so configuring it explicitly is not necessary.


### Notes and tips

#### Configuration best practices:
- When adding remote MCP servers, prefer using service tokens or environment-based secrets rather than hard-coding tokens in samples.
- For local servers, document any required dependencies and how to run them (e.g., `npm install` then `node server.js`).
- Always call `session.destroy()` or `await session.DisposeAsync()` when finished with a session to clean up resources.


#### Session resumption with MCP servers:
- You can also configure or add MCP servers when resuming a session:


<details>
<summary><strong>Node.js / TypeScript</strong></summary>

```ts
const session = await client.resumeSession(sessionId, {
  mcpServers: { /* server configs */ }
});
```
</details>

<details>
<summary><strong>Python</strong></summary>

```py
session = await client.resume_session(session_id, {"mcp_servers": { """ <configs> """ }})
```
</details>

<details>
<summary><strong>Go</strong></summary>

```go
session, err := client.ResumeSessionWithOptions(sessionID, &copilot.ResumeSessionConfig{
    MCPServers: mcpServers,
})
```
</details>

<details>
<summary><strong>.NET</strong></summary>

```csharp
var session = await client.ResumeSessionAsync(sessionId, new ResumeSessionConfig
{
    McpServers = mcpServers
});
```
</details>


#### Additional resources:
- For more information on available MCP servers:
  - [GitHub MCP Server Documentation](https://github.com/github/github-mcp-server)
  - [MCP Servers Directory](https://github.com/modelcontextprotocol/servers) - Explore more MCP servers
- For troubleshooting local MCP servers, check that:
  - The command exists and is in the PATH
  - The process has appropriate permissions and environment variables
  - Arguments are correctly formatted for the command line

