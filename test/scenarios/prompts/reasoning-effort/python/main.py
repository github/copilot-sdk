import asyncio

from copilot import CopilotClient


async def main():
    client = CopilotClient()

    try:
        session = await client.create_session(
            model="claude-opus-4.6",
            reasoning_effort="low",
            available_tools=[],
            system_message={
                "mode": "replace",
                "content": "You are a helpful assistant. Answer concisely.",
            },
        )

        response = await session.send_and_wait("What is the capital of France?")

        if response:
            print("Reasoning effort: low")
            print(f"Response: {response.data.content}")

        await session.disconnect()
    finally:
        await client.stop()


asyncio.run(main())
