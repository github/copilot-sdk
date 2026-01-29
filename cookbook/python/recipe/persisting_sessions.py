#!/usr/bin/env python3
"""
Session Persistence - Demonstrates saving and resuming conversation sessions.
Run: python persisting_sessions.py
"""

import asyncio
from datetime import datetime

from copilot import CopilotClient
from copilot.types import SessionEventType


# =============================================================================
# Basic Session Persistence
# =============================================================================


async def basic_persistence():
    """Create and resume sessions with full history."""
    print("\n=== Basic Session Persistence ===\n")

    client = CopilotClient()

    try:
        await client.start()

        # Create a session with a memorable ID
        session_id = f"demo-session-{datetime.now().strftime('%H%M%S')}"
        session = await client.create_session({"session_id": session_id})

        print(f"✓ Created session: {session.session_id}")

        # Send a message to establish context
        response_content = None

        def handler(event):
            nonlocal response_content
            if event.type == SessionEventType.ASSISTANT_MESSAGE:
                response_content = event.data.content

        session.on(handler)

        await session.send_and_wait(
            {"prompt": "Remember this: The secret code is ALPHA-7. What is it?"},
            timeout=60.0,
        )

        if response_content:
            print(f"   Response: {response_content[:100]}...")

        # Destroy session (disconnects but keeps data on disk)
        await session.destroy()
        print("✓ Session destroyed (data persisted to disk)")

        # Resume the session - all history is restored
        print("\nResuming session...")
        resumed = await client.resume_session(session_id)

        # Set up handler for resumed session
        response_content = None
        resumed.on(handler)

        print(f"✓ Resumed session: {resumed.session_id}")

        # Ask about the previous context - it should remember!
        await resumed.send_and_wait(
            {"prompt": "What was the secret code I mentioned earlier?"},
            timeout=60.0,
        )

        if response_content:
            print(f"   Response: {response_content}")

        # Get session history
        messages = await resumed.get_messages()
        print(f"\n✓ Session has {len(messages)} events in history")

        await resumed.destroy()

        # Delete session permanently
        await client.delete_session(session_id)
        print(f"✓ Session '{session_id}' deleted permanently")

    finally:
        await client.stop()


# =============================================================================
# Session Management
# =============================================================================


async def session_management():
    """Manage multiple persistent sessions."""
    print("\n=== Session Management ===\n")

    client = CopilotClient()

    try:
        await client.start()

        # Create several sessions
        sessions_to_create = [
            ("user-alice-support", "I need help with Python decorators"),
            ("user-bob-dev", "Let's design a REST API"),
            ("project-demo", "This is a demo session"),
        ]

        print("Creating multiple sessions...")
        for session_id, initial_message in sessions_to_create:
            session = await client.create_session({"session_id": session_id})
            await session.send_and_wait({"prompt": initial_message}, timeout=30.0)
            await session.destroy()
            print(f"  ✓ Created and saved: {session_id}")

        # List all available sessions
        print("\nListing all sessions:")
        all_sessions = await client.list_sessions()

        for s in all_sessions:
            session_id = s["sessionId"]
            modified = s.get("modifiedTime", "Unknown")
            summary = s.get("summary", "No summary")[:50]
            print(f"  - {session_id}")
            print(f"    Modified: {modified}")
            print(f"    Summary: {summary}...")

        print(f"\nTotal sessions: {len(all_sessions)}")

        # Resume a specific session
        print("\nResuming 'user-alice-support'...")
        try:
            alice_session = await client.resume_session("user-alice-support")

            # Get the full message history
            history = await alice_session.get_messages()
            print(f"  ✓ Restored with {len(history)} events")

            # Show event types in history
            event_types = {}
            for event in history:
                event_type = event.type
                event_types[event_type] = event_types.get(event_type, 0) + 1

            print("  Event breakdown:")
            for event_type, count in sorted(event_types.items()):
                print(f"    - {event_type}: {count}")

            await alice_session.destroy()

        except RuntimeError as e:
            print(f"  ✗ Could not resume: {e}")

        # Clean up all demo sessions
        print("\nCleaning up demo sessions...")
        for session_id, _ in sessions_to_create:
            try:
                await client.delete_session(session_id)
                print(f"  ✓ Deleted: {session_id}")
            except RuntimeError:
                print(f"  ⚠ Already deleted: {session_id}")

    finally:
        await client.stop()


# =============================================================================
# Infinite Sessions with Compaction
# =============================================================================


async def infinite_sessions_demo():
    """Use infinite sessions with automatic context compaction."""
    print("\n=== Infinite Sessions with Compaction ===\n")

    client = CopilotClient()

    try:
        await client.start()

        # Create a session with infinite session configuration
        session = await client.create_session(
            {
                "session_id": "infinite-demo",
                "infinite_sessions": {
                    "enabled": True,
                    "background_compaction_threshold": 0.80,
                    "buffer_exhaustion_threshold": 0.95,
                },
            }
        )

        print(f"✓ Created infinite session: {session.session_id}")

        # Check workspace path (where session state is stored)
        if session.workspace_path:
            print(f"  Workspace: {session.workspace_path}")

        # Send some messages to build up context
        print("\nBuilding conversation context...")

        topics = [
            "Tell me about Python's GIL in 2 sentences.",
            "Explain async/await in 2 sentences.",
            "What are decorators? 2 sentences.",
        ]

        for topic in topics:
            await session.send_and_wait({"prompt": topic}, timeout=60.0)
            print(f"  ✓ Discussed: {topic[:30]}...")

        # Get message count
        messages = await session.get_messages()
        print(f"\n✓ Session has {len(messages)} events")

        # When the context window fills up, compaction happens automatically
        # The session remains usable without losing important context

        await session.destroy()

        # Resume to verify persistence
        print("\nResuming infinite session...")
        resumed = await client.resume_session("infinite-demo")

        messages = await resumed.get_messages()
        print(f"✓ Restored with {len(messages)} events")

        await resumed.destroy()

        # Cleanup
        await client.delete_session("infinite-demo")
        print("✓ Cleaned up infinite session")

    finally:
        await client.stop()


# =============================================================================
# Session Export Pattern
# =============================================================================


async def session_export_pattern():
    """Export and inspect session history."""
    print("\n=== Session Export Pattern ===\n")

    client = CopilotClient()

    try:
        await client.start()

        # Create a session with some conversation
        session = await client.create_session({"session_id": "export-demo"})

        # Have a short conversation
        exchanges = [
            "What is the Fibonacci sequence?",
            "Show me the first 10 Fibonacci numbers.",
            "What's the 50th Fibonacci number?",
        ]

        for prompt in exchanges:
            await session.send_and_wait({"prompt": prompt}, timeout=60.0)

        # Export the full history
        history = await session.get_messages()

        print(f"Exported {len(history)} events from session\n")

        # Analyze the conversation
        print("Conversation summary:")
        for event in history:
            event_type = event.type

            if event_type == "user.message":
                content = getattr(event.data, "content", "")
                print(f"  👤 User: {content[:60]}...")

            elif event_type == "assistant.message":
                content = getattr(event.data, "content", "")
                # Truncate long responses
                display = content[:100] + "..." if len(content) > 100 else content
                print(f"  🤖 Assistant: {display}")

            elif event_type == "tool.execution_complete":
                tool_name = getattr(event.data, "tool_name", "unknown")
                print(f"  ⚙️  Tool: {tool_name}")

        await session.destroy()
        await client.delete_session("export-demo")
        print("\n✓ Session exported and cleaned up")

    finally:
        await client.stop()


# =============================================================================
# Conversation Bookmarks
# =============================================================================


async def conversation_bookmarks():
    """Save conversation checkpoints for later resumption."""
    print("\n=== Conversation Bookmarks Pattern ===\n")

    client = CopilotClient()

    try:
        await client.start()

        # Simulate a user's multi-part task
        base_id = f"user123-task-{datetime.now().strftime('%Y%m%d')}"

        # Checkpoint 1: Initial planning
        session = await client.create_session({"session_id": f"{base_id}-planning"})
        await session.send_and_wait(
            {"prompt": "I want to build a web scraper. What are the steps?"},
            timeout=60.0,
        )
        await session.destroy()
        print("✓ Saved checkpoint: planning")

        # Checkpoint 2: Implementation
        session = await client.create_session(
            {"session_id": f"{base_id}-implementation"}
        )
        await session.send_and_wait(
            {"prompt": "Show me Python code for a basic web scraper."},
            timeout=60.0,
        )
        await session.destroy()
        print("✓ Saved checkpoint: implementation")

        # List bookmarks for this task
        all_sessions = await client.list_sessions()
        task_sessions = [s for s in all_sessions if base_id in s["sessionId"]]

        print(f"\nBookmarks for task {base_id}:")
        for s in task_sessions:
            print(f"  - {s['sessionId']}")
            print(f"    Modified: {s.get('modifiedTime', 'Unknown')}")

        # User can resume any checkpoint
        print("\nResuming 'planning' checkpoint...")
        planning = await client.resume_session(f"{base_id}-planning")
        messages = await planning.get_messages()
        print(f"✓ Restored planning session with {len(messages)} events")
        await planning.destroy()

        # Cleanup
        for s in task_sessions:
            await client.delete_session(s["sessionId"])
        print("\n✓ Cleaned up all checkpoints")

    finally:
        await client.stop()


# =============================================================================
# Main
# =============================================================================


async def main():
    """Run all session persistence demonstrations."""
    print("=" * 60)
    print("Session Persistence Patterns")
    print("=" * 60)

    await basic_persistence()
    await session_management()
    await infinite_sessions_demo()
    await session_export_pattern()
    await conversation_bookmarks()

    print("\n" + "=" * 60)
    print("All patterns demonstrated!")
    print("=" * 60)


if __name__ == "__main__":
    asyncio.run(main())
