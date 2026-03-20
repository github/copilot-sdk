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
            "model": "claude-opus-4.6",
            "reasoning_effort": "low",
            "available_tools": [],
            "system_message": {
                "mode": "replace",
                "content": "You are a helpful assistant. Answer concisely.",
            },
        })

        response = await session.send_and_wait(
            "What is the capital of France?"
        )

        if response:
            print("Reasoning effort: low")
            print(f"Response: {response.data.content}")

        await session.disconnect()
    finally:
        await client.stop()


asyncio.run(main())
