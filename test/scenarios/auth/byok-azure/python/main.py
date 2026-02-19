import asyncio
import os
import sys
from copilot import CopilotClient

AZURE_OPENAI_ENDPOINT = os.environ.get("AZURE_OPENAI_ENDPOINT")
AZURE_OPENAI_API_KEY = os.environ.get("AZURE_OPENAI_API_KEY")
AZURE_OPENAI_MODEL = os.environ.get("AZURE_OPENAI_MODEL", "claude-haiku-4.5")
AZURE_API_VERSION = os.environ.get("AZURE_API_VERSION", "2024-10-21")

if not AZURE_OPENAI_ENDPOINT or not AZURE_OPENAI_API_KEY:
    print("Required: AZURE_OPENAI_ENDPOINT and AZURE_OPENAI_API_KEY", file=sys.stderr)
    sys.exit(1)


async def main():
    opts = {}
    if os.environ.get("COPILOT_CLI_PATH"):
        opts["cli_path"] = os.environ["COPILOT_CLI_PATH"]
    client = CopilotClient(opts)

    try:
        session = await client.create_session({
            "model": AZURE_OPENAI_MODEL,
            "provider": {
                "type": "azure",
                "base_url": AZURE_OPENAI_ENDPOINT,
                "api_key": AZURE_OPENAI_API_KEY,
                "azure": {
                    "api_version": AZURE_API_VERSION,
                },
            },
            "available_tools": [],
            "system_message": {
                "mode": "replace",
                "content": "You are a helpful assistant. Answer concisely.",
            },
        })

        response = await session.send_and_wait(
            {"prompt": "What is the capital of France?"}
        )

        if response:
            print(response.data.content)

        await session.destroy()
    finally:
        await client.stop()


asyncio.run(main())
