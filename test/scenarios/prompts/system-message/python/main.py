import asyncio

from copilot import CopilotClient

PIRATE_PROMPT = """You are a pirate. Always respond in pirate speak. Say 'Arrr!' in every response. Use nautical terms and pirate slang throughout."""


async def main():
    client = CopilotClient()

    try:
        session = await client.create_session(
            model="claude-haiku-4.5",
            system_message={"mode": "replace", "content": PIRATE_PROMPT},
            available_tools=[],
        )

        response = await session.send_and_wait("What is the capital of France?")

        if response:
            print(response.data.content)

        await session.disconnect()
    finally:
        await client.stop()


asyncio.run(main())
