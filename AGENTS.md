# GitHub Copilot SDK — Assistant Instructions

**Quick purpose:** Help contributors and AI coding agents quickly understand this mono-repo and be productive (build, test, add SDK features, add E2E tests). ✅

`<SDK_ROOT>` means this directory, whether it is checked out as a standalone repository or nested under another repository.

## Big picture 🔧

- The repo implements language SDKs (Node/TS, Python, Go, .NET, Rust, Java) that speak to the **Copilot CLI** via **JSON‑RPC** (see `<SDK_ROOT>/README.md` and `<SDK_ROOT>/nodejs/src/client.ts`).
- Typical flow: your App → SDK client → JSON-RPC → Copilot CLI (server mode). The CLI must be installed or you can connect to an external CLI server via the `CLI URL option (language-specific casing)` (Node: `cliUrl`, Go: `CLIUrl`, .NET: `CliUrl`, Python: `cli_url`, Java: `cliUrl`).

## Most important files to read first 📚

- Top-level: `<SDK_ROOT>/README.md` (architecture + quick start)
- Language entry points: `<SDK_ROOT>/nodejs/src/client.ts`, `<SDK_ROOT>/python/README.md`, `<SDK_ROOT>/go/README.md`, `<SDK_ROOT>/dotnet/README.md`
- Java: `<SDK_ROOT>/java/AGENTS.md`, `<SDK_ROOT>/java/README.md`, `<SDK_ROOT>/java/pom.xml`, `<SDK_ROOT>/java/sdk/pom.xml`, `<SDK_ROOT>/java/copilot-native/pom.xml`
- Rust: `<SDK_ROOT>/rust/README.md` and `<SDK_ROOT>/rust/AGENTS.md`
- Test harness & E2E: `<SDK_ROOT>/test/harness/*`, Python harness wrapper `<SDK_ROOT>/python/e2e/testharness/proxy.py`
- Schemas and type generation: `<SDK_ROOT>/scripts/codegen/` and `<SDK_ROOT>/java/scripts/codegen/`
- Session snapshots used by E2E: `<SDK_ROOT>/test/snapshots/` (used by the replay proxy)
- Docs style guide: `<SDK_ROOT>/docs/AGENTS.md`

## Developer workflows (commands you’ll use often) ▶️

- Start with [SDK development setup](CONTRIBUTING.md#developing-an-sdk) for
  layout-specific prerequisites and dependency preparation; select only the
  languages needed.
- Monorepo helpers: use the portable package scripts from `<SDK_ROOT>`:
    - Build all six SDKs: `npm --prefix <SDK_ROOT> run build`
    - Format all: `npm --prefix <SDK_ROOT> run format` | Lint all: `npm --prefix <SDK_ROOT> run lint` | Test all: `npm --prefix <SDK_ROOT> run test`
    - Run one language with a verb-first script such as `npm --prefix <SDK_ROOT> run build:python` or `npm --prefix <SDK_ROOT> run check:rust`.
    - In the canonical runtime-repository layout, build and test commands automatically refresh runtime schemas and the selected language projections; tests also request a fresh same-checkout CLI build, with unchanged Bazel actions remaining cached. Cross-target CI jobs use their native test commands with explicitly staged artifacts instead of this local facade. Standalone SDK commands retain pinned published-artifact behavior.
    - See [task scopes](CONTRIBUTING.md#build-test-and-check) for default-test
      feature coverage and check side effects. Use the contributor guide's
      [focused-test setup](CONTRIBUTING.md#testing-an-unreleased-runtime-api)
      before invoking native runners.
- Per-language:
    - For normal runtime-repository testing, prefer `npm --prefix <SDK_ROOT> run test:<language>` so schemas, generated clients, and the host CLI are current. Direct language-native commands below bypass those prerequisites; use them for focused tests after preparing the checkout, or in the standalone SDK repository.
    - Node: `cd <SDK_ROOT>/nodejs && npm ci` → `npm test` (Vitest)
    - Python: `cd <SDK_ROOT>/python && uv sync --locked --all-extras --dev` → `uv run pytest` (E2E tests use the test harness)
    - Go: `cd <SDK_ROOT>/go && go test ./...`
    - .NET: `cd <SDK_ROOT>/dotnet && dotnet test test/GitHub.Copilot.SDK.Test.csproj`
    - **.NET testing note:** Never add `InternalsVisibleTo` to any project file when writing tests. Tests must only access public APIs.
    - Java: `cd <SDK_ROOT>/java && ./mvnw clean verify` (full build + tests), `./mvnw -pl sdk spotless:apply` (format code). Use `.\mvnw.cmd` on Windows.
    - Java single unit test: `./mvnw -pl sdk test -Dtest=CopilotClientTest`; for integration tests use `./mvnw -pl sdk verify -Dit.test="<TestClass>#<testMethod>" -Dcopilot.cli.path="$COPILOT_CLI_PATH"`, substituting your test's class/method after preparing the runtime.
    - Java formatting and Javadoc checks: `./mvnw -pl sdk spotless:check checkstyle:check` | Build without tests: `./mvnw clean package -DskipTests`
    - **Java testing note:** Use `verify` for integration tests, without `-q` or piping through `grep`. Never add `InternalsVisibleTo` equivalent — tests must only access public APIs.
- Use configured LSPs for supported operations like finding references instead of pattern matching, renaming symbols, etc.

## Testing & E2E tips ⚙️

- E2E runs against a local **replaying CAPI proxy** (see `<SDK_ROOT>/test/harness/server.ts`). Most language E2E harnesses spawn that server automatically (see `<SDK_ROOT>/python/e2e/testharness/proxy.py`).
- Tests rely on YAML snapshot exchanges under `<SDK_ROOT>/test/snapshots/` — to add test scenarios, add or edit the appropriate YAML files and update tests.
- The harness prints `Listening: http://...` — tests parse this URL to configure CLI or proxy.
- Java E2E tests use `E2ETestContext`, which manages a `CapiProxy` backed by
  the in-tree `<SDK_ROOT>/test/harness`. Maven's `generate-test-resources`
  phase installs the harness and Node.js SDK dependencies in place.
- Java test method names are converted to lowercase snake_case for snapshot filenames (avoids case collisions on macOS/Windows).

## Project-specific conventions & patterns ✅

- Tools: each SDK has helper APIs to expose functions as tools; prefer the language's `DefineTool`/`@define_tool`/`CopilotTool.DefineTool` patterns (see language READMEs).
- Infinite sessions are enabled by default and persist workspace state to `~/.copilot/session-state/{sessionId}`; compaction events are emitted (`session.compaction_start`, `session.compaction_complete`). See language READMEs for usage.
- Streaming: when `streaming`/`Streaming=true` you receive delta events (`assistant.message_delta`, `assistant.reasoning_delta`) and final events (`assistant.message`, `assistant.reasoning`) — tests expect this behavior.
- Type generation is centralized in `<SDK_ROOT>/scripts/codegen/`. Standalone SDK generation downloads schemas from the pinned `github/copilot-cli` release; runtime-repository generation explicitly selects the checked-out runtime schemas.
- Java code style: 4-space indent (Spotless + Eclipse formatter), fluent setter pattern for config classes, Javadoc required on public APIs (enforced by Checkstyle, except `json`/`events` packages).
- Java handlers return `CompletableFuture` (the Java equivalent of C# `async/await`). When porting from .NET: convert properties → getters/fluent setters, use Jackson (`ObjectMapper`, `@JsonProperty`) for serialization.

## Integration & environment notes ⚠️

- The SDK requires a Copilot CLI installation or an external server reachable via the `CLI URL option (language-specific casing)` (Node: `cliUrl`, Go: `CLIUrl`, .NET: `CliUrl`, Python: `cli_url`, Java: `cliUrl`) or `COPILOT_CLI_PATH`.
- Generators and checks use language formatting tools in addition to npm
  dependencies. Use the [prerequisite table](CONTRIBUTING.md#choose-your-toolchains)
  and its linked manifests instead of assuming consumer minimum versions are
  sufficient. Nested runtime builds require the parent's Node/pnpm versions.
- Build tools and test runtimes differ: .NET SDK 10 does not supply the .NET 8
  test runtime, Java builds require JDK 25 while compatibility tests also use
  JDK 17, and Rust SDK checks use their own pinned stable and nightly toolchains.
- Java build prerequisites, supported runtime versions, formatting, and Javadoc checks are documented in `<SDK_ROOT>/java/AGENTS.md`.

## Where to add new code or tests 🧭

- SDK code: `<SDK_ROOT>/nodejs/src`, `<SDK_ROOT>/python/copilot`, `<SDK_ROOT>/go`, `<SDK_ROOT>/dotnet/src`, `<SDK_ROOT>/rust/src`, `<SDK_ROOT>/java/sdk/src/main/java`
- Unit tests: `<SDK_ROOT>/nodejs/test`, `<SDK_ROOT>/python/*`, `<SDK_ROOT>/go/*`, `<SDK_ROOT>/dotnet/test`, `<SDK_ROOT>/rust/tests`, `<SDK_ROOT>/java/sdk/src/test/java`
- E2E tests: `*/e2e/` folders that use the shared replay proxy and `<SDK_ROOT>/test/snapshots/`, `<SDK_ROOT>/java/sdk/src/test/java/**/e2e/`
- Generated types: in the runtime repository, run `npm --prefix <SDK_ROOT> run generate` or `generate:<language>` to derive committed schemas and clients from runtime HEAD. In the standalone SDK repository, the same commands use the pinned Copilot CLI release schemas. Update the pin only when intentionally advancing standalone generation inputs.
- For schema-only generation, conditional freshness checks, and protocol
  generation, follow the contributor guide's
  [revision-specific workflow](CONTRIBUTING.md#testing-an-unreleased-runtime-api).
  Verify command availability in the runtime root manifest first; do not import
  another branch's command assumptions or change development placeholders to
  bypass same-checkout artifact preparation.

### Generation freshness

In the runtime repository, start contract changes with `pnpm run generate:schemas`
from the runtime root. If the public schemas changed relative to your task's base
revision, run `pnpm run generate:sdk` and commit the schemas and all changed SDK
projections together. Already-committed schema changes still require this step.
Generator, generator-dependency, and formatter changes also require regeneration,
as do handwritten Go/.NET declarations scanned for generated-name collisions.
Unchanged public schemas and generation inputs do not require SDK projections
to be regenerated for an internal-only runtime change.

For `sdk-protocol-version.json` or its generator, run
`npm --prefix <SDK_ROOT>/nodejs run update:protocol-version` and commit all six
language constants, including Java. Aggregate `generate` refreshes these too.
Single-language generation and CLI/SDK builds are not an all-six freshness check.

In both repository layouts, the `Check schema and SDK freshness` job in `sdk.yml` owns
generation and freshness checks for all six languages. Language-specific
workflows own their builds, tests, and documentation, not duplicate codegen jobs.

## Boundaries — files you must NOT hand-edit ⛔

- **Generated code (all six SDKs)** — update its inputs or generators and use the
  generation commands above. This covers schema-derived files, including Go's
  `go/z*.go` and `go/rpc/z*.go`, and protocol-version constants. Python's
  `python/copilot/generated/__init__.py` remains handwritten.
- `<SDK_ROOT>/test/snapshots/` — authoritative test fixtures; add/edit YAML here to change E2E behavior, but don't delete without understanding downstream impact.
