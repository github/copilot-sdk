import asyncio
import os
import sys
from copilot import CopilotClient, SubprocessConfig

OPENAI_BASE_URL = os.environ.get("OPENAI_BASE_URL", "https://api.openai.com/v1")
OPENAI_MODEL = os.environ.get("OPENAI_MODEL", "claude-haiku-4.5")
OPENAI_API_KEY = os.environ.get("OPENAI_API_KEY")

if not OPENAI_API_KEY:
    print("Missing OPENAI_API_KEY.", file=sys.stderr)
    sys.exit(1)


async def main():
    client = CopilotClient(SubprocessConfig(
        cli_path=os.environ.get("COPILOT_CLI_PATH"),
    ))

    try:
        session = await client.create_session({
            "model": OPENAI_MODEL,
            "provider": {
                "type": "openai",
                "base_url": OPENAI_BASE_URL,
                "api_key": OPENAI_API_KEY,
            },
        })

        response = await session.send_and_wait(
            "What is the capital of France?"
        )

        if response:
            print(response.data.content)

        await session.disconnect()
    finally:
        await client.stop()


asyncio.run(main())
