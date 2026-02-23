import asyncio
import os
from copilot import CopilotClient

PIRATE_PROMPT = "You are a pirate. Always say Arrr!"
ROBOT_PROMPT = "You are a robot. Always say BEEP BOOP!"


async def main():
    opts = {"github_token": os.environ.get("GITHUB_TOKEN")}
    if os.environ.get("COPILOT_CLI_PATH"):
        opts["cli_path"] = os.environ["COPILOT_CLI_PATH"]
    client = CopilotClient(opts)

    try:
        session1, session2 = await asyncio.gather(
            client.create_session(
                {
                    "model": "claude-haiku-4.5",
                    "system_message": {"mode": "replace", "content": PIRATE_PROMPT},
                    "available_tools": [],
                }
            ),
            client.create_session(
                {
                    "model": "claude-haiku-4.5",
                    "system_message": {"mode": "replace", "content": ROBOT_PROMPT},
                    "available_tools": [],
                }
            ),
        )

        response1, response2 = await asyncio.gather(
            session1.send_and_wait(
                {"prompt": "What is the capital of France?"}
            ),
            session2.send_and_wait(
                {"prompt": "What is the capital of France?"}
            ),
        )

        if response1:
            print("Session 1 (pirate):", response1.data.content)
        if response2:
            print("Session 2 (robot):", response2.data.content)

        await asyncio.gather(session1.destroy(), session2.destroy())
    finally:
        await client.stop()


asyncio.run(main())
