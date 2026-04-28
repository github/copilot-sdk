# Rust scenario coverage

Rust SDK scenario samples live alongside the TypeScript / Python / Go / C# samples under
`test/scenarios/*/<scenario>/rust/`. The monorepo's `scenario-builds.yml` workflow
auto-discovers any `*/rust/Cargo.toml` under `test/scenarios/` and verifies it builds.

## Coverage

| Category       | Scenario                | Status |
|----------------|-------------------------|--------|
| `transport/`   | `stdio`                 | ✅     |
| `transport/`   | `tcp`                   | ✅     |
| `transport/`   | `external`              | ❌ deferred (needs `from_streams`-style sample) |
| `sessions/`    | `streaming`             | ✅     |
| `sessions/`    | `session-resume`        | ✅     |
| `sessions/`    | `infinite-sessions`     | ✅     |
| `sessions/`    | `concurrent-sessions`   | ✅     |
| `sessions/`    | `multi-user-*`          | ❌ deferred (multi-client orchestration) |
| `modes/`       | `default`               | ✅     |
| `modes/`       | non-default             | ❌ deferred (plan mode, read-only) |
| `tools/`       | `no-tools`              | ✅     |
| `tools/`       | `mcp-servers`           | ✅     |
| `tools/`       | `skills`                | ✅     |
| `tools/`       | `tool-filtering`        | ✅     |
| `tools/`       | `custom-agents`         | ✅     |
| `tools/`       | `tool-overrides`        | ✅     |
| `tools/`       | `virtual-filesystem`    | ❌ deferred (needs `VirtualFilesystem` hook port) |
| `callbacks/`   | `hooks`                 | ✅     |
| `callbacks/`   | `permissions`           | ✅     |
| `callbacks/`   | `user-input`            | ✅     |
| `prompts/`     | `system-message`        | ✅     |
| `prompts/`     | `reasoning-effort`      | ✅     |
| `bundling/`    | *                       | ❌ app-level concern, not an SDK gap |
| `auth/`        | *                       | ❌ deferred (GitHub-App / token-exchange) |

## Remaining gaps

- `transport/external` — needs a sample using an externally-managed CLI process (parity with Node's `from_streams`).
- `tools/virtual-filesystem` — depends on a future `VirtualFilesystem` hook port.
- `modes/*` (non-default) — plan-mode and read-only-mode samples.
- `sessions/multi-user-*` — multi-client orchestration.
- `auth/*` — GitHub-App / token-exchange sample programs.
- `bundling/*` — process bundling is application-level, not an SDK concern.

## Running the samples locally

Each scenario's `verify.sh` runs the Rust build + run phase alongside the other
languages. With a token in place (`GITHUB_TOKEN`, or `gh auth login`):

```sh
cd test/scenarios/transport/stdio && ./verify.sh
```

To build all Rust scenario samples without running them (what CI does):

```sh
for d in $(find test/scenarios -path '*/rust/Cargo.toml'); do
  (cd "$(dirname "$d")" && cargo build --quiet) || echo "FAILED: $d"
done
```
