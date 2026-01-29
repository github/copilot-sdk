# Custom Agents

Create specialized AI agents with custom prompts and capabilities.

> **Skill Level:** Advanced
>
> **Runnable Example:** [recipe/custom_agents.py](recipe/custom_agents.py)
>
> ```bash
> cd recipe && pip install -r requirements.txt
> python custom_agents.py
> ```

## Overview

> **📖 What are Custom Agents?** For an introduction to agent concepts, configuration options, and multi-language examples, see [Custom Agents Documentation](../../docs/custom-agents.md).

This recipe covers Python-specific agent patterns:

- Creating agents with specialized prompts
- Agent-specific tools and capabilities
- Multiple agents for different tasks
- Dynamic agent creation

## Quick Start

```python
import asyncio
from copilot import CopilotClient, CustomAgentConfig

async def main():
    client = CopilotClient()
    await client.start()

    # Define a code review agent
    code_reviewer = CustomAgentConfig(
        name="code-reviewer",
        description="Expert code reviewer",
        system_prompt="""
You are an expert code reviewer specializing in Python.
Focus on:
- Code quality and readability
- Security vulnerabilities
- Performance issues
- Best practices
Always provide constructive feedback with examples.
"""
    )

    session = await client.create_session({
        "custom_agents": [code_reviewer]
    })

    await session.send_and_wait({
        "prompt": "@code-reviewer Review this function:\n\ndef add(a, b): return a+b"
    })

    await session.destroy()
    await client.stop()

asyncio.run(main())
```

## Agent Patterns

### Code Reviewer Agent

```python
from copilot import CustomAgentConfig

def create_code_reviewer_agent():
    """Create a specialized code review agent."""
    return CustomAgentConfig(
        name="reviewer",
        description="Expert code reviewer for Python projects",
        system_prompt="""
You are an expert code reviewer with deep knowledge of:
- Python best practices (PEP 8, PEP 257)
- Security vulnerabilities (OWASP Top 10)
- Performance optimization
- Design patterns

When reviewing code:
1. Start with a brief summary
2. List issues by severity (critical, warning, suggestion)
3. Provide fixed code examples
4. End with overall assessment

Be constructive and educational in your feedback.
"""
    )
```

### SQL Expert Agent

```python
def create_sql_expert_agent():
    """Create a SQL database expert agent."""
    return CustomAgentConfig(
        name="sql-expert",
        description="Database and SQL expert",
        system_prompt="""
You are a database expert specializing in:
- SQL query optimization
- Schema design
- PostgreSQL, MySQL, SQLite
- Performance tuning

When helping with queries:
1. Explain the approach
2. Provide optimized SQL
3. Note any indexes needed
4. Warn about potential issues (N+1, full table scans)

Always consider data integrity and security.
"""
    )
```

### Documentation Agent

```python
def create_docs_agent():
    """Create a documentation writer agent."""
    return CustomAgentConfig(
        name="docs-writer",
        description="Technical documentation expert",
        system_prompt="""
You are a technical writer specializing in:
- API documentation
- User guides
- README files
- Code comments

When writing documentation:
1. Use clear, concise language
2. Include code examples
3. Structure with headings and lists
4. Consider the audience (beginner vs advanced)

Follow the Diátaxis documentation framework.
"""
    )
```

## Multiple Agents

Use multiple specialized agents:

```python
async def multi_agent_demo():
    """Demonstrate multiple agents in one session."""
    client = CopilotClient()
    await client.start()

    # Create multiple agents
    agents = [
        create_code_reviewer_agent(),
        create_sql_expert_agent(),
        create_docs_agent()
    ]

    session = await client.create_session({
        "custom_agents": agents
    })

    # Use specific agents by name
    await session.send_and_wait({
        "prompt": "@reviewer Check this Python code for issues"
    })

    await session.send_and_wait({
        "prompt": "@sql-expert Optimize this SELECT query"
    })

    await session.send_and_wait({
        "prompt": "@docs-writer Write a README for this project"
    })

    await session.destroy()
    await client.stop()
```

## Agents with Tools

Add specific tools to agents:

```python
from copilot import define_tool, CustomAgentConfig

# Define tools for the agent
@define_tool(
    name="run_linter",
    description="Run a linter on Python code"
)
def run_linter(code: str) -> dict:
    # In production, actually run pylint/flake8
    return {"issues": [], "score": 10.0}

@define_tool(
    name="check_security",
    description="Check code for security issues"
)
def check_security(code: str) -> dict:
    return {"vulnerabilities": [], "risk_level": "low"}

# Create agent with tools
security_agent = CustomAgentConfig(
    name="security-reviewer",
    description="Security-focused code reviewer",
    system_prompt="You are a security expert. Use the security tools."
)

session = await client.create_session({
    "custom_agents": [security_agent],
    "tools": [run_linter, check_security]
})
```

## Dynamic Agent Creation

Create agents based on context:

```python
def create_project_agent(project_type, languages):
    """Create an agent specialized for a project type."""

    language_list = ", ".join(languages)

    prompts = {
        "web": f"""
You are a web development expert specializing in {language_list}.
Focus on frontend best practices, accessibility, and performance.
""",
        "api": f"""
You are a backend API expert specializing in {language_list}.
Focus on REST/GraphQL design, security, and scalability.
""",
        "data": f"""
You are a data engineering expert specializing in {language_list}.
Focus on data pipelines, ETL, and analytics.
""",
        "ml": f"""
You are a machine learning expert specializing in {language_list}.
Focus on model development, training, and deployment.
"""
    }

    return CustomAgentConfig(
        name=f"{project_type}-expert",
        description=f"Expert in {project_type} development",
        system_prompt=prompts.get(project_type, prompts["web"])
    )


# Usage
agent = create_project_agent("api", ["Python", "FastAPI"])
```

## Agent Collaboration

Chain agents for complex tasks:

```python
async def agent_collaboration_demo(session):
    """Demonstrate agents working together."""

    # First, get code reviewed
    await session.send_and_wait({
        "prompt": "@reviewer Review this authentication code: [code]"
    })

    # Then, check security
    await session.send_and_wait({
        "prompt": "@security-reviewer Analyze the security of the above code"
    })

    # Finally, document
    await session.send_and_wait({
        "prompt": "@docs-writer Write API documentation for the auth endpoint"
    })
```

## Agent Event Handling

Track agent interactions using `SessionEventType`:

```python
from copilot.types import SessionEventType

def create_agent_handler():
    """Track which agents are responding."""
    def handler(event):
        if event.type == SessionEventType.SUBAGENT_SELECTED:
            print(f"🤖 Agent: {event.data.agent_name}")

        elif event.type == SessionEventType.ASSISTANT_MESSAGE:
            print(f"📝 Response: {event.data.content[:100]}...")

        elif event.type == SessionEventType.TOOL_EXECUTION_START:
            print(f"🔧 Tool: {event.data.tool_name}")

    return handler

session.on(create_agent_handler())
```

## Agent Configuration Options

| Option | Description | Example |
|--------|-------------|---------|
| `name` | Agent identifier | "code-reviewer" |
| `description` | What the agent does | "Reviews Python code" |
| `system_prompt` | Agent behavior/expertise | "You are an expert..." |

## Agent Selection

Copilot selects agents based on:

1. **@mention**: Explicitly call `@agent-name`
2. **Context**: Agent description matches the request
3. **Default**: Falls back to general assistant

```python
# Explicit selection
"@reviewer Check this code"

# Implicit selection (Copilot chooses based on context)
"Review this Python function for issues"
```

## Best Practices

1. **Clear system prompts**: Be specific about expertise and behavior
2. **Focused scope**: Each agent should have a clear purpose
3. **Consistent naming**: Use descriptive, memorable names
4. **Appropriate tools**: Give agents only the tools they need
5. **Test prompts**: Iterate on system prompts for best results

## Complete Example

```bash
python recipe/custom_agents.py
```

Demonstrates:
- Code reviewer agent
- SQL expert agent
- Multiple agent sessions
- Dynamic agent creation

## Next Steps

- [Custom Tools](custom-tools.md): Add tools to agents
- [MCP Servers](mcp-servers.md): Extend agent capabilities
- [Custom Providers](custom-providers.md): Use different models for agents
