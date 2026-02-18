import asyncio
import os
from copilot import CopilotClient


async def main():
    opts = {"github_token": os.environ.get("GITHUB_TOKEN")}
    if os.environ.get("COPILOT_CLI_PATH"):
        opts["cli_path"] = os.environ["COPILOT_CLI_PATH"]
    client = CopilotClient(opts)

    try:
        session = await client.create_session(
            {
                "model": "gpt-4.1",
                "streaming": True,
            }
        )

        chunk_count = 0

        def on_event(event):
            nonlocal chunk_count
            if event.type == "assistant.message.chunk":
                chunk_count += 1

        session.on(on_event)

        response = await session.send_and_wait(
            {"prompt": "What is the capital of France?"}
        )

        if response:
            print(response.data.content)
        print(f"\nStreaming chunks received: {chunk_count}")

        await session.destroy()
    finally:
        await client.stop()


asyncio.run(main())
