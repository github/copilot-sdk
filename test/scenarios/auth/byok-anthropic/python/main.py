import asyncio
import os
import sys
from copilot import CopilotClient, PermissionHandler, SubprocessConfig

ANTHROPIC_API_KEY = os.environ.get("ANTHROPIC_API_KEY")
ANTHROPIC_MODEL = os.environ.get("ANTHROPIC_MODEL", "claude-sonnet-4-20250514")
ANTHROPIC_BASE_URL = os.environ.get("ANTHROPIC_BASE_URL", "https://api.anthropic.com")

if not ANTHROPIC_API_KEY:
    print("Missing ANTHROPIC_API_KEY.", file=sys.stderr)
    sys.exit(1)


async def main():
    client = CopilotClient(SubprocessConfig(
        cli_path=os.environ.get("COPILOT_CLI_PATH"),
    ))

    try:
        session = await client.create_session(
            PermissionHandler.approve_all,
            ANTHROPIC_MODEL,
            provider={
                "type": "anthropic",
                "base_url": ANTHROPIC_BASE_URL,
                "api_key": ANTHROPIC_API_KEY,
            },
            available_tools=[],
            system_message={
                "mode": "replace",
                "content": "You are a helpful assistant. Answer concisely.",
            },
        )

        response = await session.send_and_wait(
            {"prompt": "What is the capital of France?"}
        )

        if response:
            print(response.data.content)

        await session.disconnect()
    finally:
        await client.stop()


asyncio.run(main())
