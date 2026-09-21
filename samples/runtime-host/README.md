# Runtime-supervised AHP host: source-build smoke

This Linux-only smoke uses the Node SDK's `startHost()` and the **standard**
`@microsoft/agent-host-protocol` TypeScript client. There is no application-side
AHP relay, custom protocol implementation, or prototype callback adapter.

The runtime launches `copilotd-lite`; lite uses the full host AHP listener and
connects as another SDK participant to that same runtime. `/proc` and `ps` verify
the actual executables and parent PID, including that idle lite has no child
runtime. Disposing the host closes its listener and connected AHP client and
reaps lite, without stopping the owner's SDK session.

## Prerequisites

- Build the native `copilot-runtime` launcher and its provider shared library
  from the local `copilot-agent-runtime` checkout.
- Build `copilotd-lite` from the local `copilot-host` checkout, with its Rust SDK
  dependency pointing to this local SDK checkout, not a git/release SDK.
- Install this checkout's `nodejs` and `test/harness` dependencies.
- Set the three absolute source-built artifact paths below. Missing paths fail
  immediately; this smoke never falls back to a downloaded CLI/runtime package.

The exact Node dev dependency is `@microsoft/agent-host-protocol@0.9.0`, matching
host's `rust/v0.9.0` pin (commit
`60706330f2f351b09f150d9a9c3c0eaedfc8e8b9`) and wire version `0.9.0`.
The old `ahp-adapter` prototype's `0.7.0` client is not compatible.

## Manual smoke with live inference

From the SDK repository root, with a valid GitHub Copilot credential in
`GITHUB_TOKEN` or `GH_TOKEN`:

```sh
export COPILOT_CLI_PATH=/absolute/path/to/local/runtime-executable
export COPILOT_RUNTIME_PROVIDER_LIB=/absolute/path/to/local/libcopilot_runtime.so
export COPILOTD_LITE_PATH=/absolute/path/to/local/copilotd-lite
nodejs/node_modules/.bin/tsx samples/runtime-host/smoke.ts "$PWD"
```

This sends one ordinary arithmetic turn through AHP and one through the SDK,
checks streamed AHP deltas and runtime session coexistence, then verifies
disposal. It never prints the listener's bearer token. The smoke intentionally
uses the same small standard-client helper as the automated tests.

## Focused E2Es with the existing record/replay harness

```sh
mkdir -p .runtime-host-test-work
export TMPDIR="$PWD/.runtime-host-test-work"
export COPILOT_RUNTIME_HOST_E2E=1
export GITHUB_ACTIONS=true
cd nodejs
npm test -- test/e2e/runtime_host.e2e.test.ts
```

`TMPDIR` keeps the existing harness's isolated homes, workspaces, and proxy files
inside the checkout. The opt-in flag avoids running a source-build-only suite
against released artifacts. These tests explicitly use the local runtime's TCP
transport so dropping one owner connection does not kill the shared runtime.

Three non-model lifecycle tests cover explicit/idempotent disposal, unexpected
child exit, and unexpected owner connection loss with an unrelated owner's host
and session still alive. To run only those:

```sh
npm test -- test/e2e/runtime_host.e2e.test.ts -t 'disposes|reports|cleans up'
```

The inference test uses the existing `CapiProxy`, snapshot matcher, and canonical
`session/sendandwait_blocks_until_session_idle_and_returns_final_assistant_message.yaml`
conversation: model `claude-sonnet-5`, prompt `What is 2+2?`. No recordings or
responses are fabricated. `GITHUB_ACTIONS=true` enforces replay-only matching;
an incompatible request fails rather than silently contacting live inference.
Do not overwrite that shared recording to accommodate a different request. If
the host changes its request shape, record a separate scenario using the existing
harness and a real credential, then review the resulting traffic before use.
