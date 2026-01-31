# Copilot Python SDK

Python SDK for programmatic control of GitHub Copilot CLI via JSON-RPC.

> **Note:** This SDK is in technical preview and may change in breaking ways.

## Installation

```bash
pip install -e ".[dev]"
# or
uv pip install -e ".[dev]"
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
- `github_token` (str): GitHub token for authentication. When provided, takes priority over other auth methods.
- `use_logged_in_user` (bool): Whether to use logged-in user for authentication (default: True, but False when `github_token` is provided). Cannot be used with `cli_url`.

**SessionConfig Options (for `create_session`):**

- `model` (str): Model to use ("gpt-5", "claude-sonnet-4.5", etc.). **Required when using custom provider.**
- `reasoning_effort` (str): Reasoning effort level for models that support it ("low", "medium", "high", "xhigh"). Use `list_models()` to check which models support this option.
- `session_id` (str): Custom session ID
- `tools` (list): Custom tools exposed to the CLI
- `system_message` (dict): System message configuration
- `streaming` (bool): Enable streaming delta events
- `provider` (dict): Custom API provider configuration (BYOK). See [Custom Providers](#custom-providers) section.
- `infinite_sessions` (dict): Automatic context compaction configuration
- `on_user_input_request` (callable): Handler for user input requests from the agent (enables ask_user tool). See [User Input Requests](#user-input-requests) section.
- `hooks` (dict): Hook handlers for session lifecycle events. See [Session Hooks](#session-hooks) section.

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

## Infinite Sessions

By default, sessions use **infinite sessions** which automatically manage context window limits through background compaction and persist state to a workspace directory.

```python
# Default: infinite sessions enabled with default thresholds
session = await client.create_session({"model": "gpt-5"})

# Access the workspace path for checkpoints and files
print(session.workspace_path)
# => ~/.copilot/session-state/{session_id}/

# Custom thresholds
session = await client.create_session({
    "model": "gpt-5",
    "infinite_sessions": {
        "enabled": True,
        "background_compaction_threshold": 0.80,  # Start compacting at 80% context usage
        "buffer_exhaustion_threshold": 0.95,  # Block at 95% until compaction completes
    },
})

# Disable infinite sessions
session = await client.create_session({
    "model": "gpt-5",
    "infinite_sessions": {"enabled": False},
})
```

When enabled, sessions emit compaction events:

- `session.compaction_start` - Background compaction started
- `session.compaction_complete` - Compaction finished (includes token counts)

## Custom Providers

The SDK supports custom OpenAI-compatible API providers (BYOK - Bring Your Own Key), including local providers like Ollama. When using a custom provider, you must specify the `model` explicitly.

**ProviderConfig fields:**

- `type` (str): Provider type - `"openai"`, `"azure"`, or `"anthropic"` (default: `"openai"`)
- `base_url` (str): API endpoint URL (required)
- `api_key` (str): API key (optional for local providers like Ollama)
- `bearer_token` (str): Bearer token for authentication (takes precedence over `api_key`)
- `wire_api` (str): API format for OpenAI/Azure - `"completions"` or `"responses"` (default: `"completions"`)
- `azure` (dict): Azure-specific options with `api_version` (default: `"2024-10-21"`)

**Example with Ollama:**

```python
session = await client.create_session({
    "model": "deepseek-coder-v2:16b",  # Required when using custom provider
    "provider": {
        "type": "openai",
        "base_url": "http://localhost:11434/v1",  # Ollama endpoint
        # api_key not required for Ollama
    },
})

await session.send({"prompt": "Hello!"})
```

**Example with custom OpenAI-compatible API:**

```python
import os

session = await client.create_session({
    "model": "gpt-4",
    "provider": {
        "type": "openai",
        "base_url": "https://my-api.example.com/v1",
        "api_key": os.environ["MY_API_KEY"],
    },
})
```

**Example with Azure OpenAI:**

```python
import os

session = await client.create_session({
    "model": "gpt-4",
    "provider": {
        "type": "azure",  # Must be "azure" for Azure endpoints, NOT "openai"
        "base_url": "https://my-resource.openai.azure.com",  # Just the host, no path
        "api_key": os.environ["AZURE_OPENAI_KEY"],
        "azure": {
            "api_version": "2024-10-21",
        },
    },
})
```

> **Important notes:**
> - When using a custom provider, the `model` parameter is **required**. The SDK will throw an error if no model is specified.
> - For Azure OpenAI endpoints (`*.openai.azure.com`), you **must** use `type: "azure"`, not `type: "openai"`.
> - The `base_url` should be just the host (e.g., `https://my-resource.openai.azure.com`). Do **not** include `/openai/v1` in the URL - the SDK handles path construction automatically.

## User Input Requests

Enable the agent to ask questions to the user using the `ask_user` tool by providing an `on_user_input_request` handler:

```python
async def handle_user_input(request, invocation):
    # request["question"] - The question to ask
    # request.get("choices") - Optional list of choices for multiple choice
    # request.get("allowFreeform", True) - Whether freeform input is allowed
    
    print(f"Agent asks: {request['question']}")
    if request.get("choices"):
        print(f"Choices: {', '.join(request['choices'])}")
    
    # Return the user's response
    return {
        "answer": "User's answer here",
        "wasFreeform": True,  # Whether the answer was freeform (not from choices)
    }

session = await client.create_session({
    "model": "gpt-5",
    "on_user_input_request": handle_user_input,
})
```

## Session Hooks

Hook into session lifecycle events by providing handlers in the `hooks` configuration:

```python
async def on_pre_tool_use(input, invocation):
    print(f"About to run tool: {input['toolName']}")
    # Return permission decision and optionally modify args
    return {
        "permissionDecision": "allow",  # "allow", "deny", or "ask"
        "modifiedArgs": input.get("toolArgs"),  # Optionally modify tool arguments
        "additionalContext": "Extra context for the model",
    }

async def on_post_tool_use(input, invocation):
    print(f"Tool {input['toolName']} completed")
    return {
        "additionalContext": "Post-execution notes",
    }

async def on_user_prompt_submitted(input, invocation):
    print(f"User prompt: {input['prompt']}")
    return {
        "modifiedPrompt": input["prompt"],  # Optionally modify the prompt
    }

async def on_session_start(input, invocation):
    print(f"Session started from: {input['source']}")  # "startup", "resume", "new"
    return {
        "additionalContext": "Session initialization context",
    }

async def on_session_end(input, invocation):
    print(f"Session ended: {input['reason']}")

async def on_error_occurred(input, invocation):
    print(f"Error in {input['errorContext']}: {input['error']}")
    return {
        "errorHandling": "retry",  # "retry", "skip", or "abort"
    }

session = await client.create_session({
    "model": "gpt-5",
    "hooks": {
        "on_pre_tool_use": on_pre_tool_use,
        "on_post_tool_use": on_post_tool_use,
        "on_user_prompt_submitted": on_user_prompt_submitted,
        "on_session_start": on_session_start,
        "on_session_end": on_session_end,
        "on_error_occurred": on_error_occurred,
    },
})
```

**Available hooks:**

- `on_pre_tool_use` - Intercept tool calls before execution. Can allow/deny or modify arguments.
- `on_post_tool_use` - Process tool results after execution. Can modify results or add context.
- `on_user_prompt_submitted` - Intercept user prompts. Can modify the prompt before processing.
- `on_session_start` - Run logic when a session starts or resumes.
- `on_session_end` - Cleanup or logging when session ends.
- `on_error_occurred` - Handle errors with retry/skip/abort strategies.

## Requirements

- Python 3.9+
- GitHub Copilot CLI installed and accessible
