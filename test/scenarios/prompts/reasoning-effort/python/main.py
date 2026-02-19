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
