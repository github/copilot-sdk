#!/usr/bin/env python3

import asyncio
import os
from copilot import CopilotClient
from copilot.generated.session_events import SessionEventType

async def main():
    # Create and start client
    client = CopilotClient()
    await client.start()

    try:
        # Create session
        session = await client.create_session({"model": "gpt-5"})

        # Event handler
        def handle_event(event):
            if event.type == SessionEventType.ASSISTANT_MESSAGE:
                print(f"\nCopilot: {event.data.content}")
            elif event.type == SessionEventType.TOOL_EXECUTION_START:
                print(f"  → Running: {event.data.tool_name}")
            elif event.type == SessionEventType.TOOL_EXECUTION_COMPLETE:
                # Check if tool_call_id exists in data
                call_id = getattr(event.data, "tool_call_id", "unknown")
                print(f"  ✓ Completed: {call_id}")

        session.on(handle_event)

        # Ask Copilot to organize files
        # Change this to your target folder
        target_folder = os.path.expanduser("~/Downloads")

        print(f"📂 Organizing files in: {target_folder}\n")

        await session.send_and_wait({
            "prompt": f"""
            Analyze the files in "{target_folder}" and organize them into subfolders.

            1. First, list all files and their metadata
            2. Preview grouping by file extension
            3. Create appropriate subfolders (e.g., "images", "documents", "videos")
            4. Move each file to its appropriate subfolder

            Please confirm before moving any files.
            """
        }, timeout=300)

        # Allow user to respond if Copilot asks for confirmation
        while True:
            user_input = await asyncio.get_event_loop().run_in_executor(None, input, "\nYou: ")
            if user_input.lower() in ["exit", "quit", "no", "n"]:
                break

            await session.send_and_wait({"prompt": user_input}, timeout=300)

        await session.destroy()

    finally:
        await client.stop()

if __name__ == "__main__":
    asyncio.run(main())
