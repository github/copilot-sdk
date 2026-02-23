import asyncio
import os
from copilot import CopilotClient


async def main():
    opts = {"github_token": os.environ.get("GITHUB_TOKEN")}
    if os.environ.get("COPILOT_CLI_PATH"):
        opts["cli_path"] = os.environ["COPILOT_CLI_PATH"]
    client = CopilotClient(opts)

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
            {"prompt": "Remember this: the secret word is PINEAPPLE."}
        )

        # 3. Get the session ID (don't destroy — resume needs the session to persist)
        session_id = session.session_id

        # 4. Resume the session with the same ID
        resumed = await client.resume_session(session_id)
        print("Session resumed")

        # 5. Ask for the secret word
        response = await resumed.send_and_wait(
            {"prompt": "What was the secret word I told you?"}
        )

        if response:
            print(response.data.content)

        await resumed.destroy()
    finally:
        await client.stop()


asyncio.run(main())
