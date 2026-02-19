import asyncio
import os
from copilot import CopilotClient

# Track which tools requested permission
permission_log: list[str] = []


async def log_permission(request, invocation):
    permission_log.append(f"approved:{request.tool_name}")
    return {"kind": "approved"}


async def auto_approve_tool(input_data, invocation):
    return {"permissionDecision": "allow"}


async def main():
    opts = {"github_token": os.environ.get("GITHUB_TOKEN")}
    if os.environ.get("COPILOT_CLI_PATH"):
        opts["cli_path"] = os.environ["COPILOT_CLI_PATH"]
    client = CopilotClient(opts)

    try:
        session = await client.create_session(
            {
                "model": "claude-haiku-4.5",
                "on_permission_request": log_permission,
                "hooks": {"on_pre_tool_use": auto_approve_tool},
            }
        )

        response = await session.send_and_wait(
            {
                "prompt": "List the files in the current directory using glob with pattern '*.md'."
            }
        )

        if response:
            print(response.data.content)

        await session.destroy()

        print("\n--- Permission request log ---")
        for entry in permission_log:
            print(f"  {entry}")
        print(f"\nTotal permission requests: {len(permission_log)}")
    finally:
        await client.stop()


asyncio.run(main())
