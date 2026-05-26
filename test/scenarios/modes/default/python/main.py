import asyncio

from copilot import CopilotClient


async def main():
    client = CopilotClient()

    try:
        session = await client.create_session(model="claude-haiku-4.5")

        response = await session.send_and_wait(
            "Use the grep tool to search for the word 'SDK' in README.md and show the matching lines."
        )
        if response:
            print(f"Response: {response.data.content}")

        print("Default mode test complete")

        await session.disconnect()
    finally:
        await client.stop()


asyncio.run(main())
