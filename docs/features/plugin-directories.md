# Plugin directories

A **plugin** is a directory that bundles skills, MCP servers, and client-specific capabilities behind a single manifest. Pointing the SDK at a plugin directory lets the runtime discover its supported components, so you can ship reusable capability packs without wiring each component into your host application.

This guide explains the plugin folder layout, how to load a plugin from a directory, when to use plugin directories vs. registering individual extensions, and how to make plugin sets deterministic.

## When to use plugin directories

Use a plugin directory when you want to:

* **Distribute a bundle of capabilities** as one unit — e.g., a "TypeScript reviewer" pack with a skill, a `preToolUse` hook that enforces lint, and a custom agent that runs the reviewer.
* **Vendor capability packs into a repository** so every clone of the host application loads the same extensions deterministically.
* **Develop a plugin locally** before publishing it to a marketplace.
* **Override or extend** a marketplace-installed plugin with a local checkout for testing.

If you only need to add a single MCP server, a single hook, or a single custom agent, you can register it inline via the SDK config (`mcpServers`, `hooks`, `customAgents`). Plugin directories are most useful once you have three or more related extensions that ship together.

## Plugin folder layout

### Agent Plugins

For new plugins, use the published [Agent Plugins specification](https://agent-plugins.org/specification) (1.0.0). It defines portable skills and MCP servers; other capabilities use client-specific namespaces. See the [shared plugin reference](https://docs.github.com/en/copilot/reference/plugin-and-marketplace-reference) and [Copilot CLI authoring reference](https://docs.github.com/en/copilot/reference/copilot-cli-reference/cli-plugin-reference) for supported components. [VS Code's authoring reference](https://code.visualstudio.com/docs/agents/reference/agent-plugin-reference) documents its own support; do not assume every client loads the same components.

A small plugin with a skill and a Copilot agent looks like this:

```text
my-plugin/
├── plugin.json
├── skills/
│   └── lint-fix/
│       └── SKILL.md
└── com.github.copilot/
    └── agents/
        └── code-reviewer.agent.md
```

At the plugin root, `plugin.json` declares the canonical schema:

```json
{
  "$schema": "https://agent-plugins.org/schemas/1.0.0/plugin.schema.json",
  "name": "my-plugin",
  "description": "Lint fixes and code review"
}
```

The runtime also recognizes the exact identifier `https://agent-plugins.org/schemas/1.1.0/plugin.schema.json`.

Skills are discovered only from immediate `skills/*/SKILL.md` entries. To add portable MCP servers, use root `mcp.json` with `$schema` set to `https://agent-plugins.org/schemas/1.0.0/mcp.schema.json` (or the matching 1.1.0 identifier when the manifest declares 1.1.0). The MCP schema version must match `plugin.json`.

Copilot-specific components live under `com.github.copilot/` and are not portable Agent Plugins capabilities. Their locations are listed below. The manifest's `extensions` object holds client data keyed by namespace, not component paths. Do not add legacy top-level `skills`, `agents`, or other component-path fields to a spec manifest.

> [!WARNING]
> Adding the Agent Plugins `$schema` changes discovery behavior; it does not migrate existing files. Move legacy root agents, commands, rules, hooks, LSP configuration, and extensions into `com.github.copilot/`, and use `skills/*/SKILL.md` and root `mcp.json` for portable components.

### Legacy compatibility

Plugins that do not declare an Agent Plugins schema retain legacy discovery and configurable component paths. The runtime accepts `.plugin/plugin.json`, root `plugin.json`, `.github/plugin/plugin.json`, or `.claude-plugin/plugin.json`, in that order. A root-level `SKILL.md` also qualifies as a legacy skill-only plugin. An unsupported Agent Plugins schema version is rejected, not treated as legacy.

An existing legacy plugin can keep this layout:

```
my-plugin/
├── plugin.json              # manifest (required unless using SKILL.md only)
├── SKILL.md                 # optional: top-level skill
├── hooks.json               # optional: hooks config
├── .mcp.json                # optional: MCP server config
├── agents/                  # optional: custom agents (one .md file per agent)
│   └── code-reviewer.md
└── skills/                  # optional: additional skills
    └── lint-fix/
        └── SKILL.md
```

Components are optional. For legacy manifest fields and discovery locations, see the [Copilot CLI plugin reference](https://docs.github.com/en/copilot/reference/copilot-cli-reference/cli-plugin-reference).

## Loading a plugin directory from the SDK

Plugin directories are loaded by passing `--plugin-dir <path>` to the Copilot CLI when the SDK spawns it. Each language exposes this through the runtime connection's extra-args option. The flag can be repeated to load multiple plugins.

<details open>
<summary><strong>Node.js / TypeScript</strong></summary>

<!-- docs-validate: hidden -->
```typescript
import { CopilotClient, RuntimeConnection } from "@github/copilot-sdk";

async function main() {
  const client = new CopilotClient({
    connection: RuntimeConnection.forStdio({
      args: [
        "--plugin-dir", "./plugins/code-reviewer",
        "--plugin-dir", "./plugins/lint-fix",
      ],
    }),
  });

  await client.start();
}

main();
```
<!-- /docs-validate: hidden -->

```typescript
import { CopilotClient, RuntimeConnection } from "@github/copilot-sdk";

const client = new CopilotClient({
  connection: RuntimeConnection.forStdio({
    args: [
      "--plugin-dir", "./plugins/code-reviewer",
      "--plugin-dir", "./plugins/lint-fix",
    ],
  }),
});

await client.start();
```

</details>

<details>
<summary><strong>Python</strong></summary>

<!-- docs-validate: wrap-async -->
```python
from copilot import CopilotClient, StdioRuntimeConnection

client = CopilotClient(
    connection=StdioRuntimeConnection(
        args=(
            "--plugin-dir", "./plugins/code-reviewer",
            "--plugin-dir", "./plugins/lint-fix",
        ),
    ),
)
await client.start()
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
)

func main() {
	ctx := context.Background()
	client := copilot.NewClient(&copilot.ClientOptions{
		Connection: copilot.StdioConnection{
			Args: []string{
				"--plugin-dir", "./plugins/code-reviewer",
				"--plugin-dir", "./plugins/lint-fix",
			},
		},
	})
	if err := client.Start(ctx); err != nil {
		return
	}
}
```
<!-- /docs-validate: hidden -->

```go
client := copilot.NewClient(&copilot.ClientOptions{
    Connection: copilot.StdioConnection{
        Args: []string{
            "--plugin-dir", "./plugins/code-reviewer",
            "--plugin-dir", "./plugins/lint-fix",
        },
    },
})
if err := client.Start(ctx); err != nil {
    return err
}
```

</details>

<details>
<summary><strong>.NET</strong></summary>

```csharp
using GitHub.Copilot;

await using var client = new CopilotClient(new CopilotClientOptions
{
    Connection = RuntimeConnection.ForStdio(args: new[]
    {
        "--plugin-dir", "./plugins/code-reviewer",
        "--plugin-dir", "./plugins/lint-fix",
    }),
});

await client.StartAsync();
```

</details>

<details>
<summary><strong>Java</strong></summary>

<!-- docs-validate: hidden -->
```java
import com.github.copilot.CopilotClient;
import com.github.copilot.rpc.CopilotClientOptions;

public class PluginDirectoriesExample {
    public static void main(String[] args) throws Exception {
        var options = new CopilotClientOptions()
            .setCliArgs(new String[] {
                "--plugin-dir", "./plugins/code-reviewer",
                "--plugin-dir", "./plugins/lint-fix",
            });

        var client = new CopilotClient(options);
        client.start().get();
    }
}
```
<!-- /docs-validate: hidden -->

```java
var options = new CopilotClientOptions()
    .setCliArgs(new String[] {
        "--plugin-dir", "./plugins/code-reviewer",
        "--plugin-dir", "./plugins/lint-fix",
    });

var client = new CopilotClient(options);
client.start().get();
```

</details>

<details>
<summary><strong>Rust</strong></summary>

<!-- docs-validate: hidden -->
```rust
use github_copilot_sdk::{Client, ClientOptions};

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let _client = Client::start(
        ClientOptions::new().with_extra_args([
            "--plugin-dir", "./plugins/code-reviewer",
            "--plugin-dir", "./plugins/lint-fix",
        ]),
    )
    .await?;
    Ok(())
}
```
<!-- /docs-validate: hidden -->

```rust
use github_copilot_sdk::{Client, ClientOptions};

let client = Client::start(
    ClientOptions::new().with_extra_args([
        "--plugin-dir", "./plugins/code-reviewer",
        "--plugin-dir", "./plugins/lint-fix",
    ]),
)
.await?;
```

</details>

> The example above uses an stdio runtime connection — the default when the SDK bundles the CLI. If you connect to an external runtime via a URL (`forUri` / `ForUri`), pass `--plugin-dir` to the long-running CLI server when you start it; the SDK does not forward `--plugin-dir` to runtimes it didn't spawn.

## Per-session plugin directories

`--plugin-dir` is a launch argument, so its plugin set is shared by sessions created against that CLI process. When sessions need additional, session-specific plugins, or when the SDK is connected to a runtime it did not spawn, pass the directories on the session config instead. They travel in the `session.create` and `session.resume` payloads over JSON-RPC rather than as process arguments. The connected runtime must support this option.

<!-- docs-validate: hidden -->
```typescript
import { CopilotClient } from "@github/copilot-sdk";

async function main() {
  const client = new CopilotClient();
  await client.start();

  const session = await client.createSession({
    pluginDirectories: ["./plugins/code-reviewer"],
  });
}

main();
```
<!-- /docs-validate: hidden -->

```typescript
import { CopilotClient } from "@github/copilot-sdk";

const client = new CopilotClient();
await client.start();

const session = await client.createSession({
  pluginDirectories: ["./plugins/code-reviewer"],
});
```

Relative paths resolve against `workingDirectory`, or the runtime working directory when that is unset, so absolute paths are recommended. Entries that do not resolve are logged and skipped rather than failing session creation. The option is an explicit opt-in, which means plugin agents and rules load even when `enableConfigDiscovery` is false. Assets loaded this way sit between project sources and personal or home sources in the session-wide precedence order.

The equivalent option in each SDK is:

| SDK | Session option |
|---|---|
| Node.js / TypeScript | `pluginDirectories: string[]` |
| Python | `plugin_directories=[...]` |
| Go | `PluginDirectories: []string{...}` |
| .NET | `PluginDirectories = [...]` |
| Java | `.setPluginDirectories(List.of(...))` |
| Rust | `.with_plugin_directories([...])` |

## Trusted host-bundled plugin directories

Applications that ship their own trusted plugins can register them as a client startup option. The SDK sends the complete ordered set after connecting and verifying the protocol, before `start` returns or any session can be created. Paths must be absolute; leaving the option unset or empty makes no RPC call.

<!-- docs-validate: hidden -->
```typescript
import { CopilotClient } from "@github/copilot-sdk";

async function main() {
  const client = new CopilotClient({
    builtinPluginDirectories: [
      "/opt/my-app/copilot-plugins/core",
      "/opt/my-app/copilot-plugins/github",
    ],
  });
  await client.start();
}

main();
```
<!-- /docs-validate: hidden -->

The equivalent option in each SDK is:

| SDK | Startup option |
|---|---|
| Node.js / TypeScript | `builtinPluginDirectories: string[]` |
| Python | `builtin_plugin_directories=[...]` |
| Go | `BuiltinPluginDirectories: []string{...}` |
| .NET | `BuiltinPluginDirectories = [...]` |
| Java | `.setBuiltinPluginDirectories(List.of(Path.of(...)))` |
| Rust | `.with_builtin_plugin_directories([...])` |

This is a trust boundary for plugins bundled and controlled by the host application. It is distinct from `--plugin-dir`, which is a CLI process launch argument for explicitly loading ordinary plugin directories. The startup option also works when connecting to an existing runtime that supports built-in plugin registration, because it is sent over JSON-RPC rather than forwarded as a process argument.

The bundled opt-in plugins `computer-use` and `browser-use` are the exception: they contribute no skills, hooks or MCP servers until enabled through `enabledPlugins`, `client.rpc.plugins.enable({ names: ["browser-use"] })`, or settings. Changes apply to sessions created afterwards. Managed `enabledPlugins["browser-use"]` forces Browser Use on or off regardless of user preferences. Computer Use also accepts the legacy `enabledMcpServers` preference; its managed policy veto is `features.computerUse=false`, not `enabledPlugins["computer-use"]`.

## What a plugin can contribute

Launch-time directories are shared across sessions; `pluginDirectories` applies to the session that supplies it. The runtime merges the active plugin's components with anything you register inline:

| Plugin contributes | Agent Plugins location | Visible to session as |
|---|---|---|
| Skills | `skills/*/SKILL.md` | Items in `session.skills.list()`; injectable by name |
| MCP servers | `mcp.json` | Tools and resources reachable through `session.mcp.*` |
| Custom agents | `com.github.copilot/agents/*.md` | Dispatchable via the `task(agent_type=...)` tool |
| Commands | `com.github.copilot/commands/` | Plugin slash commands |
| Rules | `com.github.copilot/rules/` | Instructions applied by the runtime |
| Hooks | `com.github.copilot/hooks/hooks.json` | Fired alongside hooks registered via the SDK |
| LSP servers | `com.github.copilot/lsp.json` | Initialized via `session.lsp.initialize(...)` |
| Extensions | `com.github.copilot/extensions/` | Runtime extension registrations |

Legacy plugins use their legacy discovery locations and manifest overrides instead.

Plugin agents are first-class sub-agents in [fleet mode](./fleet-mode.md): a parent agent can dispatch them by `agent_type`, and the runtime fires the `subagentStart` / `subagentStop` hooks for them like any other sub-agent.

## Plugin-dir vs marketplace plugins

The runtime has two ways to install plugins, and both end up looking the same to a session:

* **Marketplace / direct-repo plugins** are installed persistently through the CLI's `/plugin` slash command. They are *ambient* and participate in the session's plugin discovery and activation rules.
* **`--plugin-dir` plugins** are *explicit and ephemeral* — they only apply to the CLI process you launched with that flag. They take precedence over ambient discovery and are de-duplicated against marketplace entries with the same cache path, so the same plugin won't load twice when both surfaces reference it.

For SDK-driven applications, use `--plugin-dir` for shared process-level plugins or `pluginDirectories` for session-specific additions. Both keep explicit directories under your application's control instead of relying only on per-machine user state.

### Team marketplace sources

The server and session `plugins.marketplaces.list` APIs include marketplaces assigned to the signed-in user's teams. These sources can be browsed, refreshed, and used for plugin installation and updates through the existing plugin APIs without adding them to the user's settings.

Team marketplaces are fallback defaults, not mandatory managed settings. A same-named marketplace from user settings, repository settings, legacy registrations, Claude-compatible configuration, or runtime defaults takes precedence. A server-level `plugins.install` or `plugins.update` reads only the user's settings, as it does for other marketplaces, so an untrusted working directory cannot redirect a Team source. Managed `strictKnownMarketplaces` restrictions still apply. Team sources are fetched through the runtime's account-scoped assignment cache and are not persisted as user marketplace registrations. If an uncached lookup does not finish promptly, the operation continues without Team sources while the lookup completes in the background.

A listed marketplace that only the Team supplies has `team: true`. Because it is not registered in the user's settings, it cannot be removed: `plugins.marketplaces.remove` rejects it with an error. Hide removal for these entries the same way as for `isDefault` ones.

## Making plugin sets deterministic

When the host machine may have other plugins installed (marketplace or personal), set `COPILOT_PLUGIN_DIR_ONLY=true` in the runtime's environment to suppress ambient plugin discovery. Explicit directories supplied through either `--plugin-dir` or session `pluginDirectories` still load. This setting does not disable host-registered built-in plugins, whose activation is handled separately.

<details open>
<summary><strong>Node.js / TypeScript</strong></summary>

<!-- docs-validate: hidden -->
```typescript
import { CopilotClient, RuntimeConnection } from "@github/copilot-sdk";

async function main() {
  process.env.COPILOT_PLUGIN_DIR_ONLY = "true";
  const client = new CopilotClient({
    connection: RuntimeConnection.forStdio({
      args: ["--plugin-dir", "./plugins/code-reviewer"],
    }),
  });
  await client.start();
}

main();
```
<!-- /docs-validate: hidden -->

```typescript
process.env.COPILOT_PLUGIN_DIR_ONLY = "true";

const client = new CopilotClient({
  connection: RuntimeConnection.forStdio({
    args: ["--plugin-dir", "./plugins/code-reviewer"],
  }),
});
await client.start();
```

</details>

Use this in CI, in headless server deployments, and anywhere you want a reproducible plugin set that doesn't depend on the host's user configuration.

## Inspecting which plugins loaded

Once a session is created, list the active plugins to confirm a directory was picked up correctly:

<details open>
<summary><strong>Node.js / TypeScript</strong></summary>

<!-- docs-validate: hidden -->
```typescript
import { CopilotClient } from "@github/copilot-sdk";

async function main() {
  const client = new CopilotClient();
  await client.start();
  const session = await client.createSession({
    onPermissionRequest: async () => ({ kind: "approve-once" }),
  });

  const plugins = await session.rpc.plugins.list();
  for (const plugin of plugins.plugins) {
    console.log(`${plugin.name} (${plugin.enabled ? "enabled" : "disabled"})`);
  }
}

main();
```
<!-- /docs-validate: hidden -->

```typescript
const plugins = await session.rpc.plugins.list();
for (const plugin of plugins.plugins) {
  console.log(`${plugin.name} (${plugin.enabled ? "enabled" : "disabled"})`);
}
```

</details>

Plugins loaded via `--plugin-dir` appear in this list alongside marketplace installs. Each entry has these fields:

| Field | Description |
|-------|-------------|
| `name` | The plugin name. |
| `marketplace` | The marketplace the plugin came from. Empty for a plugin loaded from a plugin directory. |
| `version` | The plugin version, when the plugin declares one. |
| `enabled` | Whether the plugin is enabled. |

An entry for a plugin loaded from a plugin directory doesn't report that directory.

## Retrying plugins required by managed settings

When managed settings require plugins through `enabledPlugins`, the runtime installs or updates them before chat messages. If that fails, the session emits a `session.warning` with `warningType: "managed_plugins"` and waits an hour, or until the managed settings change, before trying again. The experimental `session.rpc.plugins.retryManaged()` tries again right away, for example from a Retry action next to that warning.

Call it with no arguments to retry every plugin the managed settings require, or pass `plugin@marketplace` specs in `plugins` to retry only those. The result has one entry per plugin with a `status`: `installed`, `updated`, `already_present`, `failed`, `deferred`, or `not_required`. `failed` and `deferred` entries include an `error`. The session also emits the usual `session.info` progress and `session.warning` failure events.

<details open>
<summary><strong>Node.js / TypeScript</strong></summary>

<!-- docs-validate: hidden -->
```typescript
import { CopilotClient } from "@github/copilot-sdk";

async function main() {
  const client = new CopilotClient();
  await client.start();
  const session = await client.createSession({
    onPermissionRequest: async () => ({ kind: "approve-once" }),
  });

  // Retry every plugin the managed settings require, for example from a Retry action.
  const result = await session.rpc.plugins.retryManaged();
  for (const entry of result.plugins) {
    console.log(`${entry.spec}: ${entry.status}${entry.error ? ` (${entry.error})` : ""}`);
  }

  // Or retry only the required plugins that are still missing.
  const { plugins } = await session.rpc.plugins.list();
  const missing = plugins
    .filter((plugin) => plugin.managedDesiredEnabled === true && plugin.installed === false)
    .map((plugin) => `${plugin.name}@${plugin.marketplace}`);
  await session.rpc.plugins.retryManaged({ plugins: missing });
}

main();
```
<!-- /docs-validate: hidden -->

```typescript
// Retry every plugin the managed settings require, for example from a Retry action.
const result = await session.rpc.plugins.retryManaged();
for (const entry of result.plugins) {
  console.log(`${entry.spec}: ${entry.status}${entry.error ? ` (${entry.error})` : ""}`);
}

// Or retry only the required plugins that are still missing.
const { plugins } = await session.rpc.plugins.list();
const missing = plugins
  .filter((plugin) => plugin.managedDesiredEnabled === true && plugin.installed === false)
  .map((plugin) => `${plugin.name}@${plugin.marketplace}`);
await session.rpc.plugins.retryManaged({ plugins: missing });
```

</details>

Runtimes that do not prepare managed plugins for the session reject the call.

## Troubleshooting

* **"no plugin.json or SKILL.md found in &lt;dir&gt;"**: the directory exists but doesn't qualify as a plugin. For Agent Plugins, add root `plugin.json` with a recognized schema and a name. For legacy plugins, use one of the manifest locations listed above or a root `SKILL.md`.
* **Plugin loaded but components not visible**: check which format the manifest selects. Agent Plugins uses `skills/*/SKILL.md`, root `mcp.json` with a matching schema, and `com.github.copilot/` for Copilot-specific components. Legacy root assets and component-path fields do not carry over when you add the spec schema. Use `session.rpc.skills.reload()` for skill changes; it is not an agent reload API.
* **Duplicate hooks firing**: explicit directories are de-duplicated against other entries for the same directory. Separate copies can still load twice. Remove the extra directory; `COPILOT_PLUGIN_DIR_ONLY=true` suppresses ambient plugins, not duplicate explicit copies.
* **`--plugin-dir` ignored when connecting to an external runtime**: the SDK only forwards extra args when it spawns the CLI itself. Pass `--plugin-dir` when starting the runtime server, or use session `pluginDirectories` with a runtime that supports it.

## Related

* [Custom agents](./custom-agents.md): write agents for `com.github.copilot/agents/` (or legacy `agents/`).
* [Skills](./skills.md): how `SKILL.md` files are loaded, and the skill-tier ordering rules.
* [Hooks](./hooks.md): hooks defined by a plugin fire alongside SDK-registered hooks.
* [MCP servers](./mcp.md): plugin-provided MCP servers integrate the same way as inline registrations.
* [Fleet mode](./fleet-mode.md): plugin-provided agents are dispatchable as sub-agents.
