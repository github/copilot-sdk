# MCP Servers

Configure and use Model Context Protocol (MCP) servers for extended capabilities.

> **Skill Level:** Advanced
>
> **Runnable Example:** [recipe/mcp_servers.py](recipe/mcp_servers.py)
>
> ```bash
> cd recipe && pip install -r requirements.txt
> python mcp_servers.py
> ```

## Overview

This recipe covers MCP server integration:

- Understanding MCP architecture
- GitHub MCP server configuration
- Filesystem MCP server setup
- Custom MCP servers
- Tool filtering and configuration

## What is MCP?

Model Context Protocol (MCP) is a standard for extending AI capabilities with external tools:

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   Copilot SDK   │────▶│   MCP Server    │────▶│  External API   │
│   (Your App)    │◀────│   (Bridge)      │◀────│  (GitHub, etc)  │
└─────────────────┘     └─────────────────┘     └─────────────────┘
```

MCP servers expose tools that Copilot can call automatically.

## Quick Start

```python
import asyncio
from copilot import CopilotClient, MCPServerConfig

async def main():
    # Configure GitHub MCP server
    github_mcp = MCPServerConfig(
        name="github",
        command="npx",
        args=["-y", "@modelcontextprotocol/server-github"],
        env={"GITHUB_TOKEN": os.environ["GITHUB_TOKEN"]}
    )

    client = CopilotClient()
    await client.start()

    session = await client.create_session({
        "mcp_servers": [github_mcp]
    })

    # Copilot now has access to GitHub tools!
    await session.send_and_wait({
        "prompt": "List the open issues in my-org/my-repo"
    })

    await session.destroy()
    await client.stop()

asyncio.run(main())
```

## GitHub MCP Server

Full configuration for GitHub operations:

```python
import os
from copilot import MCPServerConfig

def get_github_mcp_server():
    """Configure GitHub MCP server with token."""
    token = os.environ.get("GITHUB_TOKEN")
    if not token:
        raise ValueError("GITHUB_TOKEN environment variable required")

    return MCPServerConfig(
        name="github",
        command="npx",
        args=["-y", "@modelcontextprotocol/server-github"],
        env={
            "GITHUB_TOKEN": token
        }
    )
```

### GitHub Capabilities

The GitHub MCP server provides:

| Tool | Description |
|------|-------------|
| `list_issues` | List repository issues |
| `create_issue` | Create new issues |
| `list_pull_requests` | List PRs |
| `create_pull_request` | Create new PRs |
| `get_file_contents` | Read file from repo |
| `search_repositories` | Search GitHub |

### Usage Example

```python
await session.send_and_wait({
    "prompt": """
    For the repository 'owner/repo':
    1. List all open issues labeled 'bug'
    2. Show the 5 most recent pull requests
    3. Get the contents of README.md
    """
})
```

## Filesystem MCP Server

Access local files safely:

```python
def get_filesystem_mcp_server(allowed_paths):
    """Configure filesystem MCP server with allowed paths."""
    return MCPServerConfig(
        name="filesystem",
        command="npx",
        args=[
            "-y",
            "@modelcontextprotocol/server-filesystem",
            *allowed_paths  # Directories the server can access
        ]
    )


# Usage
fs_mcp = get_filesystem_mcp_server([
    "/home/user/projects",
    "/home/user/documents"
])

session = await client.create_session({
    "mcp_servers": [fs_mcp]
})
```

### Filesystem Capabilities

| Tool | Description |
|------|-------------|
| `read_file` | Read file contents |
| `write_file` | Write to files |
| `list_directory` | List directory contents |
| `create_directory` | Create directories |
| `move_file` | Move/rename files |
| `search_files` | Search by pattern |

## Multiple MCP Servers

Combine multiple MCP servers:

```python
async def multi_mcp_demo():
    """Use multiple MCP servers together."""

    github_mcp = get_github_mcp_server()
    filesystem_mcp = get_filesystem_mcp_server(["/home/user/projects"])

    session = await client.create_session({
        "mcp_servers": [github_mcp, filesystem_mcp]
    })

    # Copilot can use tools from both servers
    await session.send_and_wait({
        "prompt": """
        1. Get the README from github/owner/repo
        2. Save it to /home/user/projects/readme-backup.md
        """
    })
```

## Tool Filtering

Control which MCP tools are available:

```python
# Allow only specific tools
github_mcp = MCPServerConfig(
    name="github",
    command="npx",
    args=["-y", "@modelcontextprotocol/server-github"],
    env={"GITHUB_TOKEN": token},
    # Only expose read operations
    allowed_tools=["list_issues", "list_pull_requests", "get_file_contents"]
)

# Or block specific tools
github_mcp = MCPServerConfig(
    name="github",
    command="npx",
    args=["-y", "@modelcontextprotocol/server-github"],
    env={"GITHUB_TOKEN": token},
    # Block write operations
    blocked_tools=["create_issue", "create_pull_request", "delete_file"]
)
```

## Custom MCP Servers

Create your own MCP server:

```python
# Your custom MCP server (server.py)
from mcp import Server, Tool

server = Server("my-tools")

@server.tool("get_database_stats")
async def get_database_stats(database: str) -> dict:
    """Get statistics for a database."""
    return {"tables": 10, "rows": 1000}

# Configure in SDK
custom_mcp = MCPServerConfig(
    name="my-tools",
    command="python",
    args=["path/to/server.py"],
    env={"DATABASE_URL": os.environ["DATABASE_URL"]}
)
```

## Docker MCP Servers

Run MCP servers in containers:

```python
docker_mcp = MCPServerConfig(
    name="secure-tools",
    command="docker",
    args=[
        "run", "--rm", "-i",
        "-e", f"API_KEY={os.environ['API_KEY']}",
        "my-mcp-server:latest"
    ]
)
```

## Event Handling

Monitor MCP tool usage:

```python
def create_mcp_handler():
    """Track MCP tool execution."""
    def handler(event):
        if event.type == "tool.execution_start":
            tool_name = event.data.tool_name
            if tool_name.startswith("github."):
                print(f"🐙 GitHub: {tool_name}")
            elif tool_name.startswith("filesystem."):
                print(f"📁 Filesystem: {tool_name}")

        elif event.type == "tool.execution_complete":
            print(f"✅ Completed: {event.data.tool_call_id}")

        elif event.type == "tool.execution_error":
            print(f"❌ Error: {event.data.error}")

    return handler

session.on(create_mcp_handler())
```

## Error Handling

Handle MCP server issues:

```python
async def safe_mcp_session(client, mcp_servers):
    """Create session with MCP error handling."""
    try:
        session = await client.create_session({
            "mcp_servers": mcp_servers
        })
        return session

    except FileNotFoundError as e:
        print(f"MCP server not found: {e}")
        print("Try: npm install -g @modelcontextprotocol/server-github")
        raise

    except PermissionError as e:
        print(f"Permission denied: {e}")
        print("Check environment variables and file permissions")
        raise

    except TimeoutError:
        print("MCP server timed out during startup")
        raise
```

## Available MCP Servers

| Server | Package | Description |
|--------|---------|-------------|
| GitHub | `@modelcontextprotocol/server-github` | GitHub API |
| Filesystem | `@modelcontextprotocol/server-filesystem` | Local files |
| Slack | `@modelcontextprotocol/server-slack` | Slack API |
| PostgreSQL | `@modelcontextprotocol/server-postgres` | Database |
| Brave Search | `@modelcontextprotocol/server-brave-search` | Web search |

## Best Practices

1. **Secure credentials**: Use environment variables for tokens
2. **Limit access**: Use tool filtering for security
3. **Handle errors**: MCP servers can fail independently
4. **Monitor usage**: Log tool calls for debugging
5. **Test locally**: Verify MCP servers work before deploying

## Complete Example

```bash
# Set up environment
export GITHUB_TOKEN=ghp_...

python recipe/mcp_servers.py
```

Demonstrates:
- GitHub MCP server
- Filesystem MCP server
- Tool filtering
- Multiple servers

## Next Steps

- [Custom Tools](custom-tools.md): Combine MCP with custom tools
- [Custom Agents](custom-agents.md): Use MCP tools in agents
- [Error Handling](error-handling.md): Handle MCP errors
