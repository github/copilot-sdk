#!/usr/bin/env python3
"""
Error Handling Patterns for the Copilot SDK.

Run: python error_handling.py
"""

import asyncio
import signal
import sys

from copilot import CopilotClient
from copilot.types import SessionEventType


# =============================================================================
# Basic Error Handling
# =============================================================================


async def basic_error_handling():
    """Simple try-except-finally pattern."""
    print("\n=== Basic Error Handling ===\n")

    client = CopilotClient()
    session = None
    response = None

    def handle_event(event):
        nonlocal response
        if event.type == SessionEventType.ASSISTANT_MESSAGE:
            response = event.data.content

    try:
        await client.start()
        print("✓ Client connected")

        session = await client.create_session()
        session.on(handle_event)
        print(f"✓ Session: {session.session_id[:12]}...")

        await session.send_and_wait({"prompt": "Say hello."}, timeout=30.0)

        if response:
            print(f"✓ Response: {response}")

    except FileNotFoundError:
        print("✗ Copilot CLI not found")

    except ConnectionError as e:
        print(f"✗ Connection Error: {e}")

    except asyncio.TimeoutError:
        print("✗ Request timed out")

    except Exception as e:
        print(f"✗ Error: {type(e).__name__}: {e}")

    finally:
        if session:
            try:
                await session.destroy()
            except Exception:
                pass
        await client.stop()
        print("✓ Cleanup complete")


# =============================================================================
# Context Manager Pattern
# =============================================================================


class CopilotContext:
    """Context manager for automatic cleanup."""

    def __init__(self, **options):
        self.client = CopilotClient(options if options else None)
        self.session = None

    async def __aenter__(self):
        await self.client.start()
        self.session = await self.client.create_session()
        return self.client, self.session

    async def __aexit__(self, exc_type, exc_val, exc_tb):
        if self.session:
            try:
                await self.session.destroy()
            except Exception:
                pass
        await self.client.stop()
        return False


async def context_manager_demo():
    """Use context manager for automatic cleanup."""
    print("\n=== Context Manager Pattern ===\n")

    try:
        async with CopilotContext() as (client, session):
            print(f"✓ Session: {session.session_id[:12]}...")
            await session.send_and_wait({"prompt": "What is 2+2?"}, timeout=30.0)
            print("✓ Request completed")

        print("✓ Auto cleanup done")

    except Exception as e:
        print(f"✗ Error: {e}")


# =============================================================================
# Retry with Backoff
# =============================================================================


async def retry_with_backoff(func, max_retries=3, base_delay=1.0):
    """Retry with exponential backoff."""
    last_error = None

    for attempt in range(max_retries + 1):
        try:
            return await func()
        except (ConnectionError, asyncio.TimeoutError) as e:
            last_error = e
            if attempt < max_retries:
                delay = min(base_delay * (2 ** attempt), 30.0)
                print(f"  Retry {attempt + 1}/{max_retries} in {delay:.1f}s...")
                await asyncio.sleep(delay)

    if last_error is None:
        raise RuntimeError("Max retries exceeded without a captured error.")
    raise last_error


async def retry_demo():
    """Demonstrate retry pattern."""
    print("\n=== Retry Pattern ===\n")

    async def make_request():
        async with CopilotContext() as (_, session):
            await session.send_and_wait({"prompt": "Hello!"}, timeout=30.0)
            return "Success!"

    try:
        result = await retry_with_backoff(make_request)
        print(f"✓ {result}")
    except Exception as e:
        print(f"✗ All retries failed: {e}")


# =============================================================================
# Timeout and Abort
# =============================================================================


async def timeout_demo():
    """Handle timeouts with abort."""
    print("\n=== Timeout Handling ===\n")

    client = CopilotClient()

    try:
        await client.start()
        session = await client.create_session()

        try:
            await session.send_and_wait(
                {"prompt": "Write a long essay."},
                timeout=5.0
            )
            print("✓ Completed")

        except asyncio.TimeoutError:
            print("⚠ Timeout - aborting...")
            await session.abort()
            print("✓ Aborted")

        await session.destroy()

    finally:
        await client.stop()


# =============================================================================
# Graceful Shutdown
# =============================================================================


class GracefulShutdown:
    """Handle Ctrl+C gracefully."""

    def __init__(self):
        self.shutdown_event = asyncio.Event()
        self.client = None
        self.session = None

    def setup_signals(self):
        def handler(sig, frame=None):
            print("\n⚠ Shutdown requested...")
            self.shutdown_event.set()

        signal.signal(signal.SIGINT, handler)
        if sys.platform != "win32":
            signal.signal(signal.SIGTERM, handler)

    async def run(self):
        self.setup_signals()

        self.client = CopilotClient()
        await self.client.start()
        self.session = await self.client.create_session()

        print("Running... Press Ctrl+C to stop")

        try:
            while not self.shutdown_event.is_set():
                await asyncio.sleep(0.5)
        finally:
            await self.cleanup()

    async def cleanup(self):
        if self.session:
            await self.session.destroy()
        if self.client:
            await self.client.stop()
        print("✓ Shutdown complete")


async def graceful_shutdown_demo():
    """Show graceful shutdown pattern."""
    print("\n=== Graceful Shutdown Pattern ===\n")
    print("See GracefulShutdown class for implementation.")
    print("✓ Pattern documented")


# =============================================================================
# Main
# =============================================================================


async def main():
    print("=" * 50)
    print("ERROR HANDLING PATTERNS")
    print("=" * 50)

    await basic_error_handling()
    await context_manager_demo()
    await retry_demo()
    await timeout_demo()
    await graceful_shutdown_demo()

    print("\n" + "=" * 50)
    print("All demos completed!")


if __name__ == "__main__":
    asyncio.run(main())
