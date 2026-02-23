import asyncio
import os
from copilot import CopilotClient


input_log: list[str] = []


async def auto_approve_permission(request, invocation):
    return {"kind": "approved"}


async def auto_approve_tool(input_data, invocation):
    return {"permissionDecision": "allow"}


async def handle_user_input(request, invocation):
    input_log.append(f"question: {request['question']}")
    return {"answer": "Paris", "wasFreeform": True}


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
                "on_user_input_request": handle_user_input,
                "hooks": {"on_pre_tool_use": auto_approve_tool},
            }
        )

        response = await session.send_and_wait(
            {
                "prompt": (
                    "I want to learn about a city. Use the ask_user tool to ask me "
                    "which city I'm interested in. Then tell me about that city."
                )
            }
        )

        if response:
            print(response.data.content)

        await session.destroy()

        print("\n--- User input log ---")
        for entry in input_log:
            print(f"  {entry}")
        print(f"\nTotal user input requests: {len(input_log)}")
    finally:
        await client.stop()


asyncio.run(main())
