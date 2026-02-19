import asyncio
import os
from copilot import CopilotClient


async def main():
    opts = {"github_token": os.environ.get("GITHUB_TOKEN")}
    if os.environ.get("COPILOT_CLI_PATH"):
        opts["cli_path"] = os.environ["COPILOT_CLI_PATH"]
    client = CopilotClient(opts)

    try:
        session = await client.create_session({
            "model": "claude-haiku-4.5",
            "available_tools": [],
            "system_message": {
                "mode": "replace",
                "content": "You have no tools. Respond with text only.",
            },
        })

        response = await session.send_and_wait({"prompt": "Use the grep tool to search for 'SDK' in README.md."})
        if response:
            print(f"Response: {response.data.content}")

        print("Minimal mode test complete")

        await session.destroy()
    finally:
        await client.stop()


asyncio.run(main())
