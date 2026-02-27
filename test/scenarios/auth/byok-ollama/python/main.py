import asyncio
import os
import sys
from copilot import CopilotClient

OLLAMA_BASE_URL = os.environ.get("OLLAMA_BASE_URL", "http://localhost:11434/v1")
OLLAMA_MODEL = os.environ.get("OLLAMA_MODEL", "llama3.2:3b")

COMPACT_SYSTEM_PROMPT = (
    "You are a compact local assistant. Keep answers short, concrete, and under 80 words."
)


async def main():
    opts = {}
    if os.environ.get("COPILOT_CLI_PATH"):
        opts["cli_path"] = os.environ["COPILOT_CLI_PATH"]
    client = CopilotClient(opts)

    try:
        session = await client.create_session({
            "model": OLLAMA_MODEL,
            "provider": {
                "type": "openai",
                "base_url": OLLAMA_BASE_URL,
            },
            "available_tools": [],
            "system_message": {
                "mode": "replace",
                "content": COMPACT_SYSTEM_PROMPT,
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
