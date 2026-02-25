import asyncio
import os
import copilot

SYSTEM_PROMPT = """You are a helpful assistant. You have access to a limited set of tools. When asked about your tools, list exactly which tools you have available."""


async def main():
    client = copilot.cli_client(os.environ.get("COPILOT_CLI_PATH"), github_token=os.environ.get("GITHUB_TOKEN"))

    try:
        session = await client.create_session(
            {
                "model": "claude-haiku-4.5",
                "system_message": {"mode": "replace", "content": SYSTEM_PROMPT},
                "available_tools": ["grep", "glob", "view"],
            }
        )

        response = await session.send_and_wait(
            {"prompt": "What tools do you have available? List each one by name."}
        )

        if response:
            print(response.data.content)

        await session.destroy()
    finally:
        await client.stop()


asyncio.run(main())
