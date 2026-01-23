# Copilot Python SDK

Python SDK for programmatic control of GitHub Copilot CLI via JSON-RPC.

> **Note:** This SDK is in technical preview and may change in breaking ways.

## Installation

```bash
pip install -e .
```

## Quick Start

```python
import asyncio
from copilot import CopilotClient

async def main():
    # Create and start client
    client = CopilotClient()
    await client.start()

    # Create a session
    session = await client.create_session({"model": "gpt-5"})

    # Wait for response using session.idle event
    done = asyncio.Event()

    def on_event(event):
        if event.type.value == "assistant.message":
            print(event.data.content)
        elif event.type.value == "session.idle":
            done.set()

    session.on(on_event)

    # Send a message and wait for completion
    await session.send({"prompt": "What is 2+2?"})
    await done.wait()

    # Clean up
    await session.destroy()
    await client.stop()

asyncio.run(main())
```

## Features

- ✅ Full JSON-RPC protocol support
- ✅ stdio and TCP transports
- ✅ Real-time streaming events
- ✅ Session history with `get_messages()`
- ✅ Type hints throughout
- ✅ Async/await native

## API Reference

### CopilotClient

```python
client = CopilotClient({
    "cli_path": "copilot",  # Optional: path to CLI executable
    "cli_url": None,        # Optional: URL of existing server (e.g., "localhost:8080")
    "log_level": "info",    # Optional: log level (default: "info")
    "auto_start": True,     # Optional: auto-start server (default: True)
    "auto_restart": True,   # Optional: auto-restart on crash (default: True)
})
await client.start()

session = await client.create_session({"model": "gpt-5"})

def on_event(event):
    print(f"Event: {event['type']}")

session.on(on_event)
await session.send({"prompt": "Hello!"})

# ... wait for events ...

await session.destroy()
await client.stop()
```

**CopilotClient Options:**

- `cli_path` (str): Path to CLI executable (default: "copilot" or `COPILOT_CLI_PATH` env var)
- `cli_url` (str): URL of existing CLI server (e.g., `"localhost:8080"`, `"http://127.0.0.1:9000"`, or just `"8080"`). When provided, the client will not spawn a CLI process.
- `cwd` (str): Working directory for CLI process
- `port` (int): Server port for TCP mode (default: 0 for random)
- `use_stdio` (bool): Use stdio transport instead of TCP (default: True)
- `log_level` (str): Log level (default: "info")
- `auto_start` (bool): Auto-start server on first use (default: True)
- `auto_restart` (bool): Auto-restart on crash (default: True)

### Tools

Define tools with automatic JSON schema generation using the `@define_tool` decorator and Pydantic models:

```python
from pydantic import BaseModel, Field
from copilot import CopilotClient, define_tool

class LookupIssueParams(BaseModel):
    id: str = Field(description="Issue identifier")

@define_tool(description="Fetch issue details from our tracker")
async def lookup_issue(params: LookupIssueParams) -> str:
    issue = await fetch_issue(params.id)
    return issue.summary

session = await client.create_session({
    "model": "gpt-5",
    "tools": [lookup_issue],
})
```

> **Note:** When using `from __future__ import annotations`, define Pydantic models at module level (not inside functions).

**Low-level API (without Pydantic):**

For users who prefer manual schema definition:

```python
from copilot import CopilotClient, Tool

async def lookup_issue(invocation):
    issue_id = invocation["arguments"]["id"]
    issue = await fetch_issue(issue_id)
    return {
        "textResultForLlm": issue.summary,
        "resultType": "success",
        "sessionLog": f"Fetched issue {issue_id}",
    }

session = await client.create_session({
    "model": "gpt-5",
    "tools": [
        Tool(
            name="lookup_issue",
            description="Fetch issue details from our tracker",
            parameters={
                "type": "object",
                "properties": {
                    "id": {"type": "string", "description": "Issue identifier"},
                },
                "required": ["id"],
            },
            handler=lookup_issue,
        )
    ],
})
```

The SDK automatically handles `tool.call`, executes your handler (sync or async), and responds with the final result when the tool completes.

## Image Support

The SDK supports image attachments via the `attachments` parameter. You can attach images by providing their file path:

```python
await session.send({
    "prompt": "What's in this image?",
    "attachments": [
        {
            "type": "file",
            "path": "/path/to/image.jpg",
        }
    ]
})
```

Supported image formats include JPG, PNG, GIF, and other common image types. The agent's `view` tool can also read images directly from the filesystem, so you can also ask questions like:

```python
await session.send({"prompt": "What does the most recent jpg in this directory portray?"})
```

## Streaming

Enable streaming to receive assistant response chunks as they're generated:

```python
import asyncio
from copilot import CopilotClient

async def main():
    client = CopilotClient()
    await client.start()

    session = await client.create_session({
        "model": "gpt-5",
        "streaming": True
    })

    # Use asyncio.Event to wait for completion
    done = asyncio.Event()

    def on_event(event):
        if event.type.value == "assistant.message_delta":
            # Streaming message chunk - print incrementally
            delta = event.data.delta_content or ""
            print(delta, end="", flush=True)
        elif event.type.value == "assistant.reasoning_delta":
            # Streaming reasoning chunk (if model supports reasoning)
            delta = event.data.delta_content or ""
            print(delta, end="", flush=True)
        elif event.type.value == "assistant.message":
            # Final message - complete content
            print("\n--- Final message ---")
            print(event.data.content)
        elif event.type.value == "assistant.reasoning":
            # Final reasoning content (if model supports reasoning)
            print("--- Reasoning ---")
            print(event.data.content)
        elif event.type.value == "session.idle":
            # Session finished processing
            done.set()

    session.on(on_event)
    await session.send({"prompt": "Tell me a short story"})
    await done.wait()  # Wait for streaming to complete

    await session.destroy()
    await client.stop()

asyncio.run(main())
```

When `streaming=True`:

- `assistant.message_delta` events are sent with `delta_content` containing incremental text
- `assistant.reasoning_delta` events are sent with `delta_content` for reasoning/chain-of-thought (model-dependent)
- Accumulate `delta_content` values to build the full response progressively
- The final `assistant.message` and `assistant.reasoning` events contain the complete content

Note: `assistant.message` and `assistant.reasoning` (final events) are always sent regardless of streaming setting.

## Requirements

- Python 3.9+
- GitHub Copilot CLI installed and accessible
