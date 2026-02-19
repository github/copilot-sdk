import asyncio
import os
import sys
from copilot import CopilotClient


async def main():
    client = CopilotClient({
        "cli_url": os.environ.get("COPILOT_CLI_URL", "localhost:3000"),
    })

    try:
        # First session
        print("--- Session 1 ---")
        session1 = await client.create_session({"model": "claude-haiku-4.5"})

        response1 = await session1.send_and_wait(
            {"prompt": "What is the capital of France?"}
        )

        if response1 and response1.data.content:
            print(response1.data.content)
        else:
            print("No response content received for session 1", file=sys.stderr)
            sys.exit(1)

        await session1.destroy()
        print("Session 1 destroyed\n")

        # Second session — tests that the server accepts new sessions
        print("--- Session 2 ---")
        session2 = await client.create_session({"model": "claude-haiku-4.5"})

        response2 = await session2.send_and_wait(
            {"prompt": "What is the capital of France?"}
        )

        if response2 and response2.data.content:
            print(response2.data.content)
        else:
            print("No response content received for session 2", file=sys.stderr)
            sys.exit(1)

        await session2.destroy()
        print("Session 2 destroyed")

        print("\nReconnect test passed — both sessions completed successfully")
    finally:
        await client.stop()


asyncio.run(main())
