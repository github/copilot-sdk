#!/usr/bin/env python3
"""
Custom Agents - Creating specialized AI assistants with the Copilot SDK.
Run: python custom_agents.py
"""

import asyncio

from pydantic import BaseModel, Field

from copilot import CopilotClient
from copilot.tools import define_tool
from copilot.types import CustomAgentConfig, SessionEventType


# =============================================================================
# Custom Agent Configurations
# =============================================================================


def create_code_reviewer_agent():
    """Create a code review specialist agent."""
    return CustomAgentConfig(
        name="code-reviewer",
        display_name="Code Review Expert",
        description="Specializes in code review, finding bugs, and suggesting improvements",
        prompt="""
You are an expert code reviewer with decades of experience in software development.

Your responsibilities:
1. Review code for bugs, security issues, and performance problems
2. Suggest improvements for readability and maintainability
3. Identify potential edge cases and error handling gaps
4. Recommend best practices and design patterns
5. Be constructive and educational in your feedback

When reviewing code:
- Start with a high-level summary
- List specific issues with line references
- Categorize issues by severity (critical, major, minor, suggestion)
- Provide example fixes when helpful

Your tone should be professional, constructive, and encouraging.
""",
        tools=None,  # Use default tools
        infer=True,  # Available for model inference
    )


def create_sql_expert_agent():
    """Create a SQL database expert agent."""
    return CustomAgentConfig(
        name="sql-expert",
        display_name="SQL Database Expert",
        description="Specializes in SQL queries, database design, and optimization",
        prompt="""
You are a senior database engineer and SQL expert.

Your expertise includes:
1. Writing efficient SQL queries for various databases
2. Database schema design and normalization
3. Query optimization and performance tuning
4. Index strategies and execution plan analysis
5. Database migration strategies
6. Data modeling best practices

When helping with SQL:
- Ask clarifying questions about the database system (PostgreSQL, MySQL, SQLite, etc.)
- Explain query logic step by step
- Warn about potential performance issues
- Suggest indexes when appropriate
- Consider edge cases and NULL handling

Always format SQL queries properly with clear indentation.
""",
        tools=["sql_query", "explain_plan"],  # Only allow specific tools
        infer=True,
    )


def create_documentation_agent():
    """Create a technical documentation specialist agent."""
    return CustomAgentConfig(
        name="doc-writer",
        display_name="Documentation Writer",
        description="Writes clear, comprehensive technical documentation",
        prompt="""
You are a technical writer who creates clear, helpful documentation.

Your skills include:
1. Writing API documentation with examples
2. Creating user guides and tutorials
3. Writing README files and project documentation
4. Generating code comments and docstrings
5. Creating architecture documentation

Documentation principles:
- Start with the "why" before the "how"
- Include practical examples for every concept
- Use consistent formatting and structure
- Write for your audience (beginners vs experts)
- Keep it concise but complete

Output formats you excel at:
- Markdown documentation
- JSDoc/TSDoc/docstrings
- OpenAPI/Swagger specs
- Mermaid diagrams
""",
        tools=None,  # All tools available
        infer=True,
    )


def create_security_auditor_agent():
    """Create a security-focused code auditor agent."""
    return CustomAgentConfig(
        name="security-auditor",
        display_name="Security Auditor",
        description="Finds security vulnerabilities and suggests fixes",
        prompt="""
You are a cybersecurity expert specializing in application security.

Your focus areas:
1. OWASP Top 10 vulnerabilities
2. Authentication and authorization flaws
3. Input validation and injection attacks
4. Cryptographic issues
5. Secrets management
6. Dependency vulnerabilities
7. API security

When auditing code:
- Prioritize findings by severity (Critical, High, Medium, Low)
- Provide clear reproduction steps
- Reference CVEs and CWEs where applicable
- Suggest specific remediation steps
- Consider both code and configuration issues

Be thorough but avoid false positives. Explain the actual risk.
""",
        tools=["search_cve", "dependency_check"],  # Security-focused tools
        infer=True,
    )


# =============================================================================
# Custom Tools for Agents
# =============================================================================


class ExplainPlanParams(BaseModel):
    """Parameters for SQL explain plan tool."""

    query: str = Field(description="The SQL query to explain")


@define_tool(description="Explain a SQL query execution plan (simulated)")
def explain_plan(params):
    """Simulated SQL explain plan tool."""
    return f"""
Execution Plan for: {params.query[:50]}...

| Operation          | Rows  | Cost   |
|--------------------|-------|--------|
| Seq Scan           | 1000  | 100.00 |
| Index Scan         | 50    | 5.50   |
| Hash Join          | 50    | 10.25 |

Note: Simulated plan for demonstration.
"""


class SQLQueryParams(BaseModel):
    """Parameters for SQL query tool."""

    query: str = Field(description="SQL query to execute")
    database: str = Field(default="demo.db", description="Database to query")


@define_tool(description="Execute a SQL query (simulated)")
def sql_query(params):
    """Simulated SQL query tool."""
    return f"""
Query executed on {params.database}:
{params.query[:100]}...

Results (simulated):
| id | name      | value  |
|----|-----------|--------|
| 1  | Example 1 | 100    |
| 2  | Example 2 | 200    |

2 rows returned.
"""


# =============================================================================
# Event Handler
# =============================================================================


def create_event_handler():
    """Create an event handler for agent demonstrations."""

    def handler(event):
        if event.type == SessionEventType.ASSISTANT_MESSAGE:
            print(f"\n🤖 {event.data.content}\n")
        elif event.type == SessionEventType.TOOL_EXECUTION_START:
            print(f"  ⚙️ {event.data.tool_name}")
        elif event.type == SessionEventType.SESSION_ERROR:
            message = getattr(event.data, "message", str(event.data))
            print(f"  ❌ Error: {message}")

    return handler


# =============================================================================
# Demo: Single Agent
# =============================================================================


async def demo_single_agent():
    """Demonstrate using a single custom agent."""
    print("\n=== Single Custom Agent Demo ===\n")

    client = CopilotClient()

    try:
        await client.start()

        # Create session with code reviewer agent
        session = await client.create_session(
            {
                "custom_agents": [create_code_reviewer_agent()],
            }
        )

        session.on(create_event_handler())

        print("Using Code Review Agent to review a code snippet...\n")

        await session.send_and_wait(
            {
                "prompt": """
@code-reviewer Please review this Python code:

```python
def get_user(id):
    conn = db.connect()
    result = conn.execute(f"SELECT * FROM users WHERE id = {id}")
    return result.fetchone()
```
"""
            },
            timeout=120.0,
        )

        await session.destroy()

    finally:
        await client.stop()


# =============================================================================
# Demo: Multiple Agents
# =============================================================================


async def demo_multiple_agents():
    """Demonstrate multiple custom agents in one session."""
    print("\n=== Multiple Custom Agents Demo ===\n")

    client = CopilotClient()

    try:
        await client.start()

        # Create session with multiple agents
        session = await client.create_session(
            {
                "custom_agents": [
                    create_code_reviewer_agent(),
                    create_documentation_agent(),
                    create_security_auditor_agent(),
                ],
            }
        )

        session.on(create_event_handler())

        print("Multiple agents available:")
        print("  - @code-reviewer: Code review expert")
        print("  - @doc-writer: Documentation specialist")
        print("  - @security-auditor: Security expert")
        print()

        # Use the documentation agent
        print("Asking the documentation agent for help...\n")

        await session.send_and_wait(
            {
                "prompt": """
@doc-writer Write a docstring for this function:

def calculate_discount(price, percentage, min_amount=0):
    if price < min_amount:
        return price
    return price * (1 - percentage / 100)
"""
            },
            timeout=120.0,
        )

        await session.destroy()

    finally:
        await client.stop()


# =============================================================================
# Demo: Agent with Tools
# =============================================================================


async def demo_agent_with_tools():
    """Demonstrate an agent with access to specific tools."""
    print("\n=== Agent with Custom Tools Demo ===\n")

    client = CopilotClient()

    try:
        await client.start()

        # Create session with SQL expert agent and its tools
        session = await client.create_session(
            {
                "custom_agents": [create_sql_expert_agent()],
                "tools": [sql_query, explain_plan],
            }
        )

        session.on(create_event_handler())

        print("Using SQL Expert Agent with database tools...\n")

        await session.send_and_wait(
            {
                "prompt": """
@sql-expert Help me optimize this query. First show me the execution plan,
then suggest improvements:

SELECT u.name, COUNT(o.id) as order_count
FROM users u
LEFT JOIN orders o ON u.id = o.user_id
WHERE u.created_at > '2024-01-01'
GROUP BY u.id, u.name
ORDER BY order_count DESC
"""
            },
            timeout=120.0,
        )

        await session.destroy()

    finally:
        await client.stop()


# =============================================================================
# Demo: Agent Inference
# =============================================================================


async def demo_agent_inference():
    """Demonstrate automatic agent selection based on context."""
    print("\n=== Agent Inference Demo ===\n")

    client = CopilotClient()

    try:
        await client.start()

        # Create session with multiple inference-enabled agents
        session = await client.create_session(
            {
                "custom_agents": [
                    create_code_reviewer_agent(),
                    create_documentation_agent(),
                    create_sql_expert_agent(),
                ],
                "tools": [sql_query, explain_plan],
            }
        )

        session.on(create_event_handler())

        print("Agents available for inference:")
        print("  - Code Reviewer")
        print("  - Documentation Writer")
        print("  - SQL Expert")
        print()

        # The model should infer which agent to use
        prompts = [
            "Write a README for a Python CLI tool",
            "Review this code: `if x = 5: print('hello')`",
            "How do I write a JOIN query in PostgreSQL?",
        ]

        for prompt in prompts:
            print(f"Prompt: {prompt}\n")
            await session.send_and_wait({"prompt": prompt}, timeout=60.0)
            print("-" * 50)

        await session.destroy()

    finally:
        await client.stop()


# =============================================================================
# Dynamic Agent Creation
# =============================================================================


def create_dynamic_agent(name, specialty, instructions):
    """Create a custom agent dynamically based on configuration."""
    return CustomAgentConfig(
        name=name,
        display_name=specialty,
        description=f"Specialized assistant for {specialty}",
        prompt=f"""
You are a specialized assistant for {specialty}.

Instructions:
{instructions}

Always be helpful, accurate, and professional.
""",
        tools=None,  # Use default tools
        infer=True,
    )


async def demo_dynamic_agents():
    """Demonstrate creating agents dynamically."""
    print("\n=== Dynamic Agent Creation Demo ===\n")

    client = CopilotClient()

    try:
        await client.start()

        # Create agents dynamically based on configuration
        agent_configs = [
            ("python-helper", "Python Programming", "Help with Python code, libraries, and best practices."),
            ("api-designer", "REST API Design", "Design RESTful APIs following best practices."),
        ]

        agents = [
            create_dynamic_agent(name, specialty, instructions)
            for name, specialty, instructions in agent_configs
        ]

        session = await client.create_session({"custom_agents": agents})
        session.on(create_event_handler())

        print(f"Created {len(agents)} dynamic agents")
        for name, specialty, _ in agent_configs:
            print(f"  - @{name}: {specialty}")
        print()

        # Test one of the dynamic agents
        await session.send_and_wait(
            {"prompt": "@python-helper What's the difference between a list and a tuple?"},
            timeout=60.0,
        )

        await session.destroy()

    finally:
        await client.stop()


# =============================================================================
# Main
# =============================================================================


async def main():
    """Run custom agent demonstrations."""
    print("=" * 60)
    print("Custom Agents")
    print("=" * 60)

    await demo_single_agent()
    await demo_multiple_agents()
    await demo_agent_with_tools()
    await demo_dynamic_agents()

    print("\n" + "=" * 60)
    print("All demos completed!")
    print("=" * 60)


if __name__ == "__main__":
    asyncio.run(main())
