# Custom skills

Skills are reusable prompt modules that extend Copilot's capabilities. Load skills from directories to give Copilot specialized abilities for specific domains or workflows.

## Overview

A skill is a named directory containing a `SKILL.md` file—a markdown document that provides instructions to Copilot. When loaded, the skill's content is injected into the session context.

Skills allow you to:
* Package domain expertise into reusable modules
* Share specialized behaviors across projects
* Organize complex agent configurations
* Enable/disable capabilities per session

## Loading skills

Specify directories containing skills when creating a session:

<details open>
<summary><strong>Node.js / TypeScript</strong></summary>

```typescript
import { CopilotClient } from "@github/copilot-sdk";

const client = new CopilotClient();
const session = await client.createSession({
    model: "gpt-5.4",
    skillDirectories: [
        "./skills/code-review",
        "./skills/documentation",
    ],
    onPermissionRequest: async () => ({ kind: "approve-once" }),
});

// Copilot now has access to skills in those directories
await session.sendAndWait({ prompt: "Review this code for security issues" });
```

</details>

<details>
<summary><strong>Python</strong></summary>

```python
from copilot import CopilotClient, PermissionDecisionApproveOnce

async def main():
    client = CopilotClient()
    await client.start()

    session = await client.create_session(
        on_permission_request=lambda req, inv: PermissionDecisionApproveOnce(),
        model="gpt-5.4",
        skill_directories=[
            "./skills/code-review",
            "./skills/documentation",
        ],
    )

    # Copilot now has access to skills in those directories
    await session.send_and_wait("Review this code for security issues")

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
    "github.com/github/copilot-sdk/go/rpc"
)

func main() {
    ctx := context.Background()
    client := copilot.NewClient(nil)
    if err := client.Start(ctx); err != nil {
        log.Fatal(err)
    }
    defer client.Stop()

    session, err := client.CreateSession(ctx, &copilot.SessionConfig{
        Model: "gpt-5.4",
        SkillDirectories: []string{
            "./skills/code-review",
            "./skills/documentation",
        },
        OnPermissionRequest: func(req copilot.PermissionRequest, inv copilot.PermissionInvocation) (rpc.PermissionDecision, error) {
            return &rpc.PermissionDecisionApproveOnce{}, nil
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
using GitHub.Copilot;
using GitHub.Copilot.Rpc;

await using var client = new CopilotClient();
await using var session = await client.CreateSessionAsync(new SessionConfig
{
    Model = "gpt-5.4",
    SkillDirectories = new List<string>
    {
        "./skills/code-review",
        "./skills/documentation",
    },
    OnPermissionRequest = (req, inv) =>
        Task.FromResult(PermissionDecision.ApproveOnce()),
});

// Copilot now has access to skills in those directories
await session.SendAndWaitAsync(new MessageOptions
{
    Prompt = "Review this code for security issues"
});
```

</details>

<details>
<summary><strong>Java</strong></summary>

```java
import com.github.copilot.CopilotClient;
import com.github.copilot.rpc.*;
import java.util.List;

try (var client = new CopilotClient()) {
    client.start().get();

    var session = client.createSession(
        new SessionConfig()
            .setModel("gpt-5.4")
            .setSkillDirectories(List.of(
                "./skills/code-review",
                "./skills/documentation"
            ))
            .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
    ).get();

    // Copilot now has access to skills in those directories
    session.sendAndWait(new MessageOptions()
        .setPrompt("Review this code for security issues")
    ).get();
}
```

</details>

## Disabling skills

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
from copilot.session import PermissionHandler

session = await client.create_session(
    on_permission_request=PermissionHandler.approve_all,
    skill_directories=["./skills"],
    disabled_skills=["experimental-feature", "deprecated-tool"],
)
```

</details>

<details>
<summary><strong>Go</strong></summary>

<!-- docs-validate: hidden -->
```go
package main

import (
	"context"
	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/rpc"
)

func main() {
	ctx := context.Background()
	client := copilot.NewClient(nil)

	session, _ := client.CreateSession(ctx, &copilot.SessionConfig{
		SkillDirectories: []string{"./skills"},
		DisabledSkills:   []string{"experimental-feature", "deprecated-tool"},
		OnPermissionRequest: func(req copilot.PermissionRequest, inv copilot.PermissionInvocation) (rpc.PermissionDecision, error) {
			return &rpc.PermissionDecisionApproveOnce{}, nil
		},
	})
	_ = session
}
```
<!-- /docs-validate: hidden -->

```go
session, _ := client.CreateSession(context.Background(), &copilot.SessionConfig{
    SkillDirectories: []string{"./skills"},
    DisabledSkills:   []string{"experimental-feature", "deprecated-tool"},
})
```

</details>

<details>
<summary><strong>.NET</strong></summary>

<!-- docs-validate: hidden -->
```csharp
using GitHub.Copilot;
using GitHub.Copilot.Rpc;

public static class SkillsExample
{
    public static async Task Main()
    {
        await using var client = new CopilotClient();

        var session = await client.CreateSessionAsync(new SessionConfig
        {
            SkillDirectories = new List<string> { "./skills" },
            DisabledSkills = new List<string> { "experimental-feature", "deprecated-tool" },
            OnPermissionRequest = (req, inv) =>
                Task.FromResult(PermissionDecision.ApproveOnce()),
        });
    }
}
```
<!-- /docs-validate: hidden -->

```csharp
var session = await client.CreateSessionAsync(new SessionConfig
{
    SkillDirectories = new List<string> { "./skills" },
    DisabledSkills = new List<string> { "experimental-feature", "deprecated-tool" },
});
```

</details>

<details>
<summary><strong>Java</strong></summary>

<!-- docs-validate: skip -->
```java
import com.github.copilot.rpc.*;
import java.util.List;

var session = client.createSession(
    new SessionConfig()
        .setSkillDirectories(List.of("./skills"))
        .setDisabledSkills(List.of("experimental-feature", "deprecated-tool"))
        .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
).get();
```

</details>

## Skill directory structure

Each skill is a named subdirectory containing a `SKILL.md` file:

```
skills/
├── code-review/
│   └── SKILL.md
└── documentation/
    └── SKILL.md
```

The `skillDirectories` option points to the parent directory (e.g., `./skills`). The CLI discovers all `SKILL.md` files in immediate subdirectories.

### SKILL.md format

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
* **`name`**: The skill's identifier (used with `disabledSkills` to selectively disable it). If omitted, the directory name is used.
* **`description`**: A short description of what the skill does.

The markdown body contains the instructions that are injected into the session context when the skill is loaded.

## Skill providers (experimental)

> [!NOTE]
> Skill providers are experimental. The API can change in future SDK releases.

A skill provider serves skills from your application's own storage, such as a database in a multi-tenant service, instead of `SKILL.md` files on disk. Provider skills join the session's skill catalog alongside file-based skills. The model loads them on demand through the `skill` tool, and users can invoke them like any other skill.

A provider implements two operations:

* **List skills**: returns catalog metadata for every skill: a `name`, a `description`, and optionally `userInvocable`, `disableModelInvocation`, and `argumentHint`. The runtime calls it when it loads the session's skills.
* **Read skill**: returns the markdown for one skill, or a not-found result if the skill no longer exists. The runtime calls it only when the skill is loaded.

Pass the provider when you create or resume a session:

<details open>
<summary><strong>Node.js / TypeScript</strong></summary>

```typescript
import { approveAll, CopilotClient, type SkillProvider } from "@github/copilot-sdk";

const releaseSkills = new Map([
    [
        "release-notes",
        {
            description: "Writes release notes in the team's format.",
            markdown: "Group changes by feature area and link each pull request.",
        },
    ],
]);

const skillProvider: SkillProvider = {
    listSkills: () =>
        [...releaseSkills].map(([name, skill]) => ({ name, description: skill.description })),
    readSkill: (name) => releaseSkills.get(name)?.markdown ?? null,
};

const client = new CopilotClient();
const session = await client.createSession({
    onPermissionRequest: approveAll,
    skillProvider,
});
```

</details>
<details>
<summary><strong>Python</strong></summary>

```python
from copilot import CopilotClient, SkillProviderDescriptor
from copilot.session import PermissionHandler

RELEASE_SKILLS = {
    "release-notes": (
        "Writes release notes in the team's format.",
        "Group changes by feature area and link each pull request.",
    ),
}


class ReleaseSkills:
    async def list_skills(self) -> list[SkillProviderDescriptor]:
        return [
            SkillProviderDescriptor(name=name, description=description)
            for name, (description, _) in RELEASE_SKILLS.items()
        ]

    async def read_skill(self, name: str) -> str | None:
        skill = RELEASE_SKILLS.get(name)
        return skill[1] if skill else None


async def main():
    client = CopilotClient()
    await client.start()
    session = await client.create_session(
        on_permission_request=PermissionHandler.approve_all,
        skill_provider=ReleaseSkills(),
    )
```

</details>
<details>
<summary><strong>Go</strong></summary>

```go
package main

import (
	"context"
	"fmt"
	"log"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/rpc"
)

type releaseSkill struct {
	description string
	markdown    string
}

type releaseSkills map[string]releaseSkill

func (s releaseSkills) ListSkills(ctx context.Context) ([]rpc.SkillProviderDescriptor, error) {
	descriptors := make([]rpc.SkillProviderDescriptor, 0, len(s))
	for name, skill := range s {
		descriptors = append(descriptors, rpc.SkillProviderDescriptor{Name: name, Description: skill.description})
	}
	return descriptors, nil
}

func (s releaseSkills) ReadSkill(ctx context.Context, name string) (string, error) {
	skill, ok := s[name]
	if !ok {
		return "", fmt.Errorf("%w: %s", copilot.ErrSkillNotFound, name)
	}
	return skill.markdown, nil
}

func main() {
	ctx := context.Background()
	client := copilot.NewClient(nil)
	session, err := client.CreateSession(ctx, &copilot.SessionConfig{
		OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
		SkillProvider: releaseSkills{
			"release-notes": {
				description: "Writes release notes in the team's format.",
				markdown:    "Group changes by feature area and link each pull request.",
			},
		},
	})
	if err != nil {
		log.Fatal(err)
	}
	_ = session
}
```

</details>
<details>
<summary><strong>.NET</strong></summary>

```csharp
#pragma warning disable GHCP001 // Skill providers are experimental.
using GitHub.Copilot;
using GitHub.Copilot.Rpc;

public sealed class ReleaseSkills : ISkillProvider
{
    private readonly Dictionary<string, (string Description, string Markdown)> _skills = new()
    {
        ["release-notes"] = (
            "Writes release notes in the team's format.",
            "Group changes by feature area and link each pull request."),
    };

    public Task<IReadOnlyList<SkillProviderDescriptor>> ListSkillsAsync(CancellationToken cancellationToken) =>
        Task.FromResult<IReadOnlyList<SkillProviderDescriptor>>(
            _skills.Select(skill => new SkillProviderDescriptor
            {
                Name = skill.Key,
                Description = skill.Value.Description,
            }).ToList());

    public Task<string?> ReadSkillAsync(string name, CancellationToken cancellationToken) =>
        Task.FromResult(_skills.TryGetValue(name, out var skill) ? skill.Markdown : null);
}

public static class SkillProviderExample
{
    public static async Task RunAsync(CopilotClient client)
    {
        await using var session = await client.CreateSessionAsync(new SessionConfig
        {
            OnPermissionRequest = PermissionHandler.ApproveAll,
            SkillProvider = new ReleaseSkills(),
        });
    }
}
```

The .NET skill provider types raise the `GHCP001` experimental diagnostic. Suppress it with `#pragma warning disable GHCP001` or a project-level `<NoWarn>GHCP001</NoWarn>`.

</details>
<details>
<summary><strong>Java</strong></summary>

```java
import com.github.copilot.AllowCopilotExperimental;
import com.github.copilot.CopilotClient;
import com.github.copilot.CopilotSession;
import com.github.copilot.SkillProvider;
import com.github.copilot.SkillProviderDescriptor;
import com.github.copilot.rpc.PermissionHandler;
import com.github.copilot.rpc.SessionConfig;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@AllowCopilotExperimental
class ReleaseSkills implements SkillProvider {
    private final Map<String, String> markdown = Map.of(
            "release-notes", "Group changes by feature area and link each pull request.");

    @Override
    public CompletableFuture<List<SkillProviderDescriptor>> listSkills() {
        return CompletableFuture.completedFuture(List.of(new SkillProviderDescriptor(
                "release-notes", "Writes release notes in the team's format.", null, null, null)));
    }

    @Override
    public CompletableFuture<String> readSkill(String name) {
        return CompletableFuture.completedFuture(markdown.get(name));
    }
}

@AllowCopilotExperimental
class SkillProviderExample {
    static CopilotSession createSession(CopilotClient client) throws Exception {
        return client.createSession(new SessionConfig()
                .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                .setSkillProvider(new ReleaseSkills()))
                .get();
    }
}
```

The Java skill provider API is `@CopilotExperimental`, so the consuming class or method must opt in with `@AllowCopilotExperimental` (or compile with `-Acopilot.experimental.allowed=true`). See [Using experimental APIs](../../java/README.md#using-experimental-apis).

</details>
<details>
<summary><strong>Rust</strong></summary>

<!-- docs-validate: skip -->
```rust
use std::sync::Arc;

use async_trait::async_trait;
use github_copilot_sdk::{Error, SessionConfig, SkillProvider, SkillProviderDescriptor};

struct ReleaseSkills;

#[async_trait]
impl SkillProvider for ReleaseSkills {
    async fn list_skills(&self) -> Result<Vec<SkillProviderDescriptor>, Error> {
        Ok(vec![SkillProviderDescriptor {
            name: "release-notes".into(),
            description: "Writes release notes in the team's format.".into(),
            ..Default::default()
        }])
    }

    async fn read_skill(&self, name: &str) -> Result<Option<String>, Error> {
        Ok((name == "release-notes")
            .then(|| "Group changes by feature area and link each pull request.".to_string()))
    }
}

let session = client
    .create_session(
        SessionConfig::default()
            .approve_all_permissions()
            .with_skill_provider(Arc::new(ReleaseSkills)),
    )
    .await?;
```

</details>

### Provider skill content

Read skill returns the skill's `SKILL.md` text. YAML frontmatter is optional:

* Without frontmatter, the whole text is the skill body, and all metadata comes from the listed skill.
* With frontmatter, an omitted field inherits the listed value. A field that you include must match the listed value, or the skill fails to load.
* `allowed-tools` is read only from frontmatter.
* If the first line of the text is `---`, the runtime parses it as frontmatter, so don't start a body-only skill with a Markdown thematic break.

Provider skills are text-only. They have no base directory, so they can't reference bundled scripts, templates, or other files. `skills.list` reports them with the source `sdk` and an empty `path`.

### Serving existing SKILL.md files

To serve `SKILL.md` files that you already have, such as files stored in a database, parse each file's YAML frontmatter once to build its catalog entry, then return the file unchanged from read skill:

* Map `name` and `description` to the listed skill's `name` and `description`. Both are required in the catalog; the runtime doesn't fall back to a folder name or the skill body.
* Map `user-invocable`, `disable-model-invocation`, and `argument-hint` to the matching optional fields when the frontmatter sets them.
* Return the original text, frontmatter included, from read skill. Because the listed values came from the same frontmatter, they match, and the runtime still reads `allowed-tools` from it.

### Trusting provider content

Treat provider skills like skill directories: their content is trusted input. The model follows a skill's instructions, and the frontmatter controls how the skill is invoked. `allowed-tools` doesn't grant permissions in SDK sessions. The runtime reports it in the `allowedTools` field of the `skill.invoked` event, and your permission handler still decides every tool request.

Don't serve text that end users or other tenants can edit unless you would let them author a skill file. If you build skills from user input, generate the frontmatter yourself instead of passing user-supplied frontmatter through.

### Provider limits and errors

The runtime validates the provider's catalog and content:

* Names must match `^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$` and be unique, ignoring case.
* A catalog can contain at most 1,024 skills and 1 MiB of metadata. Each description and argument hint can be at most 1,024 characters.
* Each skill's markdown can be at most 1 MiB of UTF-8.
* Each call must complete within 30 seconds. When the runtime stops waiting for a call, because it timed out, the session ended, or a resume replaced the provider, it cancels the request and ignores any later result. Each SDK passes that cancellation to your provider in its usual way:

  | SDK | Cancellation signal |
  |---|---|
  | Node.js | The `signal` (`AbortSignal`) in the options passed to `listSkills` and `readSkill` is aborted. |
  | Python | The provider's `asyncio` task is cancelled with `asyncio.CancelledError`. Synchronous providers run to completion. |
  | Go | The call's `context.Context` is cancelled. |
  | .NET | The call's `CancellationToken` is cancelled. |
  | Java | The returned `CompletableFuture` is cancelled with `cancel(true)`, which doesn't interrupt running work. |
  | Rust | The provider future is dropped. |

  These signals cover calls that the runtime cancels. When the connection closes or the client is force-stopped, the .NET, Java, and Rust SDKs also cancel calls that are still running. The Node.js, Python, and Go SDKs don't: a running call continues until it returns, and the SDK discards its result.

If listing fails or returns an invalid catalog, the runtime reports the problem in the `errors` list returned by `skills.reload`. A failed reload keeps the provider skills from the last successful list. If reading a skill fails, the `skill` tool reports a generic load failure to the model. The SDK never forwards the text of errors your provider throws or returns, so that text can't leak into the conversation.

### Provider lifecycle

A provider is bound to one session:

* A provider enables skills unless you set `enableSkills` to `false`, which keeps the provider bound but never called. In `mode: "empty"`, skills stay disabled until you set `enableSkills` to `true`.
* The provider is never persisted. Pass it again when you resume a session. Resuming without a provider removes it from the session. Extensions that join the session don't change it.
* A provider skill with the same name as a file-based skill replaces the file-based skill, and `skills.reload` reports a warning.
* Sub-agents use their parent session's provider.
* The runtime can call the provider concurrently, for example from the `skill` tool, user invocations, and custom agents that preload skills, so both operations must be safe for concurrent use.
* Cloud sessions don't support skill providers. The SDK rejects the configuration before it creates the session.

## Configuration options

### SessionConfig skill fields

| Language | Field | Type | Description |
|----------|-------|------|-------------|
| Node.js | `skillDirectories` | `string[]` | Directories to load skills from |
| Node.js | `disabledSkills` | `string[]` | Skills to disable |
| Node.js | `skillProvider` | `SkillProvider` | Experimental provider for SDK-supplied skills |
| Python | `skill_directories` | `list[str]` | Directories to load skills from |
| Python | `disabled_skills` | `list[str]` | Skills to disable |
| Python | `skill_provider` | `SkillProvider` | Experimental provider for SDK-supplied skills |
| Go | `SkillDirectories` | `[]string` | Directories to load skills from |
| Go | `DisabledSkills` | `[]string` | Skills to disable |
| Go | `SkillProvider` | `SkillProvider` | Experimental provider for SDK-supplied skills |
| .NET | `SkillDirectories` | `List<string>` | Directories to load skills from |
| .NET | `DisabledSkills` | `List<string>` | Skills to disable |
| .NET | `SkillProvider` | `ISkillProvider` | Experimental provider for SDK-supplied skills |
| Java | `setSkillProvider` | `SkillProvider` | Experimental provider for SDK-supplied skills |
| Rust | `with_skill_provider` | `Arc<dyn SkillProvider>` | Experimental provider for SDK-supplied skills |

### Built-in skills and `mode: "empty"`

The runtime ships with a set of bundled **built-in** skills that are eligible by
default. When you run the client in `mode: "empty"` (the recommended baseline for
[multi-tenant servers](../setup/multi-tenancy.md)), the SDK excludes every
runtime-bundled built-in skill: it sends an empty `includedBuiltinSkills` list on
the post-create and post-resume options patch, alongside the empty
`installedPlugins` list.

This exclusion is the default, not a permanent restriction. To allow selected
runtime-bundled skills, set `includedBuiltinSkills` (or the language-specific
casing) to their names. You can also opt into your **own** custom skills under
`mode: "empty"`—enable skills and pass your own `skillDirectories`—and those
remain fully usable, including a custom skill that shares a name with a built-in.
Under `mode: "copilot-cli"` the field is omitted unless you set the option.

Custom skill directories are scanned when skills first load, including directories
that are missing or empty at that point. Filesystem changes in those directories
do not automatically refresh the catalog on later turns. After adding, editing,
or removing skills, call your session's `skills.reload` RPC:

| SDK | Reload call |
|-----|-------------|
| Node.js | `await session.rpc.skills.reload()` |
| Python | `await session.rpc.skills.reload()` |
| Go | `session.RPC.Skills.Reload(ctx)` |
| .NET | `await session.Rpc.Skills.ReloadAsync()` |
| Java | `session.getRpc().skills.reload().join()` |
| Rust | `session.rpc().skills().reload().await?` |

Changing the configured skill directories also refreshes the catalog.

## Best practices

1. **Organize by domain** - Group related skills together (e.g., `skills/security/`, `skills/testing/`)

1. **Use frontmatter** - Include `name` and `description` in YAML frontmatter for clarity

1. **Document dependencies** - Note any tools or MCP servers a skill requires

1. **Test skills in isolation** - Verify skills work before combining them

1. **Use relative paths** - Keep skills portable across environments

## Combining with other features

### Skills + custom agents

Skills listed in an agent's `skills` field are **eagerly preloaded**—their full content is injected into the agent's context at startup, so the agent has access to the skill instructions immediately without needing to invoke a skill tool. Skill names are resolved from the session-level `skillDirectories`.

```typescript
const session = await client.createSession({
    skillDirectories: ["./skills/security"],
    customAgents: [{
        name: "security-auditor",
        description: "Security-focused code reviewer",
        prompt: "Focus on OWASP Top 10 vulnerabilities",
        skills: ["security-scan", "dependency-check"],
    }],
    onPermissionRequest: async () => ({ kind: "approve-once" }),
});
```
> [!NOTE]
> Skills are opt-in—when `skills` is omitted, no skill content is injected. Sub-agents do not inherit skills from the parent; you must list them explicitly per agent.

### Skills + MCP servers

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
    onPermissionRequest: async () => ({ kind: "approve-once" }),
});
```

## Troubleshooting

### Skills not loading

1. **Check path exists** - Verify the skill directory path is correct and contains subdirectories with `SKILL.md` files
1. **Check permissions** - Ensure the SDK can read the directory
1. **Check SKILL.md format** - Verify the markdown is well-formed and any YAML frontmatter uses valid syntax
1. **Enable debug logging** - Set `logLevel: "debug"` to see skill loading logs

### Skill conflicts

If multiple skills provide conflicting instructions:
* Use `disabledSkills` to exclude conflicting skills
* Reorganize skill directories to avoid overlaps

## See also

* [Custom Agents](../getting-started.md#create-custom-agents) - Define specialized AI personas
* [Custom Tools](../getting-started.md#step-4-add-a-custom-tool) - Build your own tools
* [MCP Servers](./mcp.md) - Connect external tool providers