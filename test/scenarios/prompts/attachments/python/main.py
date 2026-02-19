import asyncio
import os
from copilot import CopilotClient

SYSTEM_PROMPT = """You are a helpful assistant. Answer questions about attached files concisely."""


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

        sample_file = os.path.join(os.path.dirname(__file__), "..", "sample-data.txt")
        sample_file = os.path.abspath(sample_file)

        response = await session.send_and_wait(
            {
                "prompt": "What languages are listed in the attached file?",
                "attachments": [{"type": "file", "path": sample_file}],
            }
        )

        if response:
            print(response.data.content)

        await session.destroy()
    finally:
        await client.stop()


asyncio.run(main())
