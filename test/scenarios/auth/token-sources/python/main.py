import asyncio
import os
import subprocess
from copilot import CopilotClient


def resolve_token():
    if os.environ.get("COPILOT_GITHUB_TOKEN"):
        return os.environ["COPILOT_GITHUB_TOKEN"], "COPILOT_GITHUB_TOKEN"
    if os.environ.get("GH_TOKEN"):
        return os.environ["GH_TOKEN"], "GH_TOKEN"
    if os.environ.get("GITHUB_TOKEN"):
        return os.environ["GITHUB_TOKEN"], "GITHUB_TOKEN"
    try:
        token = subprocess.check_output(
            ["gh", "auth", "token"], text=True
        ).strip()
        if token:
            return token, "gh CLI"
    except (subprocess.CalledProcessError, FileNotFoundError):
        pass
    return None, "gh CLI or stored OAuth"


async def main():
    token, source = resolve_token()
    print(f"Token source resolved: {source}")

    opts = {}
    if os.environ.get("COPILOT_CLI_PATH"):
        opts["cli_path"] = os.environ["COPILOT_CLI_PATH"]
    if token:
        opts["github_token"] = token
    client = CopilotClient(opts)

    try:
        session = await client.create_session({
            "model": "gpt-4.1",
            "available_tools": [],
            "system_message": {
                "mode": "replace",
                "content": "You are a helpful assistant. Answer concisely.",
            },
        })

        response = await session.send_and_wait(
            {"prompt": "What is the capital of France?"}
        )

        if response:
            print(response.data.content)

        print("\nAuth test passed — token resolved successfully")

        await session.destroy()
    finally:
        await client.stop()


asyncio.run(main())
