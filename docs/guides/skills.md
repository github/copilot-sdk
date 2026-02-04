# Custom Skills

Skills are reusable collections of prompts, tools, and configuration that extend Copilot's capabilities. Load skills from directories to give Copilot specialized abilities for specific domains or workflows.

## Overview

A skill is a directory containing:
- **Prompt files** - Instructions that guide Copilot's behavior
- **Tool definitions** - Custom tools the skill provides
- **Configuration** - Metadata about the skill

Skills allow you to:
- Package domain expertise into reusable modules
- Share specialized behaviors across projects
- Organize complex agent configurations
- Enable/disable capabilities per session

## Loading Skills

Specify directories containing skills when creating a session:

<details open>
<summary><strong>Node.js / TypeScript</strong></summary>

```typescript
import { CopilotClient } from "@github/copilot-sdk";

const client = new CopilotClient();
const session = await client.createSession({
    model: "gpt-4.1",
    skillDirectories: [
        "./skills/code-review",
        "./skills/documentation",
        "~/.copilot/skills",  // User-level skills
    ],
});

// Copilot now has access to skills in those directories
await session.sendAndWait({ prompt: "Review this code for security issues" });
```

</details>

<details>
<summary><strong>Python</strong></summary>

```python
from copilot import CopilotClient

async def main():
    client = CopilotClient()
    await client.start()

    session = await client.create_session({
        "model": "gpt-4.1",
        "skill_directories": [
            "./skills/code-review",
            "./skills/documentation",
            "~/.copilot/skills",  # User-level skills
        ],
    })

    # Copilot now has access to skills in those directories
    await session.send_and_wait({"prompt": "Review this code for security issues"})

    await client.stop()
```

</details>

<details>
<summary><strong>Go</strong></summary>

```go
package main

import (
    "context"
    "log"
    copilot "github.com/github/copilot-sdk/go"
)

func main() {
    ctx := context.Background()
    client := copilot.NewClient(nil)
    if err := client.Start(ctx); err != nil {
        log.Fatal(err)
    }
    defer client.Stop()

    session, err := client.CreateSession(ctx, &copilot.SessionConfig{
        Model: "gpt-4.1",
        SkillDirectories: []string{
            "./skills/code-review",
            "./skills/documentation",
            "~/.copilot/skills",  // User-level skills
        },
    })
    if err != nil {
        log.Fatal(err)
    }

    // Copilot now has access to skills in those directories
    _, err = session.SendAndWait(ctx, copilot.MessageOptions{
        Prompt: "Review this code for security issues",
    })
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
    SkillDirectories = new List<string>
    {
        "./skills/code-review",
        "./skills/documentation",
        "~/.copilot/skills",  // User-level skills
    },
});

// Copilot now has access to skills in those directories
await session.SendAndWaitAsync(new MessageOptions
{
    Prompt = "Review this code for security issues"
});
```

</details>

## Disabling Skills

Disable specific skills while keeping others active:

<details open>
<summary><strong>Node.js / TypeScript</strong></summary>

```typescript
const session = await client.createSession({
    skillDirectories: ["./skills"],
    disabledSkills: ["experimental-feature", "deprecated-tool"],
});
```

</details>

<details>
<summary><strong>Python</strong></summary>

```python
session = await client.create_session({
    "skill_directories": ["./skills"],
    "disabled_skills": ["experimental-feature", "deprecated-tool"],
})
```

</details>

<details>
<summary><strong>Go</strong></summary>

<!-- docs-validate: skip -->
```go
session, _ := client.CreateSession(context.Background(), &copilot.SessionConfig{
    SkillDirectories: []string{"./skills"},
    DisabledSkills:   []string{"experimental-feature", "deprecated-tool"},
})
```

</details>

<details>
<summary><strong>.NET</strong></summary>

<!-- docs-validate: skip -->
```csharp
var session = await client.CreateSessionAsync(new SessionConfig
{
    SkillDirectories = new List<string> { "./skills" },
    DisabledSkills = new List<string> { "experimental-feature", "deprecated-tool" },
});
```

</details>

## Skill Directory Structure

A typical skill directory contains:

```
skills/
└── code-review/
    ├── skill.json          # Skill metadata and configuration
    ├── prompts/
    │   ├── system.md       # System prompt additions
    │   └── examples.md     # Few-shot examples
    └── tools/
        └── lint.json       # Tool definitions
```

### skill.json

The skill manifest file:

```json
{
  "name": "code-review",
  "displayName": "Code Review Assistant",
  "description": "Specialized code review capabilities",
  "version": "1.0.0",
  "author": "Your Team",
  "prompts": ["prompts/system.md"],
  "tools": ["tools/lint.json"]
}
```

### Prompt Files

Markdown files that provide context to Copilot:

```markdown
<!-- prompts/system.md -->
# Code Review Guidelines

When reviewing code, always check for:

1. **Security vulnerabilities** - SQL injection, XSS, etc.
2. **Performance issues** - N+1 queries, memory leaks
3. **Code style** - Consistent formatting, naming conventions
4. **Test coverage** - Are critical paths tested?

Provide specific line-number references and suggested fixes.
```

## Configuration Options

### SessionConfig Skill Fields

| Language | Field | Type | Description |
|----------|-------|------|-------------|
| Node.js | `skillDirectories` | `string[]` | Directories to load skills from |
| Node.js | `disabledSkills` | `string[]` | Skills to disable |
| Python | `skill_directories` | `list[str]` | Directories to load skills from |
| Python | `disabled_skills` | `list[str]` | Skills to disable |
| Go | `SkillDirectories` | `[]string` | Directories to load skills from |
| Go | `DisabledSkills` | `[]string` | Skills to disable |
| .NET | `SkillDirectories` | `List<string>` | Directories to load skills from |
| .NET | `DisabledSkills` | `List<string>` | Skills to disable |

## Best Practices

1. **Organize by domain** - Group related skills together (e.g., `skills/security/`, `skills/testing/`)

2. **Version your skills** - Include version numbers in `skill.json` for compatibility tracking

3. **Document dependencies** - Note any tools or MCP servers a skill requires

4. **Test skills in isolation** - Verify skills work before combining them

5. **Use relative paths** - Keep skills portable across environments

## Combining with Other Features

### Skills + Custom Agents

Skills work alongside custom agents:

```typescript
const session = await client.createSession({
    skillDirectories: ["./skills/security"],
    customAgents: [{
        name: "security-auditor",
        description: "Security-focused code reviewer",
        prompt: "Focus on OWASP Top 10 vulnerabilities",
    }],
});
```

### Skills + MCP Servers

Skills can complement MCP server capabilities:

```typescript
const session = await client.createSession({
    skillDirectories: ["./skills/database"],
    mcpServers: {
        postgres: {
            type: "local",
            command: "npx",
            args: ["-y", "@modelcontextprotocol/server-postgres"],
            tools: ["*"],
        },
    },
});
```

## Troubleshooting

### Skills Not Loading

1. **Check path exists** - Verify the directory path is correct
2. **Check permissions** - Ensure the SDK can read the directory
3. **Validate skill.json** - Check for JSON syntax errors
4. **Enable debug logging** - Set `logLevel: "debug"` to see skill loading logs

### Skill Conflicts

If multiple skills define the same tool:
- Later directories in the array take precedence
- Use `disabledSkills` to exclude conflicting skills

## See Also

- [Custom Agents](../getting-started.md#create-custom-agents) - Define specialized AI personas
- [Custom Tools](../getting-started.md#step-4-add-a-custom-tool) - Build your own tools
- [MCP Servers](../mcp/overview.md) - Connect external tool providers
