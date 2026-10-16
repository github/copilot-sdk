# Runtime-supervised AHP integration tests

`tests/runtime_host_e2e.rs` exercises the real Rust SDK, a separate
`copilot-runtime` process, its provider library, and its in-process AHP
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
- Final local builds of `copilot-runtime` and `runtime.node` with the host library linked in.
  Build/freeze those separately before running the tests.
- A workspace-local scratch directory selected by `TMPDIR`. The existing
  integration harness puts its isolated homes and working directories there.

## Source artifacts

Use the `copilot-agent-runtime` checkout with its locked `copilotd-hosting`
library. The SDK is the runtime repository's `src/sdk` subtree. From
`copilot-agent-runtime/src/sdk/rust/`, substitute absolute local-build paths:

```sh
mkdir -p ../.runtime-host-test-work/rust
TMPDIR="$(cd ../.runtime-host-test-work/rust && pwd)" \
GITHUB_ACTIONS=true COPILOT_RUNTIME_HOST_E2E=1 \
COPILOT_CLI_PATH=/absolute/local/runtime/copilot-runtime \
COPILOT_RUNTIME_PROVIDER_LIB=/absolute/local/runtime/runtime.node \
cargo test --locked --no-default-features --features local-runtime,test-support \
  --test runtime_host_e2e -- --ignored --test-threads=1
```

## Assembled unpublished candidate

Use the **same candidate manifest already produced for the Node runtime-host
E2Es**, with source commits matching the runtime checkout and its locked hosting library. Its version 3 source
identity records the SDK as the runtime repository's `src/sdk` subtree at the
same commit. There is no separate Rust package staging or attestation format.
Use `samples/runtime-host/stage-candidate.ts RUNTIME_CHECKOUT OUTPUT_DIRECTORY`
from the SDK root after building the runtime launcher, provider, and `dist-cli`
assets. Staging needs only runtime artifacts: `copilotd-hosting` is linked into
the provider. See the sample README for source attestation and packaging details.

```sh
mkdir -p ../.runtime-host-test-work/rust
TMPDIR="$(cd ../.runtime-host-test-work/rust && pwd)" \
GITHUB_ACTIONS=true COPILOT_RUNTIME_HOST_E2E=1 \
COPILOT_RUNTIME_HOST_CANDIDATE_MANIFEST=/absolute/existing/candidate/candidate.json \
cargo test --locked --no-default-features --features local-runtime,test-support \
  --test runtime_host_e2e -- --ignored --test-threads=1
```

The small `tests/e2e/runtime_host_candidate.mts` bridge invokes the existing
Node `candidateHostArtifacts` helper. That helper verifies source/checksum
metadata and invokes the actual SDK materializer. Rust launches the returned
materialized runtime and removes development overrides;
Linux process checks prove it loaded the materialized adjacent assets.

## Coverage and focused checks

Focused tests cover streamed AHP inference beside an ordinary SDK session,
shared runtime session visibility, durable list/resume after disposal and
repeated listener disposal, explicit base-directory persistence across runtime restarts, catalog
writer exclusion, owner disconnect cleanup, cross-owner disposal rejection,
occupied-port startup failure and recovery, concurrent/repeated direct disposal, listener
hostname/ports/tokens (including IPv6, DNS names, missing/wrong tokens, and
fractional ports through raw RPC) and invalid combinations, protected-resource
authentication when connection-token checks are disabled, and graceful runtime
shutdown. Checks assert that the host PID is absent, the expected
provider is loaded, no host child or second runtime is spawned, the runtime owns
the listener's socket inode, existing AHP clients disconnect, the TCP listener
closes, and tokens do not leak in runtime
arguments.
The runtime must survive listener disposal and external owner disconnect.
Only runtime shutdown or dropping its owning client requires process reaping.
There is no host process-isolation boundary: killing the runtime also ends its
listeners. No task-specific crash injection is currently available, so these tests
do not claim unexpected-task-failure or process-crash isolation coverage.

The public callback cases in `tests/e2e/runtime_host_callbacks.rs` additionally
exercise application-owned sessions, exact original `Arc` release, application
tool execution and hooks, preserved prompt content, genuine host-attachment
failure (a conflicting tool), late factory completion after disposal,
listener disposal (including a pending factory), external owner connection
termination, and reclamation/reaping after dropping all owning client handles. Tool turns
reuse the canonical `multi_client/both_clients_see_tool_request_and_completion_events`
snapshot. Unit/wire tests also cover factory failure/panic, cancellation,
configuration changes, wrong-owner sessions, release races, and duplicate release.
The focused `runtime_host/app_resume_callback_composes_tools_after_history` replay
also verifies application-owned creation, persistence, complete runtime shutdown
and reaping, and restoration through the resume callback in a new runtime.
Configuration and history survive. The reconnected AHP client uses the same client
ID and advertises its persisted `client_echo` tool; that tool and the restored
application `magic_number` tool each execute once, with both actual results
asserted in the next inference request. This complements the existing
listener-resume, retained-object, and resident-publication coverage rather than
replacing it.
Ownership regressions also prove that application-retained sessions remain usable
after owner drop, and a blocked release callback plus 2,048 unrelated notifications
cannot lose or delay another handoff's cancellation/release.

The tests are ignored by ordinary Cargo runs. To compile without running native
artifacts:

```sh
cargo test --locked --no-default-features --features local-runtime,test-support \
  --test runtime_host_e2e --no-run
```

To run one case, add its name before `--`; retain `--ignored`. Run the complete
suite against **both final source and final candidate artifacts** before
claiming runtime-host parity.
