#!/usr/bin/env python3

import asyncio
from copilot import CopilotClient

async def main():
    client = CopilotClient()
    await client.start()

    try:
        # Create multiple independent sessions with config dicts
        # Use gpt-5 for the first two sessions, per the SDK's SessionConfig valid models.
        session1 = await client.create_session({"model": "gpt-5"})
        session2 = await client.create_session({"model": "gpt-5"})
        # Use a valid Claude Sonnet model identifier as per SessionConfig type definition.
        session3 = await client.create_session({"model": "claude-sonnet-4.5"})

        print("Created 3 independent sessions")

        # Each session maintains its own conversation history
        # We can run these in parallel or sequence. Sequence is easier to follow in logs.
        await session1.send_and_wait({"prompt": "You are helping with a Python project"})
        await session2.send_and_wait({"prompt": "You are helping with a TypeScript project"})
        await session3.send_and_wait({"prompt": "You are helping with a Go project"})

        print("Sent initial context to all sessions")

        # Follow-up messages stay in their respective contexts
        await session1.send_and_wait({"prompt": "How do I create a virtual environment?"})
        await session2.send_and_wait({"prompt": "How do I set up tsconfig?"})
        await session3.send_and_wait({"prompt": "How do I initialize a module?"})

        print("Sent follow-up questions to each session")

        # Clean up all sessions
        await session1.destroy()
        await session2.destroy()
        await session3.destroy()

        print("All sessions destroyed successfully")

    finally:
        await client.stop()

if __name__ == "__main__":
    asyncio.run(main())
