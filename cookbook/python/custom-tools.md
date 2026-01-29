# Custom Tools

Create custom tools to extend Copilot's capabilities with your own functionality.

> **Skill Level:** Intermediate to Advanced
>
> **Runnable Example:** [recipe/custom_tools.py](recipe/custom_tools.py)
>
> ```bash
> cd recipe && pip install -r requirements.txt
> python custom_tools.py
> ```

## Overview

This recipe covers custom tool development:

- Basic tool definition with `@define_tool`
- Pydantic models for parameter validation
- Async handlers for non-blocking operations
- Structured results with `ToolResult`
- Tool orchestration patterns

## Quick Start

```python
import asyncio
from copilot import CopilotClient, define_tool

# Define a simple tool
@define_tool(
    name="get_weather",
    description="Get the current weather for a location"
)
def get_weather(location: str) -> str:
    # In production, call a real weather API
    return f"Weather in {location}: 72°F, Sunny"

async def main():
    client = CopilotClient()
    await client.start()

    session = await client.create_session({
        "tools": [get_weather]  # Register the tool
    })

    await session.send_and_wait({
        "prompt": "What's the weather in San Francisco?"
    })
    # Copilot will call your get_weather tool!

    await session.destroy()
    await client.stop()

asyncio.run(main())
```

## Tool Definition Patterns

### Basic Tool

Simple function with type hints:

```python
@define_tool(
    name="calculate_tax",
    description="Calculate sales tax for an amount"
)
def calculate_tax(amount: float, rate: float = 0.0825) -> float:
    """Calculate tax. Rate defaults to 8.25%."""
    return round(amount * rate, 2)
```

### Pydantic Model Parameters

Use Pydantic for complex input validation:

```python
from pydantic import BaseModel, Field
from typing import Literal

class CreateTicketParams(BaseModel):
    title: str = Field(description="Ticket title")
    description: str = Field(description="Detailed description")
    priority: Literal["low", "medium", "high"] = Field(
        default="medium",
        description="Ticket priority"
    )
    assignee: str | None = Field(default=None)

@define_tool(
    name="create_ticket",
    description="Create a support ticket in the system"
)
def create_ticket(params: CreateTicketParams) -> dict:
    return {
        "id": "TICKET-123",
        "title": params.title,
        "priority": params.priority,
        "status": "created"
    }
```

### Async Handlers

For I/O-bound operations:

```python
import aiohttp

@define_tool(
    name="fetch_api",
    description="Fetch data from an API endpoint"
)
async def fetch_api(url: str, method: str = "GET") -> dict:
    async with aiohttp.ClientSession() as session:
        async with session.request(method, url) as response:
            return {
                "status": response.status,
                "data": await response.json()
            }
```

## Structured Results

Use `ToolResult` for rich responses:

```python
from copilot import ToolResult

@define_tool(
    name="analyze_code",
    description="Analyze code for issues"
)
def analyze_code(code: str, language: str) -> ToolResult:
    issues = [
        {"line": 5, "severity": "warning", "message": "Unused variable"},
        {"line": 12, "severity": "error", "message": "Syntax error"}
    ]

    return ToolResult(
        content=f"Found {len(issues)} issues",
        structured_data={"issues": issues, "language": language},
        is_error=any(i["severity"] == "error" for i in issues)
    )
```

## Multiple Tools

Register multiple tools together:

```python
# Define tools
@define_tool(name="search_docs", description="Search documentation")
def search_docs(query: str) -> list:
    return ["Result 1", "Result 2"]

@define_tool(name="get_examples", description="Get code examples")
def get_examples(topic: str) -> list:
    return [f"Example for {topic}"]

@define_tool(name="run_tests", description="Run test suite")
async def run_tests(test_path: str) -> dict:
    return {"passed": 10, "failed": 0}

# Register all tools
session = await client.create_session({
    "tools": [search_docs, get_examples, run_tests]
})
```

## Tool Categories

### Database Tools

```python
@define_tool(
    name="query_database",
    description="Execute a SQL query"
)
async def query_database(query: str, database: str = "main") -> dict:
    # Use async database driver
    return {"rows": [], "count": 0}

@define_tool(
    name="insert_record",
    description="Insert a record into a table"
)
async def insert_record(table: str, data: dict) -> dict:
    return {"id": 1, "success": True}
```

### File System Tools

```python
import os

@define_tool(
    name="list_files",
    description="List files in a directory"
)
def list_files(path: str, pattern: str = "*") -> list:
    import glob
    return glob.glob(os.path.join(path, pattern))

@define_tool(
    name="read_file_info",
    description="Get file metadata"
)
def read_file_info(path: str) -> dict:
    stat = os.stat(path)
    return {
        "size": stat.st_size,
        "modified": stat.st_mtime,
        "is_directory": os.path.isdir(path)
    }
```

### HTTP Tools

```python
@define_tool(
    name="http_request",
    description="Make an HTTP request"
)
async def http_request(
    url: str,
    method: str = "GET",
    headers: dict = None,
    body: dict = None
) -> dict:
    import aiohttp

    async with aiohttp.ClientSession() as session:
        async with session.request(
            method, url,
            headers=headers,
            json=body
        ) as response:
            return {
                "status": response.status,
                "headers": dict(response.headers),
                "body": await response.text()
            }
```

## Event Handling

Monitor tool execution:

```python
from copilot.types import SessionEventType

def create_tool_handler():
    """Track tool execution events."""
    def handler(event):
        if event.type == SessionEventType.TOOL_EXECUTION_START:
            print(f"🔧 Starting: {event.data.tool_name}")
            print(f"   Args: {event.data.arguments}")

        elif event.type == SessionEventType.TOOL_EXECUTION_COMPLETE:
            print(f"✅ Completed")

        elif event.type == SessionEventType.SESSION_ERROR:
            print(f"❌ Error: {event.data.message}")

    return handler

session.on(create_tool_handler())
```

## Error Handling

Handle tool errors gracefully:

```python
@define_tool(
    name="risky_operation",
    description="An operation that might fail"
)
def risky_operation(input_data: str) -> ToolResult:
    try:
        result = process(input_data)
        return ToolResult(
            content=f"Success: {result}",
            is_error=False
        )
    except ValueError as e:
        return ToolResult(
            content=f"Invalid input: {e}",
            is_error=True
        )
    except Exception as e:
        return ToolResult(
            content=f"Unexpected error: {e}",
            is_error=True
        )
```

## Tool Orchestration

Combine multiple tools in a workflow:

```python
async def workflow_demo(session):
    """Demonstrate tool orchestration."""

    # Single prompt triggers multiple tool calls
    await session.send_and_wait({
        "prompt": """
1. Search the documentation for 'authentication'
2. Get code examples for the results
3. Run the test suite for auth tests
4. Summarize the findings
"""
    })
    # Copilot will call search_docs → get_examples → run_tests
```

## Best Practices

| Practice | Description |
|----------|-------------|
| Clear names | Use descriptive, action-oriented names |
| Good descriptions | Help Copilot understand when to use the tool |
| Type hints | Always include type annotations |
| Pydantic models | Use for complex parameters |
| Error handling | Return ToolResult with is_error=True |
| Async for I/O | Use async for network/file operations |

## Complete Example

```bash
python recipe/custom_tools.py
```

Demonstrates:
- Basic and advanced tool definitions
- Pydantic parameter validation
- Async handlers
- Tool orchestration

## Next Steps

- [MCP Servers](mcp-servers.md): Use external tool servers
- [Custom Agents](custom-agents.md): Create specialized agents with tools
- [Streaming Responses](streaming-responses.md): Stream tool results
