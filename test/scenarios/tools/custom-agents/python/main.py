import asyncio
import os
from copilot import CopilotClient
from copilot.client import SubprocessConfig


async def main():
    client = CopilotClient(SubprocessConfig(
        github_token=os.environ.get("GITHUB_TOKEN"),
        cli_path=os.environ.get("COPILOT_CLI_PATH"),
    ))

    try:
        session = await client.create_session(
            {
                "model": "claude-haiku-4.5",
                "custom_agents": [
                    {
                        "name": "researcher",
                        "display_name": "Research Agent",
                        "description": "A research agent that can only read and search files, not modify them",
                        "tools": ["grep", "glob", "view"],
                        "prompt": "You are a research assistant. You can search and read files but cannot modify anything. When asked about your capabilities, list the tools you have access to.",
                    },
                ],
            }
        )

        response = await session.send_and_wait(
            "What custom agents are available? Describe the researcher agent and its capabilities."
        )

        if response:
            print(response.data.content)

        await session.disconnect()
    finally:
        await client.stop()


asyncio.run(main())
