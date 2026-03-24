import asyncio
import os
from copilot import CopilotClient
from copilot.client import SubprocessConfig


async def main():
    client = CopilotClient(SubprocessConfig(
        github_token=os.environ.get("GITHUB_TOKEN"),
        cli_path=os.environ.get("COPILOT_CLI_PATH"),
    ))

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
            response = await session.send_and_wait(prompt)
            if response:
                print(f"Q: {prompt}")
                print(f"A: {response.data.content}\n")

        print("Infinite sessions test complete — all messages processed successfully")

        await session.disconnect()
    finally:
        await client.stop()


asyncio.run(main())
