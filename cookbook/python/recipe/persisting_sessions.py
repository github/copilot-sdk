#!/usr/bin/env python3

import asyncio
from copilot import CopilotClient

async def main():
    client = CopilotClient()
    await client.start()

    try:
        # Create session with a memorable ID
        # Note: the SDK might generate the ID, or we pass it in config if supported
        # Looking at client.py, create_session takes config which can have session_id
        session = await client.create_session({
            "session_id": "user-123-conversation",
            "model": "gpt-4",
        })

        await session.send_and_wait({"prompt": "Let's discuss TypeScript generics"})
        print(f"Session created: {session.session_id}")

        # Destroy session but keep data on disk?
        # Actually session.destroy() releases resources in the client but also sends 'session.destroy' to the server.
        # If the server persists sessions, we can resume.
        await session.destroy()
        print("Session destroyed (client side resources released)")

        # Resume the previous session
        resumed = await client.resume_session("user-123-conversation")
        print(f"Resumed: {resumed.session_id}")

        await resumed.send_and_wait({"prompt": "What were we discussing?"})

        # Listen for the response to verify
        # (In a real app we'd set up handlers before sending)

        # list_sessions and delete_session are not currently available in the Python SDK Client
        # sessions = await client.list_sessions()
        # print("Sessions:", [s["sessionId"] for s in sessions])

        # client.delete_session("user-123-conversation")
        # print("Session deleted")

        await resumed.destroy()

    finally:
        await client.stop()

if __name__ == "__main__":
    asyncio.run(main())
