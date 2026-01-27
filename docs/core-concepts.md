# Core Concepts

This document covers the fundamental concepts shared across all SDKs. For language-specific quickstarts, see [Getting Started](getting-started.md).

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                      Your Application                        │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      CopilotClient                           │
│  - Lifecycle management (start/stop)                         │
│  - Session creation and resumption                           │
│  - Protocol handling                                         │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      CopilotSession                          │
│  - Message sending (send / send_and_wait)                    │
│  - Event subscriptions                                       │
│  - Tool and agent registration                               │
│  - Permission handling                                       │
└─────────────────────────────────────────────────────────────┘
                              │
              ┌───────────────┼───────────────┐
              ▼               ▼               ▼
        ┌─────────┐    ┌──────────┐    ┌──────────┐
        │  Tools  │    │   MCP    │    │  Agents  │
        └─────────┘    └──────────┘    └──────────┘
```

## Session Lifecycle

Sessions are the core unit of interaction. Each session maintains its own conversation context.

```
create_session()          resume_session()
      │                         │
      └──────────┬──────────────┘
                 ▼
       ┌─────────────────┐
       │  Active Session │  ◀─── send() / send_and_wait()
       └────────┬────────┘       on() event handlers
                │
                ▼ destroy()
       ┌─────────────────┐
       │   Saved State   │  ◀─── Can be resumed later
       └────────┬────────┘
                │
                ▼ delete_session()
             [Removed]
```

### Key Operations

| Operation | Description |
|-----------|-------------|
| `create_session()` | Create a new conversation |
| `resume_session(id)` | Restore a saved conversation |
| `destroy()` | Save and disconnect (data preserved) |
| `delete_session(id)` | Permanently remove session data |
| `list_sessions()` | List all saved sessions |

## Event Types

All SDKs emit the same event types. Subscribe with `session.on(handler)`.

| Event Type | Description | Data Fields |
|------------|-------------|-------------|
| `user.message` | User message sent | `content` |
| `assistant.message` | Complete assistant response | `content` |
| `assistant.message_delta` | Streaming token (when streaming enabled) | `delta_content` |
| `tool.execution_start` | Tool invocation started | `tool_name`, `tool_call_id` |
| `tool.execution_complete` | Tool finished | `tool_call_id`, `result` |
| `session.idle` | Session ready for next input | - |
| `session.error` | Error occurred | `message` |

### Event Handler Pattern

```python
# Python
def handler(event):
    if event.type == "assistant.message":
        print(event.data.content)
    elif event.type == "tool.execution_start":
        print(f"Running: {event.data.tool_name}")

session.on(handler)
```

```typescript
// TypeScript
session.on((event) => {
    if (event.type === "assistant.message") {
        console.log(event.data.content);
    }
});
```

## Sending Messages

Two methods for sending messages:

| Method | Behavior |
|--------|----------|
| `send()` | Fire and forget, returns immediately |
| `send_and_wait()` | Blocks until response complete |

```python
# Fire and forget
await session.send({"prompt": "Hello"})

# Wait for completion
await session.send_and_wait({"prompt": "Hello"}, timeout=60.0)
```

## Error Handling

Common exceptions across all SDKs:

| Exception | Cause | Solution |
|-----------|-------|----------|
| `FileNotFoundError` | CLI not installed | Install Copilot CLI |
| `ConnectionError` | Network issues | Check connection |
| `TimeoutError` | Request exceeded timeout | Increase timeout value |
| `RuntimeError` | Protocol version mismatch | Update SDK and CLI |

### Standard Pattern

```python
try:
    await client.start()
    session = await client.create_session()
    await session.send_and_wait({"prompt": "Hello"}, timeout=30.0)
except FileNotFoundError:
    print("Install Copilot CLI first")
except ConnectionError:
    print("Check network connection")
except asyncio.TimeoutError:
    print("Request timed out")
finally:
    await client.stop()
```

## Custom Tools

Tools let Copilot call your code. Define with a schema and handler.

### Tool Definition Pattern

1. **Define parameters** with a schema (Pydantic, TypeScript interface, etc.)
2. **Register handler** function that receives parsed parameters
3. **Return result** as string or structured data

```python
# Python with Pydantic
class WeatherParams(BaseModel):
    city: str = Field(description="City name")

@define_tool(description="Get weather for a city")
def get_weather(params):
    return f"Weather in {params.city}: 72°F, sunny"
```

### Tool Lifecycle

```
1. User asks question
2. Copilot decides to call tool
3. SDK validates parameters
4. Your handler executes
5. Result sent back to Copilot
6. Copilot incorporates result in response
```

## MCP Servers

Model Context Protocol (MCP) servers provide external tool integrations.

### Configuration

```python
session = await client.create_session({
    "mcp_servers": {
        "github": {
            "type": "stdio",
            "command": "npx",
            "args": ["-y", "@modelcontextprotocol/server-github"],
            "env": {"GITHUB_TOKEN": os.environ["GITHUB_TOKEN"]},
            "tools": ["*"],  # Allow all tools
        }
    }
})
```

### Common MCP Servers

| Server | Tools | Install |
|--------|-------|---------|
| `server-filesystem` | File read/write | `npx -y @modelcontextprotocol/server-filesystem /path` |
| `server-github` | GitHub API | `npx -y @modelcontextprotocol/server-github` |
| `server-sqlite` | Database queries | `npx -y @modelcontextprotocol/server-sqlite /path/to/db` |

See [MCP Documentation](mcp.md) for detailed configuration.

## Custom Agents

Agents are specialized assistants with custom prompts, tools, and behaviors.

```python
from copilot.types import CustomAgentConfig

agent = CustomAgentConfig(
    name="code-reviewer",
    display_name="Code Reviewer",
    description="Reviews code for bugs and style",
    prompt="You are an expert code reviewer...",
    tools=["search_cve"],  # Restrict to specific tools
    infer=True,  # Enable automatic selection
)

session = await client.create_session({"custom_agents": [agent]})
```

Use agents with `@agent-name` in prompts:

```
@code-reviewer Review this Python code for security issues
```

## Custom Providers (BYOK)

Bring Your Own Key to use different AI providers.

```python
from copilot import ProviderConfig

provider = ProviderConfig(
    type="openai",
    base_url="https://api.openai.com/v1",
    api_key=os.environ["OPENAI_API_KEY"],
)

session = await client.create_session({"provider": provider})
```

Supported provider types: `openai`, `azure`, `anthropic`

## Streaming

Enable real-time response streaming:

```python
session = await client.create_session({"streaming": True})

def handler(event):
    if event.type == "assistant.message_delta":
        print(event.data.delta_content, end="", flush=True)

session.on(handler)
```

## Infinite Sessions

For long-running conversations, enable automatic context management:

```python
session = await client.create_session({
    "session_id": "long-conversation",
    "infinite_sessions": {
        "enabled": True,
        "background_compaction_threshold": 0.80,
        "buffer_exhaustion_threshold": 0.95,
    }
})
```

The session automatically compacts context when limits are reached.

## Next Steps

- [Getting Started Tutorial](getting-started.md) - Build your first app
- [MCP Configuration](mcp.md) - Detailed MCP server setup
- Language-specific cookbooks for practical examples
