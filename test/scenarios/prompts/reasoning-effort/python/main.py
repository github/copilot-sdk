import asyncio
import os
import copilot


async def main():
    client = copilot.cli_client(os.environ.get("COPILOT_CLI_PATH"), github_token=os.environ.get("GITHUB_TOKEN"))

    try:
        session = await client.create_session({
            "model": "claude-opus-4.6",
            "reasoning_effort": "low",
            "available_tools": [],
            "system_message": {
                "mode": "replace",
                "content": "You are a helpful assistant. Answer concisely.",
            },
        })

        response = await session.send_and_wait(
            {"prompt": "What is the capital of France?"}
        )

        if response:
            print("Reasoning effort: low")
            print(f"Response: {response.data.content}")

        await session.destroy()
    finally:
        await client.stop()


asyncio.run(main())
