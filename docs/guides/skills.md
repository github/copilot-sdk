# Custom Skills

Skills are reusable prompt modules that extend Copilot's capabilities. Load skills from directories to give Copilot specialized abilities for specific domains or workflows.

## Overview

A skill is a named directory containing a `SKILL.md` file — a markdown document that provides instructions to Copilot. When loaded, the skill's content is injected into the session context.

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
    ],
    onPermissionRequest: async () => ({ kind: "approved" }),
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
        ],
        "on_permission_request": lambda req: {"kind": "approved"},
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
        },
        OnPermissionRequest: func(req copilot.PermissionRequest, inv copilot.PermissionInvocation) (copilot.PermissionRequestResult, error) {
            return copilot.PermissionRequestResult{Kind: "approved"}, nil
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
    },
    OnPermissionRequest = (req, inv) =>
        Task.FromResult(new PermissionRequestResult { Kind = "approved" }),
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

Each skill is a named subdirectory containing a `SKILL.md` file:

```
skills/
├── code-review/
│   └── SKILL.md
└── documentation/
    └── SKILL.md
```

The `skillDirectories` option points to the parent directory (e.g., `./skills`). The CLI discovers all `SKILL.md` files in immediate subdirectories.

### SKILL.md Format

A `SKILL.md` file is a markdown document with optional YAML frontmatter:

```markdown
---
name: code-review
description: Specialized code review capabilities
---

# Code Review Guidelines

When reviewing code, always check for:

1. **Security vulnerabilities** - SQL injection, XSS, etc.
2. **Performance issues** - N+1 queries, memory leaks
3. **Code style** - Consistent formatting, naming conventions
4. **Test coverage** - Are critical paths tested?

Provide specific line-number references and suggested fixes.
```

The frontmatter fields:
- **`name`** — The skill's identifier (used with `disabledSkills` to selectively disable it). If omitted, the directory name is used.
- **`description`** — A short description of what the skill does.

The markdown body contains the instructions that are injected into the session context when the skill is loaded.

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

2. **Use frontmatter** - Include `name` and `description` in YAML frontmatter for clarity

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
    onPermissionRequest: async () => ({ kind: "approved" }),
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
    onPermissionRequest: async () => ({ kind: "approved" }),
});
```

## Troubleshooting

### Skills Not Loading

1. **Check path exists** - Verify the skill directory path is correct and contains subdirectories with `SKILL.md` files
2. **Check permissions** - Ensure the SDK can read the directory
3. **Check SKILL.md format** - Verify the markdown is well-formed and any YAML frontmatter uses valid syntax
4. **Enable debug logging** - Set `logLevel: "debug"` to see skill loading logs

### Skill Conflicts

If multiple skills provide conflicting instructions:
- Use `disabledSkills` to exclude conflicting skills
- Reorganize skill directories to avoid overlaps

## See Also

- [Custom Agents](../getting-started.md#create-custom-agents) - Define specialized AI personas
- [Custom Tools](../getting-started.md#step-4-add-a-custom-tool) - Build your own tools
- [MCP Servers](../mcp/overview.md) - Connect external tool providers
