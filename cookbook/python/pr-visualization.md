# PR Visualization and Analytics

Build interactive CLI tools for GitHub PR analysis and visualization.

> **Skill Level:** Intermediate to Advanced
>
> **Runnable Example:** [recipe/pr_visualization.py](recipe/pr_visualization.py)
>
> ```bash
> cd recipe && pip install -r requirements.txt
> # Auto-detect from current git repo
> python pr_visualization.py
>
> # Specify a repo explicitly
> python pr_visualization.py --repo github/copilot-sdk
> ```

## Overview

This recipe demonstrates PR analytics capabilities:

- Auto-detecting GitHub repositories
- PR age analysis and charting
- Author and review status analysis
- Interactive follow-up queries
- AI-powered data visualization

## Prerequisites

```bash
pip install copilot-sdk
```

## Quick Start

```python
import asyncio
from copilot import CopilotClient
from copilot.types import SessionEventType

async def main():
    client = CopilotClient()
    await client.start()

    session = await client.create_session({
        "system_message": {
            "content": "You are analyzing PRs for github/copilot-sdk"
        }
    })

    def handle_event(event):
        if event.type == SessionEventType.ASSISTANT_MESSAGE:
            print(f"🤖 {event.data.content}")

    session.on(handle_event)

    await session.send_and_wait({
        "prompt": """
Fetch open pull requests for the repo.
Calculate the age of each PR.
Generate a bar chart showing PR age distribution.
Save as 'pr-chart.png'.
"""
    }, timeout=300.0)

    await session.destroy()
    await client.stop()

asyncio.run(main())
```

## Repository Detection

Auto-detect GitHub repo from git remote:

```python
import subprocess
import re

def get_github_remote():
    """Detect GitHub repository from git remote."""
    try:
        result = subprocess.run(
            ["git", "remote", "get-url", "origin"],
            capture_output=True, text=True, check=True
        )
        remote = result.stdout.strip()

        # SSH format: git@github.com:owner/repo.git
        ssh = re.search(r"git@github\.com:(.+/.+?)(?:\.git)?$", remote)
        if ssh:
            return ssh.group(1)

        # HTTPS format: https://github.com/owner/repo.git
        https = re.search(r"https://github\.com/(.+/.+?)(?:\.git)?$", remote)
        if https:
            return https.group(1)

    except Exception:
        pass
    return None

# Usage
repo = get_github_remote() or input("Enter repo (owner/repo): ")
```

## Analysis Types

### PR Age Analysis

```python
await session.send_and_wait({
    "prompt": """
Fetch open PRs and analyze their age:
1. Calculate days open for each PR
2. Group into buckets: <1 day, 1-3 days, 3-7 days, 7+ days
3. Generate a bar chart
4. List the 5 oldest PRs
"""
})
```

### Author Analysis

```python
await session.send_and_wait({
    "prompt": """
Analyze PRs by author:
1. Count PRs per author
2. Show average review time per author
3. Generate a pie chart of PR distribution
4. Identify most active contributors
"""
})
```

### Review Status Analysis

```python
await session.send_and_wait({
    "prompt": """
Analyze review status of open PRs:
1. Count: needs review, approved, changes requested
2. Calculate average time to first review
3. Identify PRs without any reviews
4. Generate a status breakdown chart
"""
})
```

## Interactive CLI

Build an interactive analysis tool:

```python
async def interactive_analysis(session, repo):
    """Interactive PR analysis loop."""

    # Initial analysis
    await session.send_and_wait({
        "prompt": f"Analyze open PRs for {repo}: count, average age, health summary"
    })

    print("\n💡 Ask follow-up questions or type 'exit' to quit")
    print("Examples:")
    print("  - 'Show the 5 oldest PRs'")
    print("  - 'Group by author'")
    print("  - 'Generate a pie chart'")
    print("  - 'Check for stale PRs'")

    while True:
        try:
            query = input("\nYou: ").strip()
            if query.lower() in ['exit', 'quit']:
                break
            if query:
                await session.send_and_wait({"prompt": query}, timeout=300.0)
        except (EOFError, KeyboardInterrupt):
            break
```

## Event Handling

Track analysis progress:

```python
def create_event_handler():
    """Create event handler for analysis visibility."""
    def handler(event):
        if event.type == SessionEventType.ASSISTANT_MESSAGE:
            print(f"\n🤖 {event.data.content}")
        elif event.type == SessionEventType.TOOL_EXECUTION_START:
            print(f"  ⚙️  Running: {event.data.tool_name}")
        elif event.type == SessionEventType.TOOL_EXECUTION_COMPLETE:
            print(f"  ✓ Completed")
        elif event.type == SessionEventType.SESSION_ERROR:
            print(f"  ❌ Error: {event.data.message}")

    return handler

session.on(create_event_handler())
```

## Chart Generation

Copilot can generate various charts:

```python
# Bar chart
await session.send_and_wait({
    "prompt": "Generate a bar chart of PR ages, save as pr-ages.png"
})

# Pie chart
await session.send_and_wait({
    "prompt": "Generate a pie chart of PRs by status, save as pr-status.png"
})

# Timeline
await session.send_and_wait({
    "prompt": "Generate a timeline of PR creation dates, save as pr-timeline.png"
})
```

## Why Use Copilot for This?

| Aspect | Custom Code | Copilot Approach |
|--------|-------------|------------------|
| Complexity | High (GitHub API, matplotlib) | **Minimal** |
| Maintenance | You maintain | **Copilot maintains** |
| Flexibility | Fixed logic | **AI-determined** |
| Chart types | What you coded | **Any type** |
| Grouping | Hardcoded | **Intelligent** |

## System Message Configuration

Set up context for better analysis:

```python
session = await client.create_session({
    "system_message": {
        "content": f"""
<context>
Repository: {owner}/{repo_name}
Working directory: {os.getcwd()}
</context>

<instructions>
- Use GitHub MCP Server for PR data
- Use file tools to save charts
- Be concise in responses
- Focus on actionable insights
</instructions>
"""
    }
})
```

## Best Practices

1. **Set appropriate timeouts**: GitHub API + chart generation can take time
2. **Use system messages**: Provide clear context about the repository
3. **Handle rate limits**: GitHub API has rate limits
4. **Save charts locally**: Specify save paths in the current directory
5. **Interactive follow-up**: Allow users to refine analysis

## Complete Example

```bash
python recipe/pr_visualization.py
```

Demonstrates:
- Repository auto-detection
- PR age analysis and charting
- Interactive follow-up queries
- Multiple analysis types

## Next Steps

- [Custom Tools](custom-tools.md): Create specialized PR analysis tools
- [MCP Servers](mcp-servers.md): Configure GitHub MCP integration
- [Streaming Responses](streaming-responses.md): Real-time analysis updates
