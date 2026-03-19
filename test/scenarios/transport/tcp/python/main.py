import asyncio
import os
from copilot import CopilotClient, PermissionHandler, ExternalServerConfig


async def main():
    client = CopilotClient(ExternalServerConfig(
        url=os.environ.get("COPILOT_CLI_URL", "localhost:3000"),
    ))

    try:
        session = await client.create_session(on_permission_request=PermissionHandler.approve_all, model="claude-haiku-4.5")

        response = await session.send_and_wait(
            "What is the capital of France?"
        )

        if response:
            print(response.data.content)

        await session.disconnect()
    finally:
        await client.stop()


asyncio.run(main())
