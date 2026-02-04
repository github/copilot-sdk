# Source: mcp/overview.md:60
import asyncio
from copilot import CopilotClient

async def main():
    client = CopilotClient()
    await client.start()

    session = await client.create_session({
        "model": "gpt-5",
        "mcp_servers": {
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
        },
    })

    response = await session.send_and_wait({
        "prompt": "List my recent GitHub notifications"
    })
    print(response.data.content)

    await client.stop()

asyncio.run(main())