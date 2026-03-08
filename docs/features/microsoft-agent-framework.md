# Microsoft Agent Framework Integration

Use the Copilot SDK as an agent provider inside the [Microsoft Agent Framework](https://devblogs.microsoft.com/semantic-kernel/build-ai-agents-with-github-copilot-sdk-and-microsoft-agent-framework/) (MAF) to compose multi-agent workflows alongside Azure OpenAI, Anthropic, and other providers.

## Overview

The Microsoft Agent Framework is the unified successor to Semantic Kernel and AutoGen. It provides a standard interface for building, orchestrating, and deploying AI agents. Dedicated integration packages let you wrap a Copilot SDK client as a first-class MAF agent — interchangeable with any other agent provider in the framework.

| Concept | Description |
|---------|-------------|
| **Microsoft Agent Framework** | Open-source framework for single- and multi-agent orchestration in .NET and Python |
| **Agent provider** | A backend that powers an agent (Copilot, Azure OpenAI, Anthropic, etc.) |
| **Orchestrator** | A MAF component that coordinates agents in sequential, concurrent, or handoff workflows |
| **A2A protocol** | Agent-to-Agent communication standard supported by the framework |

> **Note:** MAF integration packages are available for **.NET** and **Python**. For TypeScript and Go, use the Copilot SDK directly — the standard SDK APIs already provide tool calling, streaming, and custom agents.

## Prerequisites

Before you begin, ensure you have:

- A working [Copilot SDK setup](../getting-started.md) in your language of choice
- A GitHub Copilot subscription (Individual, Business, or Enterprise)
- The Copilot CLI installed or available via the SDK's bundled CLI

## Installation

Install the Copilot SDK alongside the MAF integration package for your language:

<details open>
<summary><strong>.NET</strong></summary>

```shell
dotnet add package GitHub.Copilot.SDK
dotnet add package Microsoft.Agents.AI.GitHub.Copilot --prerelease
```

</details>

<details>
<summary><strong>Python</strong></summary>

```shell
pip install copilot-sdk agent-framework-github-copilot
```

</details>

## Basic Usage

Wrap the Copilot SDK client as a MAF agent with a single method call. The resulting agent conforms to the framework's standard interface and can be used anywhere a MAF agent is expected.

<details open>
<summary><strong>.NET</strong></summary>

<!-- docs-validate: skip -->
```csharp
using GitHub.Copilot.SDK;
using Microsoft.Agents.AI;

await using var copilotClient = new CopilotClient();
await copilotClient.StartAsync();

// Wrap as a MAF agent
AIAgent agent = copilotClient.AsAIAgent();

// Use the standard MAF interface
string response = await agent.RunAsync("Explain how dependency injection works in ASP.NET Core");
Console.WriteLine(response);
```

</details>

<details>
<summary><strong>Python</strong></summary>

<!-- docs-validate: skip -->
```python
from agent_framework.github import GitHubCopilotAgent

async def main():
    agent = GitHubCopilotAgent(
        default_options={
            "instructions": "You are a helpful coding assistant.",
        }
    )

    async with agent:
        result = await agent.run("Explain how dependency injection works in FastAPI")
        print(result)
```

</details>

## Adding Custom Tools

Extend your Copilot agent with custom function tools. Tools defined through the standard Copilot SDK are automatically available when the agent runs inside MAF.

<details open>
<summary><strong>.NET</strong></summary>

<!-- docs-validate: skip -->
```csharp
using GitHub.Copilot.SDK;
using Microsoft.Extensions.AI;
using Microsoft.Agents.AI;

// Define a custom tool
AIFunction weatherTool = AIFunctionFactory.Create(
    (string location) => $"The weather in {location} is sunny with a high of 25°C.",
    "GetWeather",
    "Get the current weather for a given location."
);

await using var copilotClient = new CopilotClient();
await copilotClient.StartAsync();

// Create agent with tools
AIAgent agent = copilotClient.AsAIAgent(new AIAgentOptions
{
    Tools = new[] { weatherTool },
});

string response = await agent.RunAsync("What's the weather like in Seattle?");
Console.WriteLine(response);
```

</details>

<details>
<summary><strong>Python</strong></summary>

<!-- docs-validate: skip -->
```python
from agent_framework.github import GitHubCopilotAgent

def get_weather(location: str) -> str:
    """Get the current weather for a given location."""
    return f"The weather in {location} is sunny with a high of 25°C."

async def main():
    agent = GitHubCopilotAgent(
        default_options={
            "instructions": "You are a helpful assistant with access to weather data.",
        },
        tools=[get_weather],
    )

    async with agent:
        result = await agent.run("What's the weather like in Seattle?")
        print(result)
```

</details>

You can also use Copilot SDK's native tool definition alongside MAF tools:

<details open>
<summary><strong>Node.js / TypeScript (standalone SDK)</strong></summary>

```typescript
import { CopilotClient, DefineTool } from "@github/copilot-sdk";

const getWeather = DefineTool({
    name: "GetWeather",
    description: "Get the current weather for a given location.",
    parameters: { location: { type: "string", description: "City name" } },
    execute: async ({ location }) => `The weather in ${location} is sunny, 25°C.`,
});

const client = new CopilotClient();
const session = await client.createSession({
    model: "gpt-4.1",
    tools: [getWeather],
    onPermissionRequest: async () => ({ kind: "approved" }),
});

await session.sendAndWait({ prompt: "What's the weather like in Seattle?" });
```

</details>

## Multi-Agent Workflows

The primary benefit of MAF integration is composing Copilot alongside other agent providers in orchestrated workflows. Use the framework's built-in orchestrators to create pipelines where different agents handle different steps.

### Sequential Workflow

Run agents one after another, passing output from one to the next:

<details open>
<summary><strong>.NET</strong></summary>

<!-- docs-validate: skip -->
```csharp
using GitHub.Copilot.SDK;
using Microsoft.Agents.AI;
using Microsoft.Agents.AI.Orchestration;

await using var copilotClient = new CopilotClient();
await copilotClient.StartAsync();

// Copilot agent for code review
AIAgent reviewer = copilotClient.AsAIAgent(new AIAgentOptions
{
    Instructions = "You review code for bugs, security issues, and best practices. Be thorough.",
});

// Azure OpenAI agent for generating documentation
AIAgent documentor = AIAgent.FromOpenAI(new OpenAIAgentOptions
{
    Model = "gpt-4.1",
    Instructions = "You write clear, concise documentation for code changes.",
});

// Compose in a sequential pipeline
var pipeline = new SequentialOrchestrator(new[] { reviewer, documentor });

string result = await pipeline.RunAsync(
    "Review and document this pull request: added retry logic to the HTTP client"
);
Console.WriteLine(result);
```

</details>

<details>
<summary><strong>Python</strong></summary>

<!-- docs-validate: skip -->
```python
from agent_framework.github import GitHubCopilotAgent
from agent_framework.openai import OpenAIAgent
from agent_framework.orchestration import SequentialOrchestrator

async def main():
    # Copilot agent for code review
    reviewer = GitHubCopilotAgent(
        default_options={
            "instructions": "You review code for bugs, security issues, and best practices.",
        }
    )

    # OpenAI agent for documentation
    documentor = OpenAIAgent(
        model="gpt-4.1",
        instructions="You write clear, concise documentation for code changes.",
    )

    # Compose in a sequential pipeline
    pipeline = SequentialOrchestrator(agents=[reviewer, documentor])

    async with pipeline:
        result = await pipeline.run(
            "Review and document this PR: added retry logic to the HTTP client"
        )
        print(result)
```

</details>

### Concurrent Workflow

Run multiple agents in parallel and aggregate their results:

<details open>
<summary><strong>.NET</strong></summary>

<!-- docs-validate: skip -->
```csharp
using GitHub.Copilot.SDK;
using Microsoft.Agents.AI;
using Microsoft.Agents.AI.Orchestration;

await using var copilotClient = new CopilotClient();
await copilotClient.StartAsync();

AIAgent securityReviewer = copilotClient.AsAIAgent(new AIAgentOptions
{
    Instructions = "Focus exclusively on security vulnerabilities and risks.",
});

AIAgent performanceReviewer = copilotClient.AsAIAgent(new AIAgentOptions
{
    Instructions = "Focus exclusively on performance bottlenecks and optimization opportunities.",
});

// Run both reviews concurrently
var concurrent = new ConcurrentOrchestrator(new[] { securityReviewer, performanceReviewer });

string combinedResult = await concurrent.RunAsync(
    "Analyze this database query module for issues"
);
Console.WriteLine(combinedResult);
```

</details>

## Streaming Responses

When building interactive applications, stream agent responses to show real-time output. The MAF integration preserves the Copilot SDK's streaming capabilities.

<details open>
<summary><strong>.NET</strong></summary>

<!-- docs-validate: skip -->
```csharp
using GitHub.Copilot.SDK;
using Microsoft.Agents.AI;

await using var copilotClient = new CopilotClient();
await copilotClient.StartAsync();

AIAgent agent = copilotClient.AsAIAgent(new AIAgentOptions
{
    Streaming = true,
});

await foreach (var chunk in agent.RunStreamingAsync("Write a quicksort implementation in C#"))
{
    Console.Write(chunk);
}
Console.WriteLine();
```

</details>

<details>
<summary><strong>Python</strong></summary>

<!-- docs-validate: skip -->
```python
from agent_framework.github import GitHubCopilotAgent

async def main():
    agent = GitHubCopilotAgent(
        default_options={"streaming": True}
    )

    async with agent:
        async for chunk in agent.run_streaming("Write a quicksort in Python"):
            print(chunk, end="", flush=True)
        print()
```

</details>

You can also stream directly through the Copilot SDK without MAF:

<details open>
<summary><strong>Node.js / TypeScript (standalone SDK)</strong></summary>

```typescript
import { CopilotClient } from "@github/copilot-sdk";

const client = new CopilotClient();
const session = await client.createSession({
    model: "gpt-4.1",
    streaming: true,
    onPermissionRequest: async () => ({ kind: "approved" }),
});

session.on("assistant.message_delta", (event) => {
    process.stdout.write(event.data.delta ?? "");
});

await session.sendAndWait({ prompt: "Write a quicksort implementation in TypeScript" });
```

</details>

## Configuration Reference

### MAF Agent Options

| Property | Type | Description |
|----------|------|-------------|
| `Instructions` / `instructions` | `string` | System prompt for the agent |
| `Tools` / `tools` | `AIFunction[]` / `list` | Custom function tools available to the agent |
| `Streaming` / `streaming` | `bool` | Enable streaming responses |
| `Model` / `model` | `string` | Override the default model |

### Copilot SDK Options (Passed Through)

All standard [SessionConfig](../getting-started.md) options are still available when creating the underlying Copilot client. The MAF wrapper delegates to the SDK under the hood:

| SDK Feature | MAF Support |
|-------------|-------------|
| Custom tools (`DefineTool` / `AIFunctionFactory`) | ✅ Merged with MAF tools |
| MCP servers | ✅ Configured on the SDK client |
| Custom agents / sub-agents | ✅ Available within the Copilot agent |
| Infinite sessions | ✅ Configured on the SDK client |
| Model selection | ✅ Overridable per agent or per call |
| Streaming | ✅ Full delta event support |

## Best Practices

### Choose the right level of integration

Use the MAF wrapper when you need to compose Copilot with other providers in orchestrated workflows. If your application only uses Copilot, the standalone SDK is simpler and gives you full control:

```typescript
// Standalone SDK — full control, simpler setup
import { CopilotClient } from "@github/copilot-sdk";

const client = new CopilotClient();
const session = await client.createSession({
    model: "gpt-4.1",
    onPermissionRequest: async () => ({ kind: "approved" }),
});
const response = await session.sendAndWait({ prompt: "Explain this code" });
```

### Keep agents focused

When building multi-agent workflows, give each agent a specific role with clear instructions. Avoid overlapping responsibilities:

```typescript
// ❌ Too vague — overlapping roles
const agents = [
    { instructions: "Help with code" },
    { instructions: "Assist with programming" },
];

// ✅ Focused — clear separation of concerns
const agents = [
    { instructions: "Review code for security vulnerabilities. Flag SQL injection, XSS, and auth issues." },
    { instructions: "Optimize code performance. Focus on algorithmic complexity and memory usage." },
];
```

### Handle errors at the orchestration level

Wrap agent calls in error handling, especially in multi-agent workflows where one agent's failure shouldn't block the entire pipeline:

<!-- docs-validate: skip -->
```csharp
try
{
    string result = await pipeline.RunAsync("Analyze this module");
    Console.WriteLine(result);
}
catch (AgentException ex)
{
    Console.Error.WriteLine($"Agent {ex.AgentName} failed: {ex.Message}");
    // Fall back to single-agent mode or retry
}
```

## See Also

- [Getting Started](../getting-started.md) — initial Copilot SDK setup
- [Custom Agents](./custom-agents.md) — define specialized sub-agents within the SDK
- [Custom Skills](./skills.md) — reusable prompt modules
- [Microsoft Agent Framework documentation](https://learn.microsoft.com/en-us/agent-framework/agents/providers/github-copilot) — official MAF docs for the Copilot provider
- [Blog: Build AI Agents with GitHub Copilot SDK and Microsoft Agent Framework](https://devblogs.microsoft.com/semantic-kernel/build-ai-agents-with-github-copilot-sdk-and-microsoft-agent-framework/)
