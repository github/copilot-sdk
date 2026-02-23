import asyncio
import os
from copilot import CopilotClient


hook_log: list[str] = []


async def auto_approve_permission(request, invocation):
    return {"kind": "approved"}


async def on_session_start(input_data, invocation):
    hook_log.append("onSessionStart")


async def on_session_end(input_data, invocation):
    hook_log.append("onSessionEnd")


async def on_pre_tool_use(input_data, invocation):
    tool_name = input_data.get("toolName", "unknown")
    hook_log.append(f"onPreToolUse:{tool_name}")
    return {"permissionDecision": "allow"}


async def on_post_tool_use(input_data, invocation):
    tool_name = input_data.get("toolName", "unknown")
    hook_log.append(f"onPostToolUse:{tool_name}")


async def on_user_prompt_submitted(input_data, invocation):
    hook_log.append("onUserPromptSubmitted")
    return input_data


async def on_error_occurred(input_data, invocation):
    error = input_data.get("error", "unknown")
    hook_log.append(f"onErrorOccurred:{error}")


async def main():
    opts = {"github_token": os.environ.get("GITHUB_TOKEN")}
    if os.environ.get("COPILOT_CLI_PATH"):
        opts["cli_path"] = os.environ["COPILOT_CLI_PATH"]
    client = CopilotClient(opts)

    try:
        session = await client.create_session(
            {
                "model": "claude-haiku-4.5",
                "on_permission_request": auto_approve_permission,
                "hooks": {
                    "on_session_start": on_session_start,
                    "on_session_end": on_session_end,
                    "on_pre_tool_use": on_pre_tool_use,
                    "on_post_tool_use": on_post_tool_use,
                    "on_user_prompt_submitted": on_user_prompt_submitted,
                    "on_error_occurred": on_error_occurred,
                },
            }
        )

        response = await session.send_and_wait(
            {
                "prompt": "List the files in the current directory using the glob tool with pattern '*.md'.",
            }
        )

        if response:
            print(response.data.content)

        await session.destroy()

        print("\n--- Hook execution log ---")
        for entry in hook_log:
            print(f"  {entry}")
        print(f"\nTotal hooks fired: {len(hook_log)}")
    finally:
        await client.stop()


asyncio.run(main())
