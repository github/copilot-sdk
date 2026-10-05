<!-- Copyright (c) Microsoft Corporation. All rights reserved. -->

# Rust SDK

This directory contains the independent `github-copilot-sdk` crate. In
standalone SDK workflows, paths and Cargo commands below are relative to this
directory, and this crate's own lockfile and toolchain apply. When nested at
`src/sdk/rust`, the runtime CLI consumes the same checked-out sources through
the Bazel integration described under Development. The runtime's N-API
architecture does not otherwise govern this crate.

## Public API and errors

- Use the existing `Error` and `ErrorKind` in [src/errors.rs](src/errors.rs).
  Extend that public error contract instead of adding parallel per-module error
  types or returning `anyhow::Error` from library APIs.
- Treat exported types, builders, and trait signatures as a consumer-facing
  contract. Adding a required field to a public struct can break callers'
  struct literals; preserve compatibility with existing construction patterns.
- Document public APIs with rustdoc. Keep cross-language parity notes in
  [README.md](README.md), rather than repeating comparisons to other SDKs on
  individual symbols.

## Extension points and session lifetime

- Use the per-request traits in [src/handler.rs](src/handler.rs), such as
  `PermissionHandler` with `SessionConfig::with_permission_handler`, and retain
  their declared `Send + Sync + 'static` contracts.
- Implement `SessionHooks` in [src/hooks.rs](src/hooks.rs) for lifecycle hooks.
  Prefer its per-hook methods; `with_hooks` enables hooks on the session.
- Implement `SystemMessageTransform` in [src/transforms.rs](src/transforms.rs)
  with `section_ids` and `transform_section`, and register it through
  `with_system_message_transform`.
- Attach a `ToolHandler` to a `Tool` with `with_handler`, then register tools
  with `with_tools`. The `derive` feature exposes `define_tool` and `schema_for`
  for typed tool parameters. Use the current examples in
  [src/tool.rs](src/tool.rs), not hand-built JSON wire payloads.
- `EventSubscription` and `LifecycleSubscription` are streams; dropping a
  subscription unsubscribes. `Session::cancellation_token` returns a child
  token, so cancelling it must not cancel the parent session. Preserve these
  ownership and cancellation semantics when changing event dispatch.
- `ApproveAllHandler` is suitable for tests that do not exercise permission or
  managed-settings behavior. It is not an override for managed approval.

## Generated types

Do not edit `src/generated/` by hand. The generator is
`../scripts/codegen/rust.ts`. From `<SDK_ROOT>`, regenerate with the SDK facade
so the runtime layout selects checked-out schemas and the standalone layout
selects its pinned release schemas:

```bash
npm run generate:rust
```

Keep handwritten consumer-facing types in `src/types.rs` when the wire types
cannot express the public API, rather than patching generated output. Changes
to release-derived CLI pins and schema inputs belong to the release process,
not incidental SDK implementation work.

## Development

In the runtime repository, prefer `npm --prefix <SDK_ROOT> run test:rust`; it
refreshes the selected schemas and Rust projection and requests a current host
CLI before running the tests. Direct Cargo commands bypass those prerequisites
and the facade's environment applies only to its child process; it does not
persist for later shell commands. The CLI consumes this crate through the
same-checkout `//src/sdk/rust:github-copilot-sdk-local-runtime` Bazel target,
with default features disabled. That `local-runtime` mode enables in-process
transport without SDK-managed acquisition because the CLI supplies the adjacent
`runtime.node`; `bundled-cli` takes precedence in all-feature builds.

Use this crate's `rust-toolchain.toml`, `Cargo.toml`, and rustfmt configuration,
not the runtime workspace's. See [README.md#development](README.md#development)
for Node and replay-harness prerequisites. Cargo restores Rust dependencies.
The following direct Cargo commands are standalone SDK repository commands and
use its published/bundled acquisition semantics:

```bash
cargo check --all-features
cargo test --features test-support
cargo clippy --all-targets --all-features -- -D warnings
cargo +nightly-2026-04-14 fmt --all -- --config-path .rustfmt.nightly.toml --check
```

For changes to the CLI's SDK consumer path, run the repository's Bazel tests
from the runtime root:

```bash
pnpm bazel test //src/native/cli-runtime:cli-native-runtime_test //src/native/cli-runtime:runtime_sibling_resolution //src/native/cli-runtime:typed_sdk_call
```

Do not restore a repository-wide `COPILOT_SKIP_CLI_DOWNLOAD`. For focused
same-checkout compilation from this directory, use:

```bash
cargo check --no-default-features --features local-runtime
```

Run it with `COPILOT_SKIP_CLI_DOWNLOAD` unset. Tests that start the runtime must
also set `COPILOT_CLI_PATH` to the prepared same-checkout wrapper.

Follow the SDK's existing crate-local unit-test and `tests/` integration-test
layout. Integration tests needing SDK test helpers use the `test-support`
feature because the library is compiled without `cfg(test)`.

Default features bundle the CLI; disabling them changes what must be provided
externally. For transport-specific E2E settings, bundled-runtime checks, and
the full platform matrix, follow the
[Rust SDK workflow](../.github/workflows/sdk-rust.yml). A local unit test
run does not replace those checks.
