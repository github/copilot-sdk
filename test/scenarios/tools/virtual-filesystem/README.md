# Config Sample: Virtual Filesystem

Demonstrates running the Copilot agent with **custom tool implementations backed by an in-memory store** instead of the real filesystem. The agent doesn't know it's virtual — it sees `create_file`, `read_file`, and `list_files` tools that work normally, but zero bytes ever touch disk.

This pattern is the foundation for:
- **WASM / browser agents** where there's no real filesystem
- **Cloud-hosted sandboxes** where file ops go to object storage
- **Multi-tenant platforms** where each user gets isolated virtual storage
- **Office add-ins** where "files" are document sections in memory

## How It Works

1. **Disable all built-in tools** with `availableTools: []`
2. **Provide custom tools** (`create_file`, `read_file`, `list_files`) whose handlers read/write a `Map` / `dict` / `HashMap` in the host process
3. **Auto-approve permissions** — no dialogs since the tools are entirely user-controlled
4. The agent uses the tools normally — it doesn't know they're virtual

## What Each Sample Does

1. Creates a session with no built-in tools + 3 custom virtual FS tools
2. Sends: _"Create a file called plan.md with a brief 3-item project plan for building a CLI tool. Then read it back and tell me what you wrote."_
3. The agent calls `create_file` → writes to in-memory map
4. The agent calls `read_file` → reads from in-memory map
5. Prints the agent's response
6. Dumps the in-memory store to prove files exist only in memory

## Configuration

| Option | Value | Effect |
|--------|-------|--------|
| `availableTools` | `[]` (empty) | Removes all built-in tools (bash, view, edit, create_file, grep, glob, etc.) |
| `tools` | `[create_file, read_file, list_files]` | Custom tools backed by in-memory storage |
| `onPermissionRequest` | Auto-approve | No permission dialogs |
| `hooks.onPreToolUse` | Auto-allow | No tool confirmation prompts |

## Key Insight

The integrator controls the tool layer. By replacing built-in tools with custom implementations, you can swap the backing store to anything — `Map`, Redis, S3, SQLite, IndexedDB — without the agent knowing or caring. The system prompt stays the same. The agent plans and operates normally.

Custom tools with the same name as a built-in automatically override the built-in — no need to explicitly exclude them. `availableTools: []` removes all built-ins while keeping your custom tools available.

## Run

```bash
./verify.sh
```

Requires the `copilot` binary (auto-detected or set `COPILOT_CLI_PATH`) and `GITHUB_TOKEN`.
