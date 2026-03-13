import asyncio
import os

from pydantic import BaseModel, Field

from copilot import CopilotClient, PermissionHandler, SubprocessConfig, define_tool


class GrepParams(BaseModel):
    query: str = Field(description="Search query")


@define_tool("grep", description="A custom grep implementation that overrides the built-in", overrides_built_in_tool=True)
def custom_grep(params: GrepParams) -> str:
    return f"CUSTOM_GREP_RESULT: {params.query}"


async def main():
    client = CopilotClient(SubprocessConfig(
        github_token=os.environ.get("GITHUB_TOKEN"),
        cli_path=os.environ.get("COPILOT_CLI_PATH"),
    ))

    try:
        session = await client.create_session(
            {
                "model": "claude-haiku-4.5",
                "tools": [custom_grep],
                "on_permission_request": PermissionHandler.approve_all,
            }
        )

        response = await session.send_and_wait(
            "Use grep to search for the word 'hello'"
        )

        if response:
            print(response.data.content)

        await session.disconnect()
    finally:
        await client.stop()


asyncio.run(main())
