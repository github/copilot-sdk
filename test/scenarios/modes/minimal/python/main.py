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
        session = await client.create_session({
            "model": "claude-haiku-4.5",
            "available_tools": [],
            "system_message": {
                "mode": "replace",
                "content": "You have no tools. Respond with text only.",
            },
        })

        response = await session.send_and_wait("Use the grep tool to search for 'SDK' in README.md.")
        if response:
            print(f"Response: {response.data.content}")

        print("Minimal mode test complete")

        await session.disconnect()
    finally:
        await client.stop()


asyncio.run(main())
