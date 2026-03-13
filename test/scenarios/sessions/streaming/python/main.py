import asyncio
import os
from copilot import CopilotClient, PermissionHandler, SubprocessConfig


async def main():
    client = CopilotClient(SubprocessConfig(
        github_token=os.environ.get("GITHUB_TOKEN"),
        cli_path=os.environ.get("COPILOT_CLI_PATH"),
    ))

    try:
        session = await client.create_session(
            PermissionHandler.approve_all,
            "claude-haiku-4.5",
            streaming=True,
        )

        chunk_count = 0

        def on_event(event):
            nonlocal chunk_count
            if event.type.value == "assistant.message_delta":
                chunk_count += 1

        session.on(on_event)

        response = await session.send_and_wait(
            {"prompt": "What is the capital of France?"}
        )

        if response:
            print(response.data.content)
        print(f"\nStreaming chunks received: {chunk_count}")

        await session.disconnect()
    finally:
        await client.stop()


asyncio.run(main())
