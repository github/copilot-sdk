import asyncio
import os
import copilot


async def main():
    client = copilot.cli_client(os.environ.get("COPILOT_CLI_PATH"), github_token=os.environ.get("GITHUB_TOKEN"))

    try:
        # MCP server config — demonstrates the configuration pattern.
        # When MCP_SERVER_CMD is set, connects to a real MCP server.
        # Otherwise, runs without MCP tools as a build/integration test.
        mcp_servers = {}
        if os.environ.get("MCP_SERVER_CMD"):
            args = os.environ.get("MCP_SERVER_ARGS", "").split() if os.environ.get("MCP_SERVER_ARGS") else []
            mcp_servers["example"] = {
                "type": "stdio",
                "command": os.environ["MCP_SERVER_CMD"],
                "args": args,
            }

        session_config = {
            "model": "claude-haiku-4.5",
            "available_tools": [],
            "system_message": {
                "mode": "replace",
                "content": "You are a helpful assistant. Answer questions concisely.",
            },
        }
        if mcp_servers:
            session_config["mcp_servers"] = mcp_servers

        session = await client.create_session(session_config)

        response = await session.send_and_wait(
            {"prompt": "What is the capital of France?"}
        )

        if response:
            print(response.data.content)

        if mcp_servers:
            print(f"\nMCP servers configured: {', '.join(mcp_servers.keys())}")
        else:
            print("\nNo MCP servers configured (set MCP_SERVER_CMD to test with a real server)")

        await session.destroy()
    finally:
        await client.stop()


asyncio.run(main())
