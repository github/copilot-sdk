#!/usr/bin/env python3

import asyncio
from copilot import CopilotClient

async def main():
    client = CopilotClient()

    try:
        await client.start()
        # Ensure model is passed as part of a dict
        session = await client.create_session({"model": "gpt-4"})

        # Using a list to allow modification inside inner function (closure workaround)
        # or nonlocal would work if defined inside main
        response_data = {"content": None}

        def handle_message(event):
            if event.type == "assistant.message":
                response_data["content"] = event.data.content

        session.on(handle_message)

        # Use send_and_wait with timeout
        await session.send_and_wait({"prompt": "Hello!"}, timeout=30)

        if response_data["content"]:
            print(f"Copilot: {response_data['content']}")

        await session.destroy()

    except Exception as e:
        print(f"Error: {e}")
    finally:
        await client.stop()

if __name__ == "__main__":
    asyncio.run(main())
