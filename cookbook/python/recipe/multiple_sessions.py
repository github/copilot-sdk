#!/usr/bin/env python3
"""
Multiple Sessions - Managing independent conversations.

Run: python multiple_sessions.py
"""

import asyncio

from copilot import CopilotClient
from copilot.types import SessionEventType


# =============================================================================
# Basic Multiple Sessions
# =============================================================================


async def basic_multiple_sessions():
    """Create and use multiple independent sessions."""
    print("\n=== Basic Multiple Sessions ===\n")

    client = CopilotClient()

    try:
        await client.start()

        # Create independent sessions
        session1 = await client.create_session()
        session2 = await client.create_session()
        session3 = await client.create_session({"model": "claude-sonnet-4"})

        print(f"Session 1: {session1.session_id[:12]}...")
        print(f"Session 2: {session2.session_id[:12]}...")
        print(f"Session 3: {session3.session_id[:12]}...")

        # Each session has its own context
        await session1.send_and_wait({"prompt": "You help with Python."})
        await session2.send_and_wait({"prompt": "You help with JavaScript."})
        await session3.send_and_wait({"prompt": "You help with Go."})

        print("✓ Context established for all sessions")

        # Ask context-aware questions
        responses = {}

        def make_handler(name):
            def handler(event):
                if event.type == SessionEventType.ASSISTANT_MESSAGE:
                    responses[name] = event.data.content[:80] + "..."
            return handler

        session1.on(make_handler("Python"))
        session2.on(make_handler("JavaScript"))
        session3.on(make_handler("Go"))

        await session1.send_and_wait({"prompt": "How do I create a virtual env?"})
        await session2.send_and_wait({"prompt": "How do I set up package.json?"})
        await session3.send_and_wait({"prompt": "How do I initialize a module?"})

        print("\nResponses:")
        for name, response in responses.items():
            print(f"  {name}: {response}")

        await session1.destroy()
        await session2.destroy()
        await session3.destroy()

    finally:
        await client.stop()


# =============================================================================
# Parallel Execution
# =============================================================================


async def parallel_execution():
    """Execute requests across sessions in parallel."""
    print("\n=== Parallel Execution ===\n")

    client = CopilotClient()

    try:
        await client.start()

        topics = ["recursion", "polymorphism", "encapsulation"]
        sessions = [await client.create_session() for _ in topics]
        results = {}

        def make_handler(topic):
            def handler(event):
                if event.type == SessionEventType.ASSISTANT_MESSAGE:
                    results[topic] = event.data.content
            return handler

        for session, topic in zip(sessions, topics):
            session.on(make_handler(topic))

        # Execute all in parallel
        print("Executing requests in parallel...")
        await asyncio.gather(*[
            s.send_and_wait({"prompt": f"Define {t} in one sentence."})
            for s, t in zip(sessions, topics)
        ])

        print("\nResults:")
        for topic, response in results.items():
            print(f"  {topic}: {response[:80]}...")

        for session in sessions:
            await session.destroy()

    finally:
        await client.stop()


# =============================================================================
# Custom Session IDs
# =============================================================================


async def custom_session_ids():
    """Use custom IDs for session management."""
    print("\n=== Custom Session IDs ===\n")

    client = CopilotClient()

    try:
        await client.start()

        session_a = await client.create_session({"session_id": "user-42-support"})
        session_b = await client.create_session({"session_id": "user-42-dev"})

        print(f"Created: {session_a.session_id}")
        print(f"Created: {session_b.session_id}")

        await session_a.send_and_wait({"prompt": "I need help with a bug."})
        await session_b.send_and_wait({"prompt": "Let's design a feature."})

        # List sessions
        all_sessions = await client.list_sessions()
        print(f"\nTotal sessions: {len(all_sessions)}")

        await session_a.destroy()
        await session_b.destroy()

    finally:
        await client.stop()


# =============================================================================
# Session Pool
# =============================================================================


class SessionPool:
    """Reusable pool of sessions."""

    def __init__(self, client, size=3):
        self.client = client
        self.size = size
        self._available = asyncio.Queue()
        self._sessions = []

    async def initialize(self):
        for i in range(self.size):
            session = await self.client.create_session({"session_id": f"pool-{i}"})
            self._sessions.append(session)
            await self._available.put(session)
        print(f"✓ Pool initialized with {self.size} sessions")

    async def acquire(self, timeout=30.0):
        return await asyncio.wait_for(self._available.get(), timeout)

    async def release(self, session):
        await self._available.put(session)

    async def close(self):
        for s in self._sessions:
            await s.destroy()


async def session_pool_demo():
    """Demo session pool pattern."""
    print("\n=== Session Pool ===\n")

    client = CopilotClient()

    try:
        await client.start()

        pool = SessionPool(client, size=2)
        await pool.initialize()

        async def make_request(n):
            session = await pool.acquire()
            try:
                await session.send_and_wait({"prompt": f"Say 'request {n}'"})
                print(f"  Request {n} completed")
            finally:
                await pool.release(session)

        # Process 4 requests through 2 sessions
        print("Processing 4 requests through 2 sessions...")
        await asyncio.gather(*[make_request(i) for i in range(4)])

        await pool.close()
        print("✓ Pool closed")

    finally:
        await client.stop()


# =============================================================================
# Main
# =============================================================================


async def main():
    print("=" * 50)
    print("MULTIPLE SESSIONS")
    print("=" * 50)

    await basic_multiple_sessions()
    await parallel_execution()
    await custom_session_ids()
    await session_pool_demo()

    print("\n" + "=" * 50)
    print("All demos completed!")


if __name__ == "__main__":
    asyncio.run(main())
