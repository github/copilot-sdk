import asyncio

from copilot import CopilotClient


async def main():
    client = CopilotClient()

    try:
        session = await client.create_session(model="claude-haiku-4.5")

        response = await session.send_and_wait("What is the capital of France?")

        if response:
            print(response.data.content)

        await session.disconnect()
    finally:
        await client.stop()


asyncio.run(main())
