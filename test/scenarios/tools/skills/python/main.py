import asyncio
import os
from pathlib import Path

from copilot import CopilotClient


async def main():
    opts = {"github_token": os.environ.get("GITHUB_TOKEN")}
    if os.environ.get("COPILOT_CLI_PATH"):
        opts["cli_path"] = os.environ["COPILOT_CLI_PATH"]
    client = CopilotClient(opts)

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
            {"prompt": "Use the greeting skill to greet someone named Alice."}
        )

        if response:
            print(response.data.content)

        print("\nSkill directories configured successfully")

        await session.destroy()
    finally:
        await client.stop()


asyncio.run(main())
