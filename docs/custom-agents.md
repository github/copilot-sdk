# Custom Agents

Custom agents are specialized AI assistants with custom prompts, tools, and behaviors. They allow you to create domain-specific assistants that excel at particular tasks like code review, SQL optimization, or documentation writing.

## What is an Agent?

An agent is a session configuration that defines:

- **Custom system prompt**: Specialized behavior and expertise
- **Specific tools**: Limited or extended capabilities
- **Focused context**: Domain-specific knowledge

```
┌─────────────────────────────────────────────┐
│                Custom Agent                 │
├─────────────────────────────────────────────┤
│  Name: "code-reviewer"                      │
│  Prompt: "You are an expert code reviewer"  │
│  Tools: [analyze_code, check_style]         │
│  Infer: true (auto-selection enabled)       │
└─────────────────────────────────────────────┘
```

## Quick Start

<details open>
<summary><strong>Node.js / TypeScript</strong></summary>

```typescript
import { CopilotClient, SessionEventType } from "@github/copilot-sdk";

const client = new CopilotClient();
const session = await client.createSession({
    model: "gpt-4.1",
    customAgents: [
        {
            name: "code-reviewer",
            displayName: "Code Review Expert",
            description: "Reviews code for bugs, security, and best practices",
            prompt: `You are an expert code reviewer specializing in:
- Code quality and readability
- Security vulnerabilities (OWASP Top 10)
- Performance optimization
- Best practices and design patterns

When reviewing, start with a summary, then list issues by severity.`,
            tools: null,  // Use all available tools
            infer: true,  // Enable automatic selection
        },
    ],
});

session.on((event) => {
    if (event.type === SessionEventType.AssistantMessage) {
        console.log(event.data.content);
    }
});

await session.sendAndWait({
    prompt: "@code-reviewer Review this function:\n\ndef get_user(id): return db.query(f'SELECT * FROM users WHERE id={id}')"
});

await client.stop();
```

</details>

<details>
<summary><strong>Python</strong></summary>

```python
import asyncio
from copilot import CopilotClient
from copilot.types import CustomAgentConfig, SessionEventType

async def main():
    client = CopilotClient()
    await client.start()

    code_reviewer = CustomAgentConfig(
        name="code-reviewer",
        display_name="Code Review Expert",
        description="Reviews code for bugs, security, and best practices",
        prompt="""You are an expert code reviewer specializing in:
- Code quality and readability
- Security vulnerabilities (OWASP Top 10)
- Performance optimization
- Best practices and design patterns

When reviewing, start with a summary, then list issues by severity.""",
        tools=None,  # Use all available tools
        infer=True,  # Enable automatic selection
    )

    session = await client.create_session({
        "model": "gpt-4.1",
        "custom_agents": [code_reviewer],
    })

    def handle_event(event):
        if event.type == SessionEventType.ASSISTANT_MESSAGE:
            print(event.data.content)

    session.on(handle_event)

    await session.send_and_wait({
        "prompt": "@code-reviewer Review this function:\n\ndef get_user(id): return db.query(f'SELECT * FROM users WHERE id={id}')"
    })

    await client.stop()

asyncio.run(main())
```

</details>

<details>
<summary><strong>Go</strong></summary>

```go
package main

import (
    "fmt"
    "log"

    copilot "github.com/github/copilot-sdk/go"
)

func main() {
    client := copilot.NewClient(nil)
    if err := client.Start(); err != nil {
        log.Fatal(err)
    }
    defer client.Stop()

    session, err := client.CreateSession(&copilot.SessionConfig{
        Model: "gpt-4.1",
        CustomAgents: []copilot.CustomAgentConfig{
            {
                Name:        "code-reviewer",
                DisplayName: "Code Review Expert",
                Description: "Reviews code for bugs, security, and best practices",
                Prompt: `You are an expert code reviewer specializing in:
- Code quality and readability
- Security vulnerabilities (OWASP Top 10)
- Performance optimization
- Best practices and design patterns

When reviewing, start with a summary, then list issues by severity.`,
                Tools: nil,   // Use all available tools
                Infer: true,  // Enable automatic selection
            },
        },
    })
    if err != nil {
        log.Fatal(err)
    }

    session.On(func(event copilot.SessionEvent) {
        if event.Type == copilot.SessionEventTypeAssistantMessage {
            fmt.Println(*event.Data.Content)
        }
    })

    _, err = session.SendAndWait(copilot.MessageOptions{
        Prompt: "@code-reviewer Review this function:\n\ndef get_user(id): return db.query(f'SELECT * FROM users WHERE id={id}')",
    }, 0)
    if err != nil {
        log.Fatal(err)
    }
}
```

</details>

<details>
<summary><strong>.NET</strong></summary>

```csharp
using GitHub.Copilot.SDK;

await using var client = new CopilotClient();
await using var session = await client.CreateSessionAsync(new SessionConfig
{
    Model = "gpt-4.1",
    CustomAgents = new[]
    {
        new CustomAgentConfig
        {
            Name = "code-reviewer",
            DisplayName = "Code Review Expert",
            Description = "Reviews code for bugs, security, and best practices",
            Prompt = @"You are an expert code reviewer specializing in:
- Code quality and readability
- Security vulnerabilities (OWASP Top 10)
- Performance optimization
- Best practices and design patterns

When reviewing, start with a summary, then list issues by severity.",
            Tools = null,  // Use all available tools
            Infer = true,  // Enable automatic selection
        },
    },
});

session.On(e =>
{
    if (e.Type == SessionEventType.AssistantMessage)
    {
        Console.WriteLine(e.Data.Content);
    }
});

await session.SendAndWaitAsync(new MessageOptions
{
    Prompt = "@code-reviewer Review this function:\n\ndef get_user(id): return db.query(f'SELECT * FROM users WHERE id={id}')"
});
```

</details>

## Agent Configuration Options

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `name` | `string` | Yes | Unique identifier for the agent (used with `@` mentions) |
| `display_name` | `string` | No | Human-readable name for UI display |
| `description` | `string` | No | Description of what the agent does (helps with auto-selection) |
| `prompt` | `string` | Yes | System prompt defining the agent's behavior and expertise |
| `tools` | `string[]` or `null` | No | List of allowed tools (`null` = all, `[]` = none) |
| `mcp_servers` | `object` | No | MCP servers specific to this agent |
| `infer` | `boolean` | No | Whether the agent can be auto-selected based on context |

## Using Agents

### Explicit Selection with @mention

Call a specific agent by name:

```
@code-reviewer Check this code for security issues
@sql-expert Optimize this query for performance
@doc-writer Write API documentation for this function
```

### Automatic Selection (Infer)

When `infer: true`, Copilot automatically selects the best agent based on the prompt context:

```
"Review this Python function for bugs"     → Selects code-reviewer
"How do I write a JOIN query?"             → Selects sql-expert
"Write a README for this project"          → Selects doc-writer
```

## Multiple Agents

Register multiple agents in a single session:

<details open>
<summary><strong>Node.js / TypeScript</strong></summary>

```typescript
const session = await client.createSession({
    customAgents: [
        {
            name: "reviewer",
            description: "Code review expert",
            prompt: "You are an expert code reviewer...",
            infer: true,
        },
        {
            name: "sql-expert",
            description: "Database and SQL specialist",
            prompt: "You are a database expert...",
            infer: true,
        },
        {
            name: "doc-writer",
            description: "Technical documentation writer",
            prompt: "You are a technical writer...",
            infer: true,
        },
    ],
});

// Use specific agents
await session.sendAndWait({ prompt: "@reviewer Check this for bugs" });
await session.sendAndWait({ prompt: "@sql-expert Optimize this query" });
await session.sendAndWait({ prompt: "@doc-writer Document this API" });
```

</details>

<details>
<summary><strong>Python</strong></summary>

```python
session = await client.create_session({
    "custom_agents": [
        CustomAgentConfig(
            name="reviewer",
            description="Code review expert",
            prompt="You are an expert code reviewer...",
            infer=True,
        ),
        CustomAgentConfig(
            name="sql-expert",
            description="Database and SQL specialist",
            prompt="You are a database expert...",
            infer=True,
        ),
        CustomAgentConfig(
            name="doc-writer",
            description="Technical documentation writer",
            prompt="You are a technical writer...",
            infer=True,
        ),
    ],
})

# Use specific agents
await session.send_and_wait({"prompt": "@reviewer Check this for bugs"})
await session.send_and_wait({"prompt": "@sql-expert Optimize this query"})
await session.send_and_wait({"prompt": "@doc-writer Document this API"})
```

</details>

<details>
<summary><strong>Go</strong></summary>

```go
session, err := client.CreateSession(&copilot.SessionConfig{
    CustomAgents: []copilot.CustomAgentConfig{
        {
            Name:        "reviewer",
            Description: "Code review expert",
            Prompt:      "You are an expert code reviewer...",
            Infer:       true,
        },
        {
            Name:        "sql-expert",
            Description: "Database and SQL specialist",
            Prompt:      "You are a database expert...",
            Infer:       true,
        },
        {
            Name:        "doc-writer",
            Description: "Technical documentation writer",
            Prompt:      "You are a technical writer...",
            Infer:       true,
        },
    },
})
if err != nil {
    log.Fatal(err)
}

// Use specific agents
session.SendAndWait(copilot.MessageOptions{Prompt: "@reviewer Check this for bugs"}, 0)
session.SendAndWait(copilot.MessageOptions{Prompt: "@sql-expert Optimize this query"}, 0)
session.SendAndWait(copilot.MessageOptions{Prompt: "@doc-writer Document this API"}, 0)
```

</details>

<details>
<summary><strong>.NET</strong></summary>

```csharp
await using var session = await client.CreateSessionAsync(new SessionConfig
{
    CustomAgents = new[]
    {
        new CustomAgentConfig
        {
            Name = "reviewer",
            Description = "Code review expert",
            Prompt = "You are an expert code reviewer...",
            Infer = true,
        },
        new CustomAgentConfig
        {
            Name = "sql-expert",
            Description = "Database and SQL specialist",
            Prompt = "You are a database expert...",
            Infer = true,
        },
        new CustomAgentConfig
        {
            Name = "doc-writer",
            Description = "Technical documentation writer",
            Prompt = "You are a technical writer...",
            Infer = true,
        },
    },
});

// Use specific agents
await session.SendAndWaitAsync(new MessageOptions { Prompt = "@reviewer Check this for bugs" });
await session.SendAndWaitAsync(new MessageOptions { Prompt = "@sql-expert Optimize this query" });
await session.SendAndWaitAsync(new MessageOptions { Prompt = "@doc-writer Document this API" });
```

</details>

## Agents with Custom Tools

Combine agents with custom tools for powerful workflows:

<details open>
<summary><strong>Node.js / TypeScript</strong></summary>

```typescript
import { CopilotClient, defineTool } from "@github/copilot-sdk";
import { z } from "zod";

// Define a custom tool
const runLinter = defineTool({
    name: "run_linter",
    description: "Run a linter on Python code",
    parameters: z.object({
        code: z.string().describe("The code to lint"),
    }),
    handler: async ({ code }) => {
        // In production, actually run pylint/flake8
        return { issues: [], score: 10.0 };
    },
});

const session = await client.createSession({
    tools: [runLinter],
    customAgents: [
        {
            name: "linter",
            description: "Code quality checker with linting",
            prompt: "You check code quality. Use the run_linter tool to analyze code.",
            tools: ["run_linter"],  // Only this tool available
            infer: true,
        },
    ],
});
```

</details>

<details>
<summary><strong>Python</strong></summary>

```python
from copilot import CopilotClient
from copilot.tools import define_tool
from copilot.types import CustomAgentConfig
from pydantic import BaseModel, Field

class LintParams(BaseModel):
    code: str = Field(description="The code to lint")

@define_tool(description="Run a linter on Python code")
def run_linter(params):
    # In production, actually run pylint/flake8
    return {"issues": [], "score": 10.0}

session = await client.create_session({
    "tools": [run_linter],
    "custom_agents": [
        CustomAgentConfig(
            name="linter",
            description="Code quality checker with linting",
            prompt="You check code quality. Use the run_linter tool to analyze code.",
            tools=["run_linter"],  # Only this tool available
            infer=True,
        ),
    ],
})
```

</details>

<details>
<summary><strong>Go</strong></summary>

```go
// Define parameter type
type LintParams struct {
    Code string `json:"code" jsonschema:"The code to lint"`
}

// Define the tool
runLinter := copilot.DefineTool(
    "run_linter",
    "Run a linter on Python code",
    func(params LintParams, inv copilot.ToolInvocation) (map[string]interface{}, error) {
        // In production, actually run pylint/flake8
        return map[string]interface{}{"issues": []string{}, "score": 10.0}, nil
    },
)

session, err := client.CreateSession(&copilot.SessionConfig{
    Tools: []copilot.Tool{runLinter},
    CustomAgents: []copilot.CustomAgentConfig{
        {
            Name:        "linter",
            Description: "Code quality checker with linting",
            Prompt:      "You check code quality. Use the run_linter tool to analyze code.",
            Tools:       []string{"run_linter"}, // Only this tool available
            Infer:       true,
        },
    },
})
```

</details>

<details>
<summary><strong>.NET</strong></summary>

```csharp
using Microsoft.Extensions.AI;
using System.ComponentModel;

// Define a custom tool
var runLinter = AIFunctionFactory.Create(
    ([Description("The code to lint")] string code) =>
    {
        // In production, actually run pylint/flake8
        return new { issues = Array.Empty<string>(), score = 10.0 };
    },
    "run_linter",
    "Run a linter on Python code"
);

await using var session = await client.CreateSessionAsync(new SessionConfig
{
    Tools = [runLinter],
    CustomAgents = new[]
    {
        new CustomAgentConfig
        {
            Name = "linter",
            Description = "Code quality checker with linting",
            Prompt = "You check code quality. Use the run_linter tool to analyze code.",
            Tools = new[] { "run_linter" }, // Only this tool available
            Infer = true,
        },
    },
});
```

</details>

## Agents with MCP Servers

Give agents access to specific MCP servers:

<details open>
<summary><strong>Node.js / TypeScript</strong></summary>

```typescript
import { CopilotClient } from "@github/copilot-sdk";

const session = await client.createSession({
    customAgents: [
        {
            name: "github-helper",
            description: "GitHub operations assistant",
            prompt: "You help with GitHub tasks using the GitHub API.",
            mcpServers: {
                github: {
                    type: "stdio",
                    command: "npx",
                    args: ["-y", "@modelcontextprotocol/server-github"],
                    env: { GITHUB_TOKEN: process.env.GITHUB_TOKEN },
                    tools: ["*"],
                },
            },
            infer: true,
        },
    ],
});
```

</details>

<details>
<summary><strong>Python</strong></summary>

```python
import os
from copilot import CopilotClient
from copilot.types import CustomAgentConfig

session = await client.create_session({
    "custom_agents": [
        CustomAgentConfig(
            name="github-helper",
            description="GitHub operations assistant",
            prompt="You help with GitHub tasks using the GitHub API.",
            mcp_servers={
                "github": {
                    "type": "stdio",
                    "command": "npx",
                    "args": ["-y", "@modelcontextprotocol/server-github"],
                    "env": {"GITHUB_TOKEN": os.environ["GITHUB_TOKEN"]},
                    "tools": ["*"],
                },
            },
            infer=True,
        ),
    ],
})
```

</details>

<details>
<summary><strong>Go</strong></summary>

```go
import "os"

session, err := client.CreateSession(&copilot.SessionConfig{
    CustomAgents: []copilot.CustomAgentConfig{
        {
            Name:        "github-helper",
            Description: "GitHub operations assistant",
            Prompt:      "You help with GitHub tasks using the GitHub API.",
            MCPServers: map[string]copilot.MCPServerConfig{
                "github": {
                    Type:    "stdio",
                    Command: "npx",
                    Args:    []string{"-y", "@modelcontextprotocol/server-github"},
                    Env:     map[string]string{"GITHUB_TOKEN": os.Getenv("GITHUB_TOKEN")},
                    Tools:   []string{"*"},
                },
            },
            Infer: true,
        },
    },
})
```

</details>

<details>
<summary><strong>.NET</strong></summary>

```csharp
using GitHub.Copilot.SDK;

await using var session = await client.CreateSessionAsync(new SessionConfig
{
    CustomAgents = new[]
    {
        new CustomAgentConfig
        {
            Name = "github-helper",
            Description = "GitHub operations assistant",
            Prompt = "You help with GitHub tasks using the GitHub API.",
            McpServers = new Dictionary<string, object>
            {
                ["github"] = new McpLocalServerConfig
                {
                    Type = "stdio",
                    Command = "npx",
                    Args = new[] { "-y", "@modelcontextprotocol/server-github" },
                    Env = new Dictionary<string, string>
                    {
                        ["GITHUB_TOKEN"] = Environment.GetEnvironmentVariable("GITHUB_TOKEN")!
                    },
                    Tools = new[] { "*" },
                },
            },
            Infer = true,
        },
    },
});
```

</details>

See [MCP Servers](mcp.md) for detailed MCP configuration.

## Event Handling

Track agent interactions with events:

<details open>
<summary><strong>Node.js / TypeScript</strong></summary>

```typescript
import { SessionEventType } from "@github/copilot-sdk";

session.on((event) => {
    switch (event.type) {
        case SessionEventType.SubagentSelected:
            console.log(`🤖 Agent selected: ${event.data.agentName}`);
            break;
        case SessionEventType.AssistantMessage:
            console.log(`📝 Response: ${event.data.content}`);
            break;
        case SessionEventType.ToolExecutionStart:
            console.log(`🔧 Tool: ${event.data.toolName}`);
            break;
    }
});
```

</details>

<details>
<summary><strong>Python</strong></summary>

```python
from copilot.types import SessionEventType

def handle_event(event):
    if event.type == SessionEventType.SUBAGENT_SELECTED:
        print(f"🤖 Agent selected: {event.data.agent_name}")
    elif event.type == SessionEventType.ASSISTANT_MESSAGE:
        print(f"📝 Response: {event.data.content}")
    elif event.type == SessionEventType.TOOL_EXECUTION_START:
        print(f"🔧 Tool: {event.data.tool_name}")

session.on(handle_event)
```

</details>

<details>
<summary><strong>Go</strong></summary>

```go
session.On(func(event copilot.SessionEvent) {
    switch event.Type {
    case copilot.SessionEventTypeSubagentSelected:
        fmt.Printf("🤖 Agent selected: %s\n", *event.Data.AgentName)
    case copilot.SessionEventTypeAssistantMessage:
        fmt.Printf("📝 Response: %s\n", *event.Data.Content)
    case copilot.SessionEventTypeToolExecutionStart:
        fmt.Printf("🔧 Tool: %s\n", *event.Data.ToolName)
    }
})
```

</details>

<details>
<summary><strong>.NET</strong></summary>

```csharp
using GitHub.Copilot.SDK;

session.On(e =>
{
    switch (e.Type)
    {
        case SessionEventType.SubagentSelected:
            Console.WriteLine($"🤖 Agent selected: {e.Data.AgentName}");
            break;
        case SessionEventType.AssistantMessage:
            Console.WriteLine($"📝 Response: {e.Data.Content}");
            break;
        case SessionEventType.ToolExecutionStart:
            Console.WriteLine($"🔧 Tool: {e.Data.ToolName}");
            break;
    }
});
```

</details>

## Best Practices

1. **Clear prompts**: Be specific about the agent's expertise and behavior
2. **Focused scope**: Each agent should have a clear, single purpose
3. **Descriptive names**: Use memorable, action-oriented names like `code-reviewer`
4. **Appropriate tools**: Restrict tools to only what the agent needs
5. **Enable infer**: Set `infer: true` for seamless automatic selection
6. **Test prompts**: Iterate on system prompts for best results

## Common Agent Patterns

| Agent Type | Use Case | Key Prompt Elements |
|------------|----------|---------------------|
| Code Reviewer | Review code for bugs/style | Focus areas, severity levels, output format |
| SQL Expert | Database queries and optimization | Supported DBs, explain approach, warn about issues |
| Doc Writer | Technical documentation | Formats (Markdown, JSDoc), audience, structure |
| Security Auditor | Find vulnerabilities | OWASP Top 10, CVE references, remediation steps |
| API Designer | REST/GraphQL design | Best practices, versioning, error handling |

## Related Resources

- [Getting Started Guide](./getting-started.md) - SDK basics and custom tools
- [MCP Servers](./mcp.md) - Extend agents with MCP tools

## See Also

- Language-specific cookbooks for practical examples:
  - [Python Cookbook](../cookbook/python/custom-agents.md)
  - [Node.js Cookbook](../cookbook/nodejs/README.md)
  - [Go Cookbook](../cookbook/go/README.md)
  - [.NET Cookbook](../cookbook/dotnet/README.md)
