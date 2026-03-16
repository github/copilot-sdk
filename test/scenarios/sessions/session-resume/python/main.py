import asyncio
import os
from copilot import CopilotClient, SubprocessConfig


async def main():
    client = CopilotClient(SubprocessConfig(
        github_token=os.environ.get("GITHUB_TOKEN"),
        cli_path=os.environ.get("COPILOT_CLI_PATH"),
    ))

    try:
        # 1. Create a session
        session = await client.create_session(
            {
                "model": "claude-haiku-4.5",
                "available_tools": [],
            }
        )

        # 2. Send the secret word
        await session.send_and_wait(
            "Remember this: the secret word is PINEAPPLE."
        )

        # 3. Get the session ID (don't disconnect — resume needs the session to persist)
        session_id = session.session_id

        # 4. Resume the session with the same ID
        resumed = await client.resume_session(session_id)
        print("Session resumed")

        # 5. Ask for the secret word
        response = await resumed.send_and_wait(
            "What was the secret word I told you?"
        )

        if response:
            print(response.data.content)

        await resumed.disconnect()
    finally:
        await client.stop()


asyncio.run(main())
