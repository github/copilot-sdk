# GitHub Copilot SDK — Assistant Instructions

**Quick purpose:** Help contributors and AI coding agents quickly understand this mono-repo and be productive (build, test, add SDK features, add E2E tests). ✅

## Big picture 🔧

- The repo implements language SDKs (Node/TS, Python, Go, .NET) that speak to the **Copilot CLI** via **JSON‑RPC** (see `README.md` and `nodejs/src/client.ts`).
- Typical flow: your App → SDK client → JSON-RPC → Copilot CLI (server mode). The CLI must be installed or you can connect to an external CLI server via the `CLI URL option (language-specific casing)` (Node: `cliUrl`, Go: `CLIUrl`, .NET: `CliUrl`, Python: `cli_url`).

## Most important files to read first 📚

- Top-level: `README.md` (architecture + quick start)
- Language entry points: `nodejs/src/client.ts`, `python/README.md`, `go/README.md`, `dotnet/README.md`
- Test harness & E2E: `test/harness/*`, Python harness wrapper `python/e2e/testharness/proxy.py`
- Schemas & type generation: `nodejs/scripts/generate-session-types.ts`
- Session snapshots used by E2E: `test/snapshots/` (used by the replay proxy)

## Developer workflows (commands you’ll use often) ▶️

- Monorepo helpers: use `just` tasks from repo root:
  - Install deps: `just install` (runs npm ci, uv pip install -e, go mod download, dotnet restore)
  - Format all: `just format` | Lint all: `just lint` | Test all: `just test`
- Per-language:
  - Node: `cd nodejs && npm ci` → `npm test` (Vitest), `npm run generate:session-types` to regenerate session-event types
  - Python: `cd python && uv pip install -e ".[dev]"` → `uv run pytest` (E2E tests use the test harness)
  - Go: `cd go && go test ./...`
  - .NET: `cd dotnet && dotnet test test/GitHub.Copilot.SDK.Test.csproj`

## Testing & E2E tips ⚙️

- E2E runs against a local **replaying CAPI proxy** (see `test/harness/server.ts`). Most language E2E harnesses spawn that server automatically (see `python/e2e/testharness/proxy.py`).
- Tests rely on YAML snapshot exchanges under `test/snapshots/` — to add test scenarios, add or edit the appropriate YAML files and update tests.
- The harness prints `Listening: http://...` — tests parse this URL to configure CLI or proxy.

## Project-specific conventions & patterns ✅

- Tools: each SDK has helper APIs to expose functions as tools; prefer the language's `DefineTool`/`@define_tool`/`AIFunctionFactory.Create` patterns (see language READMEs).
- Infinite sessions are enabled by default and persist workspace state to `~/.copilot/session-state/{sessionId}`; compaction events are emitted (`session.compaction_start`, `session.compaction_complete`). See language READMEs for usage.
- Streaming: when `streaming`/`Streaming=true` you receive delta events (`assistant.message_delta`, `assistant.reasoning_delta`) and final events (`assistant.message`, `assistant.reasoning`) — tests expect this behavior.
- Type generation is centralized in `nodejs/scripts/generate-session-types.ts` and requires the `@github/copilot` schema to be present (often via `npm link` or installed package).

## Integration & environment notes ⚠️

- The SDK requires a Copilot CLI installation or an external server reachable via the `CLI URL option (language-specific casing)` (Node: `cliUrl`, Go: `CLIUrl`, .NET: `CliUrl`, Python: `cli_url`) or `COPILOT_CLI_PATH`.
- Some scripts (typegen, formatting) call external tools: `gofmt`, `dotnet format`, `tsx` (available via npm), `quicktype`/`quicktype-core` (used by the Node typegen script), and `prettier` (provided as an npm devDependency). Most of these are available through the repo's package scripts or devDependencies—run `just install` (and `cd nodejs && npm ci`) to install them. Ensure the required tools are available in CI / developer machines.
- Tests may assume `node >= 18`, `python >= 3.9`, platform differences handled (Windows uses `shell=True` for npx in harness).

## Where to add new code or tests 🧭

- SDK code: `nodejs/src`, `python/copilot`, `go`, `dotnet/src`
- Unit tests: `nodejs/test`, `python/*`, `go/*`, `dotnet/test`
- E2E tests: `*/e2e/` folders that use the shared replay proxy and `test/snapshots/`
- Generated types: update schema in `@github/copilot` then run `cd nodejs && npm run generate:session-types` and commit generated files in `src/generated` or language generated location.
