# Runtime-supervised AHP integration tests

`tests/runtime_host_e2e.rs` exercises the real Rust SDK, a separate
`copilot-runtime` process, its provider library, and its supervised `copilotd-lite`
WebSocket listener. The AHP client is the standard upstream Rust
`ahp`/`ahp-ws` client, pinned to `rust/v0.9.0`.

These tests reuse `tests/e2e/support.rs` and the existing Node CapiProxy, including
its normal inference matcher. Every inference uses model `claude-sonnet-5`,
prompt `What is 2+2?`, and the unchanged shared recording:

```
test/snapshots/session/sendandwait_blocks_until_session_idle_and_returns_final_assistant_message.yaml
```

`GITHUB_ACTIONS=true` enforces replay-only operation. There is no AHP mock,
alternative inference server, generated recording, or real GitHub credential.
The suite does not build native artifacts or download a replacement runtime.

## Prerequisites

- Linux with `/proc` available, Rust's pinned toolchain, Node, and the SDK's
  existing installed Node test dependencies (`nodejs/node_modules/.bin/tsx`).
- Final local builds of `copilot-runtime`, `runtime.node`, and `copilotd-lite`.
  Build/freeze those separately before running the tests.
- A workspace-local scratch directory selected by `TMPDIR`. The existing
  integration harness puts its isolated homes and working directories there.

## Source artifacts

From `rust/`, substitute absolute local-build paths:

```sh
mkdir -p ../.runtime-host-test-work/rust
TMPDIR="$(cd ../.runtime-host-test-work/rust && pwd)" \
GITHUB_ACTIONS=true COPILOT_RUNTIME_HOST_E2E=1 \
COPILOT_CLI_PATH=/absolute/local/runtime/copilot-runtime \
COPILOT_RUNTIME_PROVIDER_LIB=/absolute/local/runtime/runtime.node \
COPILOTD_LITE_PATH=/absolute/local/host/copilotd-lite \
cargo test --locked --no-default-features --features test-support \
  --test runtime_host_e2e -- --ignored --test-threads=1
```

## Assembled unpublished candidate

Use the **same candidate manifest already produced for the Node runtime-host
E2Es**, with source commits matching the local checkouts. There is no separate
Rust package staging or attestation format.

```sh
mkdir -p ../.runtime-host-test-work/rust
TMPDIR="$(cd ../.runtime-host-test-work/rust && pwd)" \
GITHUB_ACTIONS=true COPILOT_RUNTIME_HOST_E2E=1 \
COPILOT_RUNTIME_HOST_CANDIDATE_MANIFEST=/absolute/existing/candidate/manifest.json \
cargo test --locked --no-default-features --features test-support \
  --test runtime_host_e2e -- --ignored --test-threads=1
```

The small `tests/e2e/runtime_host_candidate.mts` bridge invokes the existing
Node `candidateHostArtifacts` helper. That helper verifies source/checksum
metadata and invokes the actual SDK materializer. Rust launches the returned
materialized runtime and removes the provider/lite development overrides;
Linux process checks prove it loaded the materialized adjacent assets.

## Coverage and focused checks

Nine focused tests cover streamed AHP inference beside an ordinary SDK session,
shared runtime session visibility, durable list/resume after disposal and
SIGKILL, explicit base-directory persistence across runtime restarts, catalog
writer exclusion, owner disconnect cleanup, cross-owner disposal rejection,
unexpected-exit callbacks, concurrent/repeated direct disposal, listener
hostname/ports/tokens and invalid combinations, protected-resource
authentication when connection-token checks are disabled, and graceful runtime
shutdown. OS checks assert the runtime is the actual host parent, the expected
provider is loaded, no second runtime is spawned, the child is reaped, existing
AHP clients disconnect, and the TCP listener closes.

The tests are ignored by ordinary Cargo runs. To compile without running native
artifacts:

```sh
cargo test --locked --no-default-features --features test-support \
  --test runtime_host_e2e --no-run
```

To run one case, add its name before `--`; retain `--ignored`. Run the complete
suite against **both final source and final candidate artifacts** before
claiming runtime-host parity.
