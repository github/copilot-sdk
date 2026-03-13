import asyncio
import os
from copilot import CopilotClient, PermissionHandler, SubprocessConfig

PIRATE_PROMPT = """You are a pirate. Always respond in pirate speak. Say 'Arrr!' in every response. Use nautical terms and pirate slang throughout."""


async def main():
    client = CopilotClient(SubprocessConfig(
        github_token=os.environ.get("GITHUB_TOKEN"),
        cli_path=os.environ.get("COPILOT_CLI_PATH"),
    ))

    try:
        session = await client.create_session(
            PermissionHandler.approve_all,
            "claude-haiku-4.5",
            system_message={"mode": "replace", "content": PIRATE_PROMPT},
            available_tools=[],
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
