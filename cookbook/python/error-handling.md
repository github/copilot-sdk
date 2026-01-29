# Error Handling Patterns

Master error handling in your Copilot SDK applications with production-ready patterns.

> **Skill Level:** Beginner to Intermediate
>
> **Runnable Example:** [recipe/error_handling.py](recipe/error_handling.py)
>
> ```bash
> cd recipe && pip install -r requirements.txt
> python error_handling.py
> ```

## Overview

This recipe covers essential error handling patterns for building robust applications with the Copilot SDK:

- Basic try-except-finally patterns
- Context managers for automatic cleanup
- Retry logic with exponential backoff
- Timeout handling and request abortion
- Graceful shutdown with signal handling

## Quick Start

The simplest error handling pattern:

```python
import asyncio
from copilot import CopilotClient

async def main():
    client = CopilotClient()

    try:
        await client.start()
        session = await client.create_session()

        response = await session.send_and_wait(
            {"prompt": "Hello!"},
            timeout=30.0
        )

        await session.destroy()

    except FileNotFoundError:
        print("Copilot CLI not found. Please install it.")
    except ConnectionError:
        print("Could not connect to server.")
    except asyncio.TimeoutError:
        print("Request timed out.")
    except Exception as e:
        print(f"Error: {e}")
    finally:
        await client.stop()

asyncio.run(main())
```

## Error Types

### Common Exceptions

| Exception | Cause | Solution |
|-----------|-------|----------|
| `FileNotFoundError` | CLI not installed | Install Copilot CLI |
| `ConnectionError` | Network issues | Check connection |
| `asyncio.TimeoutError` | Request took too long | Increase timeout |
| `RuntimeError` | Protocol mismatch | Update SDK/CLI |

### Handling Specific Errors

```python
try:
    await client.start()
except FileNotFoundError:
    print("Install: https://github.com/github/copilot-cli")
except ConnectionError as e:
    print(f"Connection failed: {e}")
except RuntimeError as e:
    if "protocol version" in str(e).lower():
        print("Update your SDK or CLI to match versions.")
    else:
        raise
```

## Context Manager Pattern (Recommended)

Create a reusable context manager for automatic cleanup:

```python
class CopilotContext:
    """Automatic cleanup with context manager."""

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
        return False  # Don't suppress exceptions


# Usage
async def main():
    async with CopilotContext() as (client, session):
        await session.send_and_wait({"prompt": "Hello!"})
    # Automatic cleanup happens here
```

## Timeout Handling

### Using send_and_wait with Timeout

```python
try:
    # Wait up to 60 seconds
    response = await session.send_and_wait(
        {"prompt": "Complex question..."},
        timeout=60.0
    )
except asyncio.TimeoutError:
    print("Request timed out after 60 seconds")
```

### Aborting Long Requests

```python
async def abort_after_delay(session, seconds):
    await asyncio.sleep(seconds)
    await session.abort()
    print(f"Aborted after {seconds}s")

# Start abort task and request concurrently
abort_task = asyncio.create_task(abort_after_delay(session, 10))

try:
    await session.send({"prompt": "Long task..."})
    await abort_task
except Exception:
    pass
```

## Retry Pattern

Implement exponential backoff for transient failures:

```python
async def retry_with_backoff(
    func,
    max_retries=3,
    base_delay=1.0,
    max_delay=30.0,
):
    """Retry with exponential backoff."""
    for attempt in range(max_retries + 1):
        try:
            return await func()
        except (ConnectionError, asyncio.TimeoutError) as e:
            if attempt < max_retries:
                delay = min(base_delay * (2 ** attempt), max_delay)
                print(f"Retry {attempt + 1}/{max_retries} in {delay}s...")
                await asyncio.sleep(delay)
            else:
                raise


# Usage
response = await retry_with_backoff(
    lambda: session.send_and_wait({"prompt": "Hello!"}, timeout=30.0),
    max_retries=3
)
```

## Graceful Shutdown

Handle Ctrl+C and SIGTERM gracefully:

```python
import signal
import sys

class GracefulShutdown:
    def __init__(self, client):
        self.client = client
        self.shutdown_event = asyncio.Event()

    def register(self):
        loop = asyncio.get_running_loop()

        def handler(sig):
            print(f"\nReceived {sig.name}, shutting down...")
            self.shutdown_event.set()

        if sys.platform != "win32":
            for sig in (signal.SIGINT, signal.SIGTERM):
                loop.add_signal_handler(sig, lambda s=sig: handler(s))
        else:
            signal.signal(signal.SIGINT, lambda s, f: handler(signal.SIGINT))

    async def wait(self):
        await self.shutdown_event.wait()

    async def cleanup(self):
        errors = await self.client.stop()
        if errors:
            print(f"Cleanup errors: {errors}")


# Usage in a long-running application
async def main():
    client = CopilotClient()
    shutdown = GracefulShutdown(client)

    try:
        await client.start()
        shutdown.register()

        # Your application loop
        while not shutdown.shutdown_event.is_set():
            await asyncio.sleep(0.1)

    finally:
        await shutdown.cleanup()
```

## Best Practices

1. **Always clean up**: Use try-finally or context managers
2. **Set appropriate timeouts**: Match timeout to expected task duration
3. **Implement retries**: Handle transient network failures
4. **Log errors**: Capture details for debugging
5. **Validate early**: Check prerequisites before starting

## Complete Example

See the full implementation with all patterns:

```bash
python recipe/error_handling.py
```

The example demonstrates:
- Basic error handling
- Context manager pattern
- Retry with exponential backoff
- Timeout and abort patterns
- Graceful shutdown

## Next Steps

- [Multiple Sessions](multiple-sessions.md): Manage concurrent conversations
- [Persisting Sessions](persisting-sessions.md): Save and resume sessions
- [Custom Tools](custom-tools.md): Extend Copilot with your own functions
