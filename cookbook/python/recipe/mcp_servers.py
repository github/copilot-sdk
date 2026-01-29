#!/usr/bin/env python3
"""
MCP Servers - Integrating Model Context Protocol servers with Copilot.
Run: python mcp_servers.py
"""

import asyncio
import os

from copilot import CopilotClient
from copilot.types import MCPLocalServerConfig, MCPRemoteServerConfig, SessionEventType


# =============================================================================
# MCP Server Configurations
# =============================================================================


def get_filesystem_mcp_server():
    """Configure the official filesystem MCP server."""
    allowed_dir = os.path.expanduser("~/Documents")
    return MCPLocalServerConfig(
        type="stdio",
        command="npx",
        args=["-y", "@modelcontextprotocol/server-filesystem", allowed_dir],
        tools=["*"],
    )


def get_github_mcp_server():
    """Configure the GitHub MCP server. Requires GITHUB_TOKEN."""
    github_token = os.environ.get("GITHUB_TOKEN", "")
    return MCPLocalServerConfig(
        type="stdio",
        command="npx",
        args=["-y", "@modelcontextprotocol/server-github"],
        env={"GITHUB_TOKEN": github_token},
        tools=["*"],
        timeout=30000,
    )


def get_sqlite_mcp_server(db_path):
    """Configure the SQLite MCP server."""
    return MCPLocalServerConfig(
        type="stdio",
        command="npx",
        args=["-y", "@modelcontextprotocol/server-sqlite", db_path],
        tools=["query", "list_tables", "describe_table"],
    )


def get_remote_mcp_server():
    """Configure a remote HTTP/SSE MCP server."""
    return MCPRemoteServerConfig(
        type="sse",
        url=os.environ.get("MCP_SERVER_URL", "http://localhost:3001/mcp"),
        headers={"Authorization": f"Bearer {os.environ.get('MCP_SERVER_TOKEN', '')}"},
        tools=["*"],
        timeout=10000,
    )


def get_custom_mcp_server(command, args, tools=None):
    """Configure a custom MCP server."""
    return MCPLocalServerConfig(
        type="stdio",
        command=command,
        args=args,
        tools=tools or ["*"],
    )


# =============================================================================
# Event Handler
# =============================================================================


def create_mcp_event_handler(verbose=True):
    """Create an event handler that highlights MCP tool usage."""

    def handler(event):
        if event.type == SessionEventType.ASSISTANT_MESSAGE:
            print(f"\n🤖 {event.data.content}\n")
        elif event.type == SessionEventType.TOOL_EXECUTION_START:
            tool_name = event.data.tool_name
            prefix = "🔌 MCP Tool:" if tool_name.startswith("mcp_") else "⚙️  Tool:"
            print(f"  {prefix} {tool_name}")
        elif event.type == SessionEventType.TOOL_EXECUTION_COMPLETE:
            print("  ✓ Done")
        elif event.type == SessionEventType.SESSION_ERROR:
            message = getattr(event.data, "message", str(event.data))
            print(f"  ❌ Error: {message}")

    return handler


# =============================================================================
# Demo: GitHub MCP
# =============================================================================


async def demo_github_mcp():
    """Demonstrate using the GitHub MCP server. Requires GITHUB_TOKEN."""
    print("\n=== GitHub MCP Server Demo ===\n")

    if not os.environ.get("GITHUB_TOKEN"):
        print("⚠️ GITHUB_TOKEN not set. Skipping GitHub MCP demo.")
        return

    client = CopilotClient()

    try:
        await client.start()

        # Create session with GitHub MCP server
        session = await client.create_session(
            {
                "mcp_servers": {
                    "github": get_github_mcp_server(),
                },
            }
        )

        session.on(create_mcp_event_handler())

        print("Using GitHub MCP server to analyze a repository...\n")

        await session.send_and_wait(
            {
                "prompt": """
Search for the most popular Python repositories on GitHub.
Show me the top 3 by stars, with their descriptions.
"""
            },
            timeout=120.0,
        )

        await session.destroy()

    except Exception as e:
        print(f"Error: {e}")

    finally:
        await client.stop()


# =============================================================================
# Demo: Filesystem MCP
# =============================================================================


async def demo_filesystem_mcp():
    """Demonstrate using the filesystem MCP server."""
    print("\n=== Filesystem MCP Server Demo ===\n")

    # Create a test directory
    test_dir = os.path.expanduser("~/copilot-mcp-test")
    os.makedirs(test_dir, exist_ok=True)

    # Create a test file
    test_file = os.path.join(test_dir, "sample.txt")
    with open(test_file, "w") as f:
        f.write("Hello from the MCP demo!\nThis is a test file.")

    print(f"Test directory: {test_dir}")
    print(f"Test file: {test_file}\n")

    client = CopilotClient()

    try:
        await client.start()

        # Configure filesystem server for the test directory
        fs_server = MCPLocalServerConfig(
            type="stdio",
            command="npx",
            args=["-y", "@modelcontextprotocol/server-filesystem", test_dir],
            tools=["*"],
        )

        session = await client.create_session(
            {
                "mcp_servers": {
                    "filesystem": fs_server,
                },
            }
        )

        session.on(create_mcp_event_handler())

        print("Using filesystem MCP server to explore files...\n")

        await session.send_and_wait(
            {
                "prompt": f"""
List the files in {test_dir} and read the content of sample.txt.
Then create a new file called 'created_by_copilot.txt' with a greeting message.
"""
            },
            timeout=120.0,
        )

        await session.destroy()

    except Exception as e:
        print(f"Error: {e}")
        print("Note: Make sure the filesystem MCP server is installed:")
        print("  npx -y @modelcontextprotocol/server-filesystem --help")

    finally:
        await client.stop()

        # Cleanup
        import shutil

        if os.path.exists(test_dir):
            shutil.rmtree(test_dir)
            print(f"\nCleaned up test directory: {test_dir}")


# =============================================================================
# Demo: Multiple MCP Servers
# =============================================================================


async def demo_multiple_mcp_servers():
    """Demonstrate using multiple MCP servers together."""
    print("\n=== Multiple MCP Servers Demo ===\n")

    # Create test directory for filesystem server
    test_dir = os.path.expanduser("~/copilot-mcp-multi-test")
    os.makedirs(test_dir, exist_ok=True)

    client = CopilotClient()

    try:
        await client.start()

        # Configure multiple MCP servers
        mcp_servers = {
            "filesystem": MCPLocalServerConfig(
                type="stdio",
                command="npx",
                args=["-y", "@modelcontextprotocol/server-filesystem", test_dir],
                tools=["*"],
            ),
        }

        # Add GitHub if token is available
        if os.environ.get("GITHUB_TOKEN"):
            mcp_servers["github"] = get_github_mcp_server()

        print(f"Configured {len(mcp_servers)} MCP server(s): {list(mcp_servers.keys())}")

        session = await client.create_session(
            {
                "mcp_servers": mcp_servers,
            }
        )

        session.on(create_mcp_event_handler())

        # Create a prompt that uses multiple servers
        prompt = f"List files in {test_dir} using the filesystem tools."

        if "github" in mcp_servers:
            prompt += " Also, show me 1 trending Python repository from GitHub."

        await session.send_and_wait({"prompt": prompt}, timeout=120.0)

        await session.destroy()

    except Exception as e:
        print(f"Error: {e}")

    finally:
        await client.stop()

        # Cleanup
        import shutil

        if os.path.exists(test_dir):
            shutil.rmtree(test_dir)


# =============================================================================
# Demo: Tool Filtering
# =============================================================================


async def demo_tool_filtering():
    """Demonstrate filtering which MCP tools are available."""
    print("\n=== MCP Tool Filtering Demo ===\n")

    print("Tool filtering options:")
    print("  ['*']             - All tools")
    print("  ['tool1', 'tool2'] - Only specific tools")
    print("  []                - No tools (disabled)")

    # Example: read-only filesystem
    read_only_config = MCPLocalServerConfig(
        type="stdio",
        command="npx",
        args=["-y", "@modelcontextprotocol/server-filesystem", os.getcwd()],
        tools=["read_file", "list_directory", "get_file_info"],
    )

    print(f"\nConfigured read-only filesystem: {read_only_config.get('tools')}")
    print("✓ Tool filtering configured")


# =============================================================================
# MCP Guide
# =============================================================================


def print_mcp_guide():
    """Print a quick guide for MCP servers."""
    print("""
MCP (Model Context Protocol) GUIDE

Common MCP Servers:
  @modelcontextprotocol/server-filesystem  - File operations
  @modelcontextprotocol/server-github      - GitHub API
  @modelcontextprotocol/server-sqlite      - SQLite queries
  @modelcontextprotocol/server-slack       - Slack integration

Install: npx -y @modelcontextprotocol/server-<name> --help
Docs: https://modelcontextprotocol.io/docs/servers/building
""")


# =============================================================================
# Main
# =============================================================================


async def main():
    """Run MCP demonstrations."""
    print("=" * 60)
    print("MCP Servers")
    print("=" * 60)

    print_mcp_guide()

    print("Environment:")
    print(f"  GITHUB_TOKEN: {'✓' if os.environ.get('GITHUB_TOKEN') else '✗'}")

    await demo_tool_filtering()
    await demo_filesystem_mcp()
    await demo_github_mcp()
    await demo_multiple_mcp_servers()

    print("\n" + "=" * 60)
    print("All demos completed!")
    print("=" * 60)


if __name__ == "__main__":
    asyncio.run(main())
