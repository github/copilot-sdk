# Streaming Responses

Handle real-time streaming for progressive output and better user experience.

> **Skill Level:** Intermediate
>
> **Runnable Example:** [recipe/streaming_responses.py](recipe/streaming_responses.py)
>
> ```bash
> cd recipe && pip install -r requirements.txt
> python streaming_responses.py
> ```

## Overview

This recipe covers streaming patterns:

- Basic streaming with `send()` and events
- Progress indicators during generation
- Typewriter effect for chat UIs
- Parallel streaming from multiple sessions
- Chunk processing and aggregation

## Quick Start

```python
import asyncio
from copilot import CopilotClient

async def main():
    client = CopilotClient()
    await client.start()

    session = await client.create_session()

    # Stream handler for real-time output
    def on_stream(event):
        if event.type == "assistant.message.delta":
            # Print each chunk as it arrives
            print(event.data.content, end="", flush=True)
        elif event.type == "assistant.message":
            print()  # Newline after complete message

    session.on(on_stream)

    # send() returns immediately, events stream in
    await session.send({"prompt": "Write a haiku about Python programming"})

    # Wait for completion
    await asyncio.sleep(5)

    await session.destroy()
    await client.stop()

asyncio.run(main())
```

## Streaming Events

| Event Type | Description | Data |
|------------|-------------|------|
| `assistant.message.delta` | Partial content chunk | `content` (string) |
| `assistant.message` | Complete message | Full `content` |
| `tool.execution_start` | Tool starting | `tool_name`, `arguments` |
| `tool.execution_complete` | Tool finished | `result` |
| `turn.complete` | Full turn done | Turn metadata |

## Basic Streaming

Stream responses with full event handling:

```python
async def stream_response(session, prompt):
    """Stream a response with progress tracking."""
    chunks = []
    complete = asyncio.Event()

    def handler(event):
        if event.type == "assistant.message.delta":
            chunks.append(event.data.content)
            print(event.data.content, end="", flush=True)

        elif event.type == "assistant.message":
            print()  # Newline
            complete.set()

        elif event.type == "error":
            print(f"\nError: {event.data.message}")
            complete.set()

    session.on(handler)
    await session.send({"prompt": prompt})
    await complete.wait()

    return "".join(chunks)
```

## Progress Indicators

Show progress during generation:

```python
async def stream_with_progress(session, prompt):
    """Stream with visual progress indicator."""
    import sys

    chunk_count = 0
    spinner = ['⠋', '⠙', '⠹', '⠸', '⠼', '⠴', '⠦', '⠧', '⠇', '⠏']

    def handler(event):
        nonlocal chunk_count

        if event.type == "assistant.message.delta":
            chunk_count += 1
            # Show spinner
            sys.stdout.write(f"\r{spinner[chunk_count % len(spinner)]} Generating...")
            sys.stdout.flush()

        elif event.type == "assistant.message":
            sys.stdout.write(f"\r✅ Complete ({chunk_count} chunks)\n\n")
            print(event.data.content)

        elif event.type == "tool.execution_start":
            print(f"\n🔧 Running {event.data.tool_name}...")

    session.on(handler)
    await session.send_and_wait({"prompt": prompt})
```

## Typewriter Effect

Create a chat-like typing experience:

```python
async def typewriter_effect(session, prompt, delay=0.02):
    """Display response with typewriter effect."""
    complete = asyncio.Event()

    async def display_chunk(text):
        for char in text:
            print(char, end="", flush=True)
            await asyncio.sleep(delay)

    buffer = []
    display_task = None

    def handler(event):
        nonlocal display_task

        if event.type == "assistant.message.delta":
            buffer.append(event.data.content)

        elif event.type == "assistant.message":
            complete.set()

    session.on(handler)
    await session.send({"prompt": prompt})

    # Display buffered content with delay
    while not complete.is_set() or buffer:
        if buffer:
            chunk = buffer.pop(0)
            await display_chunk(chunk)
        else:
            await asyncio.sleep(0.01)

    print()  # Final newline
```

## Parallel Streaming

Stream from multiple sessions simultaneously:

```python
async def parallel_streaming():
    """Stream responses from multiple sessions in parallel."""
    client = CopilotClient()
    await client.start()

    topics = ["Python", "JavaScript", "Rust"]
    sessions = []
    results = {topic: [] for topic in topics}
    events = {topic: asyncio.Event() for topic in topics}

    # Create sessions with handlers
    for topic in topics:
        session = await client.create_session()

        def make_handler(t):
            def handler(event):
                if event.type == "assistant.message.delta":
                    results[t].append(event.data.content)
                elif event.type == "assistant.message":
                    events[t].set()
            return handler

        session.on(make_handler(topic))
        sessions.append((topic, session))

    # Send all prompts
    for topic, session in sessions:
        await session.send({"prompt": f"Describe {topic} in 2 sentences"})

    # Wait for all to complete
    await asyncio.gather(*[e.wait() for e in events.values()])

    # Print results
    for topic in topics:
        print(f"\n{topic}: {''.join(results[topic])}")

    # Cleanup
    for _, session in sessions:
        await session.destroy()
    await client.stop()
```

## Stream Aggregation

Collect and process streamed content:

```python
class StreamAggregator:
    """Aggregate streaming content for processing."""

    def __init__(self):
        self.chunks = []
        self.complete = asyncio.Event()
        self.tool_calls = []

    def handler(self, event):
        if event.type == "assistant.message.delta":
            self.chunks.append(event.data.content)

        elif event.type == "assistant.message":
            self.complete.set()

        elif event.type == "tool.execution_complete":
            self.tool_calls.append({
                "id": event.data.tool_call_id,
                "result": event.data.result
            })

    @property
    def content(self):
        return "".join(self.chunks)

    async def wait(self, timeout=60.0):
        await asyncio.wait_for(self.complete.wait(), timeout)


# Usage
aggregator = StreamAggregator()
session.on(aggregator.handler)
await session.send({"prompt": "Analyze this code..."})
await aggregator.wait()
print(f"Response: {aggregator.content}")
print(f"Tools called: {len(aggregator.tool_calls)}")
```

## Rich Console Output

Use rich library for enhanced display:

```python
from rich.console import Console
from rich.live import Live
from rich.markdown import Markdown

async def rich_streaming(session, prompt):
    """Stream with rich formatting."""
    console = Console()
    content = []

    def handler(event):
        if event.type == "assistant.message.delta":
            content.append(event.data.content)

    session.on(handler)

    with Live(console=console, refresh_per_second=10) as live:
        await session.send({"prompt": prompt})

        while True:
            # Update display with markdown
            live.update(Markdown("".join(content)))
            await asyncio.sleep(0.1)

            if session.is_idle:
                break
```

## Timeout Handling

Handle slow or stalled streams:

```python
async def stream_with_timeout(session, prompt, timeout=30.0):
    """Stream with timeout protection."""
    complete = asyncio.Event()
    last_chunk_time = asyncio.get_event_loop().time()

    def handler(event):
        nonlocal last_chunk_time
        last_chunk_time = asyncio.get_event_loop().time()

        if event.type == "assistant.message":
            complete.set()

    session.on(handler)
    await session.send({"prompt": prompt})

    try:
        await asyncio.wait_for(complete.wait(), timeout=timeout)
    except asyncio.TimeoutError:
        print(f"Stream timed out after {timeout}s")
        await session.abort()  # Cancel the generation
```

## Best Practices

1. **Use `send()` for streaming**: Returns immediately, events stream in
2. **Handle all event types**: Don't just handle deltas, handle errors too
3. **Buffer appropriately**: Don't overwhelm the display
4. **Set timeouts**: Protect against stalled streams
5. **Clean up handlers**: Remove handlers when done if reusing sessions

## Complete Example

```bash
python recipe/streaming_responses.py
```

Demonstrates:
- Basic streaming
- Progress indicators
- Typewriter effect
- Parallel streaming

## Next Steps

- [Error Handling](error-handling.md): Handle streaming errors
- [Custom Tools](custom-tools.md): Stream tool results
- [Multiple Sessions](multiple-sessions.md): Parallel streaming patterns
