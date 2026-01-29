#!/usr/bin/env python3
"""
Custom Tools - Defining and using custom tools with the Copilot SDK.
Run: python custom_tools.py
"""

import asyncio
import json
import random
from datetime import datetime

from pydantic import BaseModel, Field

from copilot import CopilotClient
from copilot.tools import define_tool
from copilot.types import SessionEventType, ToolResult


# =============================================================================
# Simple Tools with @define_tool Decorator
# =============================================================================


class GetWeatherParams(BaseModel):
    """Parameters for the get_weather tool."""

    city: str = Field(description="The city name to get weather for")
    units: str = Field(
        default="celsius",
        description="Temperature units: 'celsius' or 'fahrenheit'",
    )


@define_tool(description="Get the current weather for a city")
def get_weather(params):
    """Simulated weather API. In production, call a real weather API."""
    weather_data = {
        "temperature": random.randint(15, 30),
        "condition": random.choice(["sunny", "cloudy", "rainy", "partly cloudy"]),
        "humidity": random.randint(40, 80),
        "wind_speed": random.randint(5, 25),
    }

    if params.units == "fahrenheit":
        weather_data["temperature"] = int(weather_data["temperature"] * 9 / 5 + 32)
        temp_unit = "°F"
    else:
        temp_unit = "°C"

    return (
        f"Weather in {params.city}:\n"
        f"  Temperature: {weather_data['temperature']}{temp_unit}\n"
        f"  Condition: {weather_data['condition']}\n"
        f"  Humidity: {weather_data['humidity']}%\n"
        f"  Wind: {weather_data['wind_speed']} km/h"
    )


class CalculatorParams(BaseModel):
    """Parameters for the calculator tool."""

    expression: str = Field(description="Mathematical expression to evaluate")


@define_tool(description="Evaluate a mathematical expression safely")
def calculator(params):
    """Safely evaluate mathematical expressions."""
    allowed = set("0123456789+-*/().% ")

    if not all(c in allowed for c in params.expression):
        return "Error: Expression contains invalid characters"

    try:
        # Use eval with restricted globals for safety
        result = eval(params.expression, {"__builtins__": {}}, {})
        return f"Result: {result}"
    except Exception as e:
        return f"Error: {e}"


class GetTimeParams(BaseModel):
    """Parameters for the get_time tool."""

    timezone: str = Field(default="UTC", description="Timezone name")


@define_tool(description="Get the current time in a specific timezone")
def get_current_time(params):
    """Get the current time."""
    now = datetime.now()
    return f"Current time ({params.timezone}): {now.strftime('%Y-%m-%d %H:%M:%S')}"


# =============================================================================
# Async Tool
# =============================================================================


class FetchURLParams(BaseModel):
    """Parameters for the fetch_url tool."""

    url: str = Field(description="The URL to fetch content from")
    max_length: int = Field(default=1000, description="Maximum characters to return")


@define_tool(description="Fetch content from a URL (simulated)")
async def fetch_url(params):
    """Async tool for fetching URL content."""
    await asyncio.sleep(0.5)  # Simulate network delay

    # Simulated response
    content = f"""
<!DOCTYPE html>
<html>
<head><title>Content from {params.url}</title></head>
<body>
<h1>Fetched Content</h1>
<p>This is simulated content from {params.url}</p>
<p>In a real implementation, this would be the actual page content.</p>
</body>
</html>
"""

    if len(content) > params.max_length:
        content = content[: params.max_length] + "... (truncated)"

    return content


# =============================================================================
# Tool with Invocation Context
# =============================================================================


class LogMessageParams(BaseModel):
    """Parameters for the log_message tool."""

    level: str = Field(description="Log level: 'info', 'warning', 'error'")
    message: str = Field(description="The message to log")


@define_tool(description="Log a message with context information")
def log_message(params, invocation):
    """Tool that accesses the invocation context."""
    log_entry = {
        "timestamp": datetime.now().isoformat(),
        "level": params.level.upper(),
        "message": params.message,
        "session_id": invocation["session_id"][:12] + "...",
        "tool_call_id": invocation["tool_call_id"][:8] + "...",
    }

    print(f"[LOG] {json.dumps(log_entry, indent=2)}")
    return f"Logged {params.level} message: {params.message}"


# =============================================================================
# Tool Returning Structured Data
# =============================================================================


class SearchParams(BaseModel):
    """Parameters for the search_database tool."""

    query: str = Field(description="Search query string")
    limit: int = Field(default=5, description="Maximum results")


class SearchResult(BaseModel):
    """A single search result."""

    id: str
    title: str
    score: float
    snippet: str


@define_tool(description="Search a database of documents (simulated)")
def search_database(params):
    """Tool that returns structured Pydantic models."""
    results = []
    for i in range(min(params.limit, 5)):
        results.append(
            SearchResult(
                id=f"doc-{random.randint(1000, 9999)}",
                title=f"Document about {params.query} ({i + 1})",
                score=random.uniform(0.7, 1.0),
                snippet=f"This document discusses {params.query} in detail...",
            )
        )
    return results


# =============================================================================
# Tool with Custom Result
# =============================================================================


class FormatDataParams(BaseModel):
    """Parameters for the format_data tool."""

    data: dict = Field(description="The data to format")
    format: str = Field(default="json", description="Output format: json, table, markdown")


@define_tool(description="Format data in various output formats")
def format_data(params):
    """Tool that returns a custom ToolResult."""
    data = params.data

    if params.format == "json":
        formatted = json.dumps(data, indent=2)
    elif params.format == "table":
        lines = []
        for key, value in data.items():
            lines.append(f"| {key:20} | {str(value):30} |")
        formatted = "\n".join(lines)
    elif params.format == "markdown":
        lines = [f"- **{key}**: {value}" for key, value in data.items()]
        formatted = "\n".join(lines)
    else:
        return ToolResult(
            textResultForLlm=f"Unknown format: {params.format}",
            resultType="failure",
            error=f"Unknown format: {params.format}",
        )

    return ToolResult(
        textResultForLlm=formatted,
        resultType="success",
    )


# =============================================================================
# Tool Without Decorator
# =============================================================================


def create_greeting_tool():
    """Create a tool using the functional API instead of decorators."""

    class GreetParams(BaseModel):
        name: str = Field(description="Name of the person to greet")
        language: str = Field(default="en", description="Language code: en, es, fr, de")

    def handler(params):
        greetings = {
            "en": f"Hello, {params.name}!",
            "es": f"¡Hola, {params.name}!",
            "fr": f"Bonjour, {params.name}!",
            "de": f"Hallo, {params.name}!",
        }
        return greetings.get(params.language, greetings["en"])

    return define_tool(
        "greet_user",
        description="Greet a user in their preferred language",
        handler=handler,
        params_type=GreetParams,
    )


# =============================================================================
# Demo: Basic Tools
# =============================================================================


async def demo_basic_tools():
    """Demonstrate basic custom tools."""
    print("\n=== Basic Custom Tools ===\n")

    client = CopilotClient()

    try:
        await client.start()

        # Create session with custom tools
        tools = [get_weather, calculator, get_current_time]
        session = await client.create_session({"tools": tools})

        def handler(event):
            if event.type == SessionEventType.ASSISTANT_MESSAGE:
                print(f"\n🤖 {event.data.content}")
            elif event.type == SessionEventType.TOOL_EXECUTION_START:
                print(f"  ⚙️  Executing: {event.data.tool_name}")
            elif event.type == SessionEventType.TOOL_EXECUTION_COMPLETE:
                print(f"  ✓ Completed")

        session.on(handler)

        # Ask questions that use the tools
        print("Asking about weather...")
        await session.send_and_wait(
            {"prompt": "What's the weather like in Tokyo and New York?"},
            timeout=60.0,
        )

        print("\n" + "-" * 40)
        print("Asking for calculations...")
        await session.send_and_wait(
            {"prompt": "Calculate: 15% of 250, and also (125 * 4) / 5"},
            timeout=60.0,
        )

        print("\n" + "-" * 40)
        print("Asking for time...")
        await session.send_and_wait(
            {"prompt": "What time is it right now?"},
            timeout=60.0,
        )

        await session.destroy()

    finally:
        await client.stop()


async def demo_advanced_tools():
    """Demonstrate advanced custom tools."""
    print("\n=== Advanced Custom Tools ===\n")

    client = CopilotClient()

    try:
        await client.start()

        tools = [fetch_url, search_database, format_data, log_message, create_greeting_tool()]
        session = await client.create_session({"tools": tools})

        def handler(event):
            if event.type == SessionEventType.ASSISTANT_MESSAGE:
                print(f"\n🤖 {event.data.content}")
            elif event.type == SessionEventType.TOOL_EXECUTION_START:
                print(f"  ⚙️  Executing: {event.data.tool_name}")

        session.on(handler)

        # Test structured data tool
        print("Testing search tool...")
        await session.send_and_wait(
            {"prompt": "Search for documents about 'machine learning'"},
            timeout=60.0,
        )

        print("\n" + "-" * 40)
        print("Testing greeting tool...")
        await session.send_and_wait(
            {"prompt": "Greet Alice in French and Bob in Spanish"},
            timeout=60.0,
        )

        print("\n" + "-" * 40)
        print("Testing format tool...")
        await session.send_and_wait(
            {
                "prompt": "Format this data as markdown: name=John, age=30, city=NYC"
            },
            timeout=60.0,
        )

        await session.destroy()

    finally:
        await client.stop()


async def demo_tool_orchestration():
    """Demonstrate multiple tools working together."""
    print("\n=== Tool Orchestration ===\n")

    client = CopilotClient()

    try:
        await client.start()

        tools = [get_weather, calculator, get_current_time, search_database, log_message]
        session = await client.create_session({"tools": tools})

        def handler(event):
            if event.type == SessionEventType.ASSISTANT_MESSAGE:
                print(f"\n🤖 {event.data.content}")
            elif event.type == SessionEventType.TOOL_EXECUTION_START:
                print(f"  ⚙️  {event.data.tool_name}")
            elif event.type == SessionEventType.TOOL_EXECUTION_COMPLETE:
                print(f"  ✓ Done")

        session.on(handler)

        # Complex request requiring multiple tools
        print("Complex request using multiple tools...")
        await session.send_and_wait(
            {
                "prompt": """
Please help me with a few things:
1. What's the current time?
2. What's the weather in London?
3. Search for documents about 'Python'
4. Log an info message saying 'User requested status check'
5. Calculate: what's 20% of the number 350?

Summarize all findings at the end.
"""
            },
            timeout=120.0,
        )

        await session.destroy()

    finally:
        await client.stop()


# =============================================================================
# Main
# =============================================================================


async def main():
    """Run all custom tools demonstrations."""
    print("=" * 60)
    print("Custom Tools Patterns")
    print("=" * 60)

    await demo_basic_tools()
    await demo_advanced_tools()
    await demo_tool_orchestration()

    print("\n" + "=" * 60)
    print("All demos completed!")
    print("=" * 60)


if __name__ == "__main__":
    asyncio.run(main())
