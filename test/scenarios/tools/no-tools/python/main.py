import asyncio
import os
from copilot import CopilotClient

SYSTEM_PROMPT = """You are a minimal assistant with no tools available.
You cannot execute code, read files, edit files, search, or perform any actions.
You can only respond with text based on your training data.
If asked about your capabilities or tools, clearly state that you have no tools available."""


async def main():
    opts = {"github_token": os.environ.get("GITHUB_TOKEN")}
    if os.environ.get("COPILOT_CLI_PATH"):
        opts["cli_path"] = os.environ["COPILOT_CLI_PATH"]
    client = CopilotClient(opts)

    try:
        session = await client.create_session(
            {
                "model": "claude-haiku-4.5",
                "system_message": {"mode": "replace", "content": SYSTEM_PROMPT},
                "available_tools": [],
            }
        )

        response = await session.send_and_wait(
            {"prompt": "Use the bash tool to run 'echo hello'."}
        )

        if response:
            print(response.data.content)

        await session.destroy()
    finally:
        await client.stop()


asyncio.run(main())
