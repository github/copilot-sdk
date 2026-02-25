import asyncio
import os
import copilot


async def main():
    client = copilot.cli_client(os.environ.get("COPILOT_CLI_PATH"), github_token=os.environ.get("GITHUB_TOKEN"))

    try:
        session = await client.create_session({
            "model": "claude-haiku-4.5",
        })

        response = await session.send_and_wait({"prompt": "Use the grep tool to search for the word 'SDK' in README.md and show the matching lines."})
        if response:
            print(f"Response: {response.data.content}")

        print("Default mode test complete")

        await session.destroy()
    finally:
        await client.stop()


asyncio.run(main())
