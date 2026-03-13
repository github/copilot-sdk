import asyncio
import os
from pathlib import Path

from copilot import CopilotClient, SubprocessConfig


async def main():
    client = CopilotClient(SubprocessConfig(
        github_token=os.environ.get("GITHUB_TOKEN"),
        cli_path=os.environ.get("COPILOT_CLI_PATH"),
    ))

    try:
        skills_dir = str(Path(__file__).resolve().parent.parent / "sample-skills")

        session = await client.create_session(
            {
                "model": "claude-haiku-4.5",
                "skill_directories": [skills_dir],
                "on_permission_request": lambda _: {"kind": "approved"},
                "hooks": {
                    "on_pre_tool_use": lambda _: {"permission_decision": "allow"},
                },
            }
        )

        response = await session.send_and_wait(
            "Use the greeting skill to greet someone named Alice."
        )

        if response:
            print(response.data.content)

        print("\nSkill directories configured successfully")

        await session.disconnect()
    finally:
        await client.stop()


asyncio.run(main())
