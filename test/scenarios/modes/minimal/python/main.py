import asyncio
import os
import copilot


async def main():
    client = copilot.cli_client(os.environ.get("COPILOT_CLI_PATH"), github_token=os.environ.get("GITHUB_TOKEN"))

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
