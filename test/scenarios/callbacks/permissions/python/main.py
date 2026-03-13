import asyncio
import os
from copilot import CopilotClient, SubprocessConfig

# Track which tools requested permission
permission_log: list[str] = []


async def log_permission(request, invocation):
    permission_log.append(f"approved:{request.tool_name}")
    return {"kind": "approved"}


async def auto_approve_tool(input_data, invocation):
    return {"permissionDecision": "allow"}


async def main():
    client = CopilotClient(SubprocessConfig(
        github_token=os.environ.get("GITHUB_TOKEN"),
        cli_path=os.environ.get("COPILOT_CLI_PATH"),
    ))

    try:
        session = await client.create_session(
            {
                "model": "claude-haiku-4.5",
                "on_permission_request": log_permission,
                "hooks": {"on_pre_tool_use": auto_approve_tool},
            }
        )

        response = await session.send_and_wait(
            "List the files in the current directory using glob with pattern '*.md'."
        )

        if response:
            print(response.data.content)

        await session.disconnect()

        print("\n--- Permission request log ---")
        for entry in permission_log:
            print(f"  {entry}")
        print(f"\nTotal permission requests: {len(permission_log)}")
    finally:
        await client.stop()


asyncio.run(main())
