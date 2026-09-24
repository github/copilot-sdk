# Contributing

Thanks for your interest in contributing!

This repository contains the Copilot SDK, a set of multi-language SDKs (Node/TypeScript, Python, Go, .NET, Java, and Rust) for building applications with the GitHub Copilot agent, maintained by the GitHub Copilot team.

Contributions to this project are [released](https://help.github.com/articles/github-terms-of-service/#6-contributions-under-repository-license) to the public under the [project's open source license](LICENSE).

Please note that this project is released with a [Contributor Code of Conduct](CODE_OF_CONDUCT.md). By participating in this project you agree to abide by its terms.

## Before You Submit a PR

**Please discuss any feature work with us before writing code.**

The team already has a committed product roadmap, and features must be maintained in sync across all supported languages. Pull requests that introduce features not previously aligned with the team are unlikely to be accepted, regardless of their quality or scope.

If you submit a PR, **be sure to link to an associated issue describing the bug or agreed feature**. No PRs without context :)

## What We're Looking For

We welcome:

- Bug fixes with clear reproduction steps
- Improvements to documentation
- Making the SDKs more idiomatic and nice to use for each supported language
- Bug reports and feature suggestions on [our issue tracker](https://github.com/github/copilot-sdk/issues) — especially for bugs with repro steps

We are generally **not** looking for:

- New features, capabilities, or UX changes that haven't been discussed and agreed with the team
- Refactors or architectural changes
- Integrations with external tools or services
- Additional documentation
- **SDKs for other languages** — if you want to create a Copilot SDK for another language, we'd love to hear from you and may offer to link to your SDK from our repo. However we do not plan to add further language-specific SDKs to this repo in the short term, since we need to retain our maintenance capacity for moving forwards quickly with the existing language set. For other languages, please consider running your own external project.

## Microsoft Contributor Setup

Microsoft contributors who need recent builds of `@github`-scoped packages from the internal Azure Artifacts feed can run this command from PowerShell at the SDK root (`src/sdk` when nested, or the standalone repository root):

```powershell
node .\scripts\npm-auth-refresh.mjs --run
```

Alternatively, on any platform, run `npm run auth:refresh` from the `nodejs` directory.

The command creates or updates scoped registry configurations at `nodejs/.npmrc`, `test/harness/.npmrc`, and `java/scripts/codegen/.npmrc`, preserving unrelated settings. Each configuration routes only the `@github` scope through the `copilot-canary` feed's `@Local` view, so you can then use the normal dependency installation commands. Credentials remain in your user-level npm configuration rather than in project files. On Windows, the command uses `vsts-npm-auth`; on Linux and macOS, it uses the Microsoft Azure Artifacts npm credential provider. Both paths force a credential refresh.

Run `node .\scripts\npm-auth-refresh.mjs --run` from PowerShell at the SDK root again after an Azure Artifacts 401 or 403 response, or rerun `npm run auth:refresh` from `nodejs`. To return to your previous registry behavior, remove the `@github:registry` entry from each of the three `.npmrc` files, or restore its previous value if you had a custom entry. Delete a file only if it contains no other settings. Public contributors do not need this setup and are unaffected.

## Developing an SDK

The **SDK root** is `src/sdk` in `github/copilot-agent-runtime`, or the repository
root in the standalone `github/copilot-sdk` checkout. Unless stated otherwise,
run the commands in this section from the SDK root. The same package scripts
work in both layouts; `just` is not required.

In the runtime repository, first run `pnpm install` from the **runtime root**.
It installs runtime/CLI dependencies, not SDK dependencies or language toolchains.
Use the parent repository's Node.js and pnpm requirements for this layout.
You can stay with `build:cli` and `test:cli` at the runtime root without setting
up the optional SDK languages.

### Choose your toolchains

Install only the tools for the SDK and tasks you need. For all-six
build/test/check coverage on your host, install every applicable row. Follow
the linked project configuration when a pin changes; consumer minimum versions
are not necessarily sufficient to build the repository.

| SDK or task | Developer prerequisites and source |
| --- | --- |
| Node.js and shared tooling | [Node.js](https://nodejs.org/en/download) satisfying [the SDK engines](nodejs/package.json): `^20.19.0 \|\| >=22.12.0`; SDK CI uses Node 22. Nested runtime builds require **Node >=24** and the parent repository's pinned pnpm. Use npm for SDK-local projects and their lockfiles. |
| Python | [Python >=3.11](python/pyproject.toml) and [uv](https://docs.astral.sh/uv/getting-started/installation/). `uv sync` installs the project, development tools, and pinned Ruff; do not install those globally. CI tests on 3.11 and validates docs on 3.12. |
| Go | [Install Go](https://go.dev/doc/install) at the version selected by [go.mod](go/go.mod), currently 1.24, also required by [samples](go/samples/go.mod). Go includes `gofmt`. Checks additionally need [golangci-lint](https://golangci-lint.run/docs/welcome/install/); [CI](.github/workflows/sdk-go.yml) currently uses `latest`, not an exact repository pin. |
| .NET | [.NET SDK](https://dotnet.microsoft.com/download/dotnet) selected by [global.json](dotnet/global.json): 10.0.100 with major roll-forward. Also install the **[.NET 8 runtime](https://dotnet.microsoft.com/download/dotnet/8.0)** for the `net8.0` [tests](dotnet/test/GitHub.Copilot.SDK.Test.csproj). SDK 10 alone does not supply that runtime. Windows additionally runs `net472` tests and needs a compatible .NET Framework runtime. |
| Java | [Install a JDK](https://learn.microsoft.com/java/openjdk/download) meeting the [build requirement](java/sdk/pom.xml), currently >=25. The artifact targets Java 17; a second JDK 17 is needed only to reproduce that compatibility test. From `java/`, use `./mvnw` (`.\mvnw.cmd` on Windows): the [wrapper](java/.mvn/wrapper/maven-wrapper.properties) downloads pinned Maven. Normal builds/tests do not require a global Maven install. |
| Rust | [rustup](https://rustup.rs/) with the SDK's [pinned toolchain and components](rust/rust-toolchain.toml), currently 1.94.0. Formatting/check tasks also need rustfmt from the toolchain selected in [run-tasks.mjs](scripts/run-tasks.mjs). This is separate from the parent runtime's Rust toolchain. |

For Rust formatting, substitute the current formatter toolchain from
`scripts/run-tasks.mjs` for `<formatter-toolchain>`:

```bash
rustup toolchain install "<formatter-toolchain>" --profile minimal --component rustfmt
```

Use your host's native build prerequisites: for example, MSVC build tools for
Windows native compilation, or a C compiler, `pkg-config`, and OpenSSL
development libraries for Linux Rust SDK tests using native TLS. Windows
runtime builds also use Git for Windows Bash. Do not install cross-compilers,
Docker, or every tested JDK just to run the normal host SDK tasks. The musl,
cross-target, and transport matrices in [SDK CI](.github/workflows/sdk.yml)
are separate from local host validation.

Verify installations before running the expensive tasks: `node --version`,
`uv --version`, `go version`, `dotnet --list-sdks`, `dotnet --list-runtimes`,
`java -version`, and `rustup toolchain list`. In particular, check for
`Microsoft.NETCore.App 8.x`, not just SDK 10. Check `dotnet --version` from
`dotnet/` so `global.json` applies. Rustup shims and the Maven wrapper can
download missing tools on first use.

### Prepare project dependencies

Build tasks restore the selected SDK's dependencies: Node runs
`npm ci --ignore-scripts`, Python runs `uv sync --all-extras --dev`, and the
other native build tools restore their project dependencies. In the runtime
layout, build/test/generate tasks also install the codegen npm dependencies
and refresh public schemas and the selected projections through Bazel.
This preparation can update generated source files.

Tests and checks need additional tools that `pnpm install` does not provide.
Before Node, Python, Go, .NET, or Rust SDK tests, prepare Node tooling and the
shared replay harness if they are not already installed:

```bash
npm --prefix nodejs ci --ignore-scripts
npm --prefix test/harness ci --ignore-scripts
```

The full Node test task also runs the corrections-script tests:

```bash
npm --prefix scripts/corrections ci
```

Java's Maven test lifecycle prepares its Node and harness dependencies itself.
For Python checks/tests without first building, run
`uv sync --locked --all-extras --dev` from `python/`.

### Build, test, and check

Substitute `nodejs`, `python`, `go`, `dotnet`, `java`, or `rust` for `<language>`.

| From the SDK root | Scope |
| --- | --- |
| `npm run build:<language>` | Build one SDK; `npm run build` builds all six. |
| `npm run test:<language>` | Run that language's suite; `npm test` runs all six. |
| `npm run test:default` | Node SDK unit tests and default-feature Rust SDK tests with `test-support`. |
| `npm run check:<language>` | Language-specific checks. Java includes `verify` (tests), .NET includes a solution build, and Rust includes Clippy and nightly formatting. |
| `npm run format:<language>` | Apply that language's formatter. |
| `npm run format:check:<language>` | Check formatting without applying it. |
| `npm run generate:<language>` | Regenerate one projection; `npm run generate` regenerates all six. |

The runtime root provides `pnpm run build:sdk:<language>`,
`pnpm run test:sdk:<language>`, and `pnpm run generate:sdk:<language>` aliases.
For checks/formatting there, use `npm --prefix src/sdk run check:<language>`
or `format:<language>`: the root TypeScript linters and formatter exclude SDK
sources. Runtime-root aliases live in the parent runtime's
[`package.json`](https://github.com/github/copilot-agent-runtime/blob/main/package.json);
SDK-local commands live in [package.json](package.json) and
[run-tasks.mjs](scripts/run-tasks.mjs).

In a runtime checkout, SDK tests request a current host `build:cli` before
running; Java and aggregate SDK builds also prepare the CLI. Unchanged Bazel
actions remain cached. The Rust SDK build uses Bazel, but its tests use the
independent SDK Cargo toolchain. Standalone tasks instead use the SDK's pinned
published runtime inputs. Do not work around a missing checkout artifact by
changing release pins or switching to a published runtime.

Rust's `test:rust` and `test:default` run `cargo test --features test-support`;
they do not cover non-default `derive` or in-process tests. For those changes,
use the corresponding feature selections in the
[Rust SDK workflow](.github/workflows/sdk-rust.yml) after preparing the runtime.

The facade does **not** forward arbitrary native test selectors. For a focused
test, first prepare the checkout, then use the native runner as described below.
Language-specific details:

- [Node.js/TypeScript](nodejs/README.md#development)
- [Python](python/README.md#development)
- [Go](go/README.md#development)
- [.NET](dotnet/README.md#development)
- [Rust](rust/README.md#development)
- [Java](java/README.md#development-setup)

### Testing an unreleased runtime API

In `github/copilot-agent-runtime`, Rust contracts under
`src/native/sdk-contract` produce `generated/api.schema.json` and
`generated/session-events.schema.json`. From the **runtime root**, use:

```bash
pnpm run generate:sdk
pnpm run test:sdk:<language>
```

Commit changed public schemas and all affected language projections together;
a CLI build or selected-language build is not an all-six freshness check.
Do not hand-edit generated wrappers. Go, .NET, and Rust generators invoke
external formatters, so have those tools installed even when only generating
their sources.

**Schema-only workflow, where available:** some runtime revisions add
`generate:schemas` and `check:sdk-generation` to the root `package.json`.
Check that those scripts exist before using them; they are not standalone SDK
commands. On those revisions, run `pnpm run generate:schemas` first. If either
public schema differs from your task's base revision, run `pnpm run generate:sdk`.
Already-committed schema changes count, even if regeneration leaves a clean
working tree. Generator, dependency, formatter, and scanned Go/.NET declaration
changes also require projection generation; internal-only runtime changes with
unchanged public schemas and generation inputs do not.

In that workflow, aggregate `generate:sdk` also refreshes all six protocol
constants from `sdk-protocol-version.json`; single-language generation does not.
`pnpm run check:sdk-generation` regenerates and checks schemas first, then SDK
projections, then protocol constants. Without CI event metadata it checks every
projection. It leaves regenerated files for inspection, so it is not a read-only
check. Its Rust generator requires the pinned nightly rustfmt from
[toolchain setup](#choose-your-toolchains), including when invoked by the default
root build. The Bazel-only `build:cli` path does not acquire that requirement.

In a standalone SDK checkout, generation instead downloads schemas from the
pinned release. Prepare `scripts/codegen` dependencies first, and
`java/scripts/codegen` dependencies when generating Java:

```bash
npm --prefix scripts/codegen ci
npm --prefix java/scripts/codegen ci
npm run generate
```

A standalone checkout can also consume schemas exported by a separate runtime
checkout through the same facade:

```bash
npm run generate -- --runtime-source checkout --schema-dir /absolute/path/to/runtime/generated
```

Generate both `api.schema.json` and `session-events.schema.json` in the runtime
checkout first, using that revision's supported commands. Keep them from the
same immutable runtime revision and record the producer commit and both file
digests when handing off an unreleased API. The equivalent generator environment
is `COPILOT_RUNTIME_SOURCE=checkout` with `COPILOT_CLI_SCHEMA_DIR` pointing to
their shared directory. Missing or invalid schemas fail rather than falling
back to a published package.

This selects generation inputs only: it does not publish or install a runtime,
change the CLI release pin, or make a new RPC callable on an older runtime.
Use the matching runtime build for integration checks and retain capability
checks for unsupported runtimes. Do not replace installed package sources or
edit generated files to emulate an unreleased contract.

Do not replace runtime-checkout pins with a published version to make setup
work. If the shared CLI version is `0.0.0-dev`, it is a development placeholder:
local work still uses same-checkout artifacts. Release snapshot export, not
developer setup, is responsible for replacing placeholders with the published
CLI version. These source-export pins are separate from protocol-version
constants; neither should be changed incidentally during onboarding.

For focused native tests in either layout, resolve the prepared runtime from
the **SDK root**. In the nested layout, first run `pnpm run build:cli` from the
runtime root, or use an SDK test facade command to refresh it. The resolver
requires an existing same-checkout artifact; it does not rebuild it:

```bash
# Runtime checkout only; omit this line in the standalone SDK repository.
export COPILOT_RUNTIME_SOURCE=checkout
export COPILOT_CLI_PATH="$(npm --prefix nodejs run --silent prepare:runtime -- --print-path)"
# Needed by Node tests that specifically exercise the legacy JavaScript CLI.
export COPILOT_LEGACY_CLI_PATH="$(npm --prefix nodejs run --silent prepare:runtime -- --print-legacy-path)"
npm --prefix nodejs test -- test/e2e/structured_output.e2e.test.ts
(cd dotnet && dotnet test test/GitHub.Copilot.SDK.Test.csproj \
  --filter FullyQualifiedName~StructuredOutputE2ETests)
```

These are shell-local overrides for focused runs, not machine-wide settings.
The facade sets runtime paths only for its own child processes and clears
stale or cross-target overrides before building the host CLI. Cross-target CI
instead stages explicit artifacts and uses native test commands; do not copy
its environment wholesale into local development. Rust native tests have
additional feature/acquisition choices documented in [rust/AGENTS.md](rust/AGENTS.md).

### Documentation checks

API snippet validation is separate from SDK tests. From the SDK root:

```bash
npm --prefix scripts/docs-validation ci
# For nodejs, go, dotnet, or java:
npm run docs:<language>
# Python needs the project's interpreter and mypy:
uv run --locked --project python npm run docs:python
```

This extracts and validates snippets for Node.js, Python, Go, .NET, or Java.
Java's current docs validator calls `mvn` directly,
so this task also needs Maven 3.9+ on PATH. There is no `docs:rust` facade;
follow [the Rust SDK workflow](.github/workflows/sdk-rust.yml) for rustdoc.

### Recording and replaying SDK tests

The shared harness records real inference responses under `test/snapshots`.
Record new captures with `GITHUB_TOKEN` set and `GITHUB_ACTIONS` unset;
never author model responses by hand. Rerun with `GITHUB_ACTIONS=true` and real
provider credentials removed to require replay instead of forwarding cache
misses upstream. In the standalone layout, an unreleased-runtime change may
require a newer published pin before pinned-schema CI can pass; do not change
the pin incidentally while working on SDK source.

For recording behind `HTTPS_PROXY`, Node versions that support environment
proxies (including Node 24.20) need `NODE_USE_ENV_PROXY=1` in the test runner's
environment. If the host proxy substitutes a protected credential, set
`GITHUB_TOKEN="$GH_TOKEN"` using its issued placeholder; do not print or persist
the credential. Keep localhost and loopback in `NO_PROXY`.

Equivalent cross-language E2Es must share snapshot names and prompts, not
language-specific copies. The structured-output suite in **all six SDKs** reuses
the following captures in `test/snapshots/structured_output/`, recorded using
real CAPI `gpt-4.1` calls through the shared harness:

| Shared capture (without `.yaml`) | Flow |
| --- | --- |
| `infers_typed_result_after_custom_tool` | Inferred typed result after a tool call, streamed text, then an unformatted follow-up |
| `sends_explicit_schema_for_message_and_batch` | Explicit-schema batch RPC followed by a schema-bearing single send |
| `send_selects_correlated_response_after_idle` | Event-driven send, tool commentary, originating-message correlation, and an idle boundary held by a stop hook |
| `typed_wait_returns_stop_hook_correction` | Typed wait returns the corrected answer, not the first assistant message |
| `typed_wait_returns_stop_hook_correction_after_terminal_tool` | Output-only finalization after a terminal tool, followed by a stop-hook correction |
| `typed_result_after_terminal_tool_and_steering` | Immediate steering during a terminal tool preserves the active schema |
| `typed_wait_returns_late_steering_response` | Steering after the first final answer remains part of the original run |
| `concurrent_typed_sends_return_their_own_results` | Concurrent queued runs use different inferred types and return their own results |

Typed cases call the public idiomatic APIs: Node/Zod, C# generics, Python/Pydantic,
Go generics, Java annotated records using the existing tool schema generator,
and Rust generics with `derive`/schemars. The tool/follow-up case also checks the
actual provider request's inferred schema, so a recorded JSON response alone
cannot mask missing schema forwarding. Explicit-schema and event-stream cases
exercise the corresponding raw public APIs instead.

Every language additionally checks rejection before admission and zero provider
calls for oversized schemas and typed immediate steering. These cases have no
model responses and therefore need **no snapshot**. Do not create canned responses
or empty model captures for them. Unit tests supplement, rather than replace,
the shared runtime E2Es.

## Submitting a Pull Request

1. Fork and clone the repository
1. Follow the development instructions for the SDK(s) you're modifying
1. Create a new branch: `git checkout -b my-branch-name`
1. Make your change, add tests, and run the documented checks
1. Push to your fork and [submit a pull request][pr]
1. Pat yourself on the back and wait for your pull request to be reviewed and merged.

Here are a few things you can do that will increase the likelihood of your pull request being accepted:

- Write tests.
- Keep your change as focused as possible. If there are multiple changes you would like to make that are not dependent upon each other, consider submitting them as separate pull requests.
- Write a [good commit message](http://tbaggery.com/2008/04/19/a-note-about-git-commit-messages.html).

## Resources

- [How to Contribute to Open Source](https://opensource.guide/how-to-contribute/)
- [Using Pull Requests](https://help.github.com/articles/about-pull-requests/)
- [GitHub Help](https://help.github.com)
