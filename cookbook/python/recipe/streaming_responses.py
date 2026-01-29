#!/usr/bin/env python3
"""
Streaming Responses - Real-time streaming with the Copilot SDK.

Run: python streaming_responses.py
"""

import asyncio
import sys

from copilot import CopilotClient
from copilot.types import SessionEventType


# =============================================================================
# Basic Streaming
# =============================================================================


async def basic_streaming():
    """Stream responses in real-time."""
    print("\n=== Basic Streaming ===\n")

    client = CopilotClient()

    try:
        await client.start()
        session = await client.create_session({"streaming": True})

        print("Ask: Write a haiku about programming\n")
        print("Response: ", end="", flush=True)

        def handler(event):
            if event.type == SessionEventType.ASSISTANT_MESSAGE_DELTA:
                delta = getattr(event.data, "delta_content", "")
                if delta:
                    print(delta, end="", flush=True)
            elif event.type == SessionEventType.SESSION_IDLE:
                print("\n")

        session.on(handler)

        await session.send_and_wait(
            {"prompt": "Write a haiku about programming"},
            timeout=60.0
        )

        await session.destroy()

    finally:
        await client.stop()


# =============================================================================
# Streaming with Progress
# =============================================================================


async def streaming_with_progress():
    """Show progress indicators during streaming."""
    print("\n=== Streaming with Progress ===\n")

    client = CopilotClient()

    try:
        await client.start()
        session = await client.create_session({"streaming": True})

        state = {"thinking": False, "tools": 0, "chars": 0}

        def handler(event):
            if event.type == SessionEventType.ASSISTANT_REASONING_DELTA:
                if not state["thinking"]:
                    print("\n💭 Thinking: ", end="", flush=True)
                    state["thinking"] = True
                delta = getattr(event.data, "delta_content", "")
                if delta:
                    print(delta, end="", flush=True)

            elif event.type == SessionEventType.ASSISTANT_MESSAGE_DELTA:
                if state["thinking"]:
                    print("\n\n📝 Response: ", end="", flush=True)
                    state["thinking"] = False
                delta = getattr(event.data, "delta_content", "")
                if delta:
                    print(delta, end="", flush=True)
                    state["chars"] += len(delta)

            elif event.type == SessionEventType.TOOL_EXECUTION_START:
                state["tools"] += 1
                print(f"\n  🔧 [{state['tools']}] {event.data.tool_name}...", end="")

            elif event.type == SessionEventType.TOOL_EXECUTION_COMPLETE:
                print(" ✓")

            elif event.type == SessionEventType.SESSION_IDLE:
                print(f"\n\n📊 {state['chars']} chars, {state['tools']} tools")

        session.on(handler)

        print("Ask: Explain recursion with a code example\n")

        await session.send_and_wait(
            {"prompt": "Explain recursion with a simple Python example"},
            timeout=120.0
        )

        await session.destroy()

    finally:
        await client.stop()


# =============================================================================
# Interactive Chat
# =============================================================================


async def interactive_chat():
    """Interactive chat with streaming."""
    print("\n=== Interactive Chat ===\n")
    print("Type messages. Press Ctrl+C or 'exit' to quit.\n")

    client = CopilotClient({"log_level": "error"})

    try:
        await client.start()
        session = await client.create_session({"streaming": True})

        response_started = False

        def handler(event):
            nonlocal response_started

            if event.type == SessionEventType.ASSISTANT_MESSAGE_DELTA:
                if not response_started:
                    print("\n🤖 ", end="", flush=True)
                    response_started = True
                delta = getattr(event.data, "delta_content", "")
                if delta:
                    print(delta, end="", flush=True)

            elif event.type == SessionEventType.TOOL_EXECUTION_START:
                print(f"\n   ⚙️ {event.data.tool_name}", end="")

            elif event.type == SessionEventType.SESSION_IDLE:
                if response_started:
                    print("\n")
                response_started = False

        session.on(handler)

        while True:
            try:
                user_input = input("You: ").strip()
            except (EOFError, KeyboardInterrupt):
                print("\n👋 Goodbye!")
                break

            if user_input.lower() in ["exit", "quit"]:
                print("👋 Goodbye!")
                break

            if user_input:
                response_started = False
                await session.send_and_wait({"prompt": user_input}, timeout=120.0)

        await session.destroy()

    finally:
        await client.stop()


# =============================================================================
# Typewriter Effect
# =============================================================================


async def typewriter_effect():
    """Display with typewriter animation."""
    print("\n=== Typewriter Effect ===\n")

    client = CopilotClient()

    try:
        await client.start()
        session = await client.create_session({"streaming": True})

        buffer = []
        complete = asyncio.Event()

        def handler(event):
            if event.type == SessionEventType.ASSISTANT_MESSAGE_DELTA:
                delta = getattr(event.data, "delta_content", "")
                if delta:
                    buffer.append(delta)
            elif event.type == SessionEventType.SESSION_IDLE:
                complete.set()

        session.on(handler)

        # Start request
        asyncio.create_task(session.send_and_wait(
            {"prompt": "Write a short poem about code."},
            timeout=60.0
        ))

        print("Response: ", end="", flush=True)

        # Display with delay
        while not complete.is_set() or buffer:
            if buffer:
                chunk = buffer.pop(0)
                for char in chunk:
                    print(char, end="", flush=True)
                    await asyncio.sleep(0.02)
            else:
                await asyncio.sleep(0.05)

        print("\n")
        await session.destroy()

    finally:
        await client.stop()


# =============================================================================
# Main
# =============================================================================


async def main():
    print("=" * 50)
    print("STREAMING RESPONSES")
    print("=" * 50)

    await basic_streaming()
    await streaming_with_progress()
    await typewriter_effect()

    # Skip interactive in automated runs
    if sys.stdin.isatty():
        run_interactive = input("\nRun interactive chat? (y/n): ").lower()
        if run_interactive == "y":
            await interactive_chat()

    print("\n" + "=" * 50)
    print("All demos completed!")


if __name__ == "__main__":
    asyncio.run(main())
