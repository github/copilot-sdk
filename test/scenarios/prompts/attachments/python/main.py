import asyncio
import os
from copilot import CopilotClient, SubprocessConfig

SYSTEM_PROMPT = """You are a helpful assistant. Answer questions about attached files concisely."""


async def main():
    client = CopilotClient(SubprocessConfig(
        github_token=os.environ.get("GITHUB_TOKEN"),
        cli_path=os.environ.get("COPILOT_CLI_PATH"),
    ))

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

        await session.disconnect()
    finally:
        await client.stop()


asyncio.run(main())
