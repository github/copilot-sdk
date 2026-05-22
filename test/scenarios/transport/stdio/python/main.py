import asyncio
import os

from copilot import CopilotClient, CopilotClientOptions


async def main():
    client = CopilotClient(
        CopilotClientOptions(
            github_token=os.environ.get("GITHUB_TOKEN"),
        )
    )

    try:
        session = await client.create_session(model="claude-haiku-4.5")

        response = await session.send_and_wait("What is the capital of France?")

        if response:
            print(response.data.content)

        await session.disconnect()
    finally:
        await client.stop()


asyncio.run(main())
