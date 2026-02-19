import asyncio
import os
from copilot import CopilotClient


async def main():
    client = CopilotClient({
        "cli_url": os.environ.get("COPILOT_CLI_URL", "localhost:3000"),
    })

    try:
        session = await client.create_session({"model": "claude-haiku-4.5"})

        response = await session.send_and_wait(
            {"prompt": "What is the capital of France?"}
        )

        if response:
            print(response.data.content)

        await session.destroy()
    finally:
        await client.stop()


asyncio.run(main())
