import asyncio
import os
from copilot import CopilotClient


async def main():
    opts = {"github_token": os.environ.get("GITHUB_TOKEN")}
    if os.environ.get("COPILOT_CLI_PATH"):
        opts["cli_path"] = os.environ["COPILOT_CLI_PATH"]
    client = CopilotClient(opts)

    try:
        session = await client.create_session({
            "model": "claude-haiku-4.5",
            "available_tools": [],
            "system_message": {
                "mode": "replace",
                "content": "You are a helpful assistant. Answer concisely in one sentence.",
            },
            "infinite_sessions": {
                "enabled": True,
                "background_compaction_threshold": 0.80,
                "buffer_exhaustion_threshold": 0.95,
            },
        })

        prompts = [
            "What is the capital of France?",
            "What is the capital of Japan?",
            "What is the capital of Brazil?",
        ]

        for prompt in prompts:
            response = await session.send_and_wait({"prompt": prompt})
            if response:
                print(f"Q: {prompt}")
                print(f"A: {response.data.content}\n")

        print("Infinite sessions test complete — all messages processed successfully")

        await session.destroy()
    finally:
        await client.stop()


asyncio.run(main())
