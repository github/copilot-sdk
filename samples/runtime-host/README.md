# Runtime-supervised AHP host: source-build smoke

This Linux-only smoke uses the Node SDK's `startHost()` and the **standard**
`@microsoft/agent-host-protocol` TypeScript client. There is no application-side
AHP relay, custom protocol implementation, or prototype callback adapter.

The runtime launches `copilotd-lite`; lite uses the full host AHP listener and
connects as another SDK participant to that same runtime. `/proc` and `ps` verify
the actual executables and parent PID, including that idle lite has no child
runtime. Disposing the host closes its listener and connected AHP client and
reaps lite, without stopping the owner's SDK session.

Both source-build and assembled local-candidate modes run the same tests and
manual smoke; only artifact selection differs.

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
Before creating the AHP session, the client discovers the agent's advertised
GitHub protected resource and sends the standard `authenticate` command with
the GitHub credential. Listener authentication and GitHub authentication remain
separate; no protected-resource checks are bypassed.

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

## Assembled local candidate package

Set `COPILOT_RUNTIME_HOST_CANDIDATE_MANIFEST` to an absolute local manifest path
instead of setting the three development artifact overrides. Keep
`COPILOT_RUNTIME_HOST_E2E=1`, `GITHUB_ACTIONS=true`, and checkout-local `TMPDIR`
for E2Es; run the identical command above.

The package must contain local build outputs at
`prebuilds/<platform>/{copilot-runtime,runtime.node,copilotd-lite}`. Its
`package.json` must identify `@github/copilot-<platform>` or
`@github/copilot-sdk-<platform>` and carry existing `copilotRuntime` metadata:
`sourceRepository: "github/copilot-agent-runtime"` and `sourceSha` equal to the
manifest's runtime source commit.

The staging step writes this local-build attestation (replace all placeholders;
checksums are lowercase SHA-256 of the actual local build outputs):

```json
{
  "schemaVersion": 1,
  "kind": "local-runtime-host-candidate",
  "platform": "linux-x64",
  "packageRoot": "/absolute/local-candidate/node_modules/@github/copilot-linux-x64",
  "sources": {
    "runtime": {
      "repository": "github/copilot-agent-runtime",
      "checkout": "/absolute/copilot-agent-runtime",
      "commit": "<full local runtime HEAD>"
    },
    "host": {
      "repository": "github/copilot-host",
      "checkout": "/absolute/copilot-host",
      "commit": "<full local host HEAD>"
    },
    "sdk": {
      "repository": "github/copilot-sdk",
      "checkout": "/absolute/copilot-sdk",
      "commit": "<full local SDK HEAD>"
    }
  },
  "artifacts": {
    "runtime": {
      "path": "prebuilds/linux-x64/copilot-runtime",
      "sourcePath": "/absolute/copilot-agent-runtime/local-build/copilot-runtime",
      "sha256": "<SHA-256>"
    },
    "provider": {
      "path": "prebuilds/linux-x64/runtime.node",
      "sourcePath": "/absolute/copilot-agent-runtime/local-build/runtime.node",
      "sha256": "<SHA-256>"
    },
    "lite": {
      "path": "prebuilds/linux-x64/copilotd-lite",
      "sourcePath": "/absolute/copilot-host/local-build/copilotd-lite",
      "sha256": "<SHA-256>"
    }
  }
}
```

Source revisions must match the three local checkouts, including this SDK.
Original build artifacts must live inside their respective checkouts, outside
`node_modules`; candidate packages may live inside `node_modules`. Before
attesting, verify the host's Cargo metadata resolves the Rust SDK to this local
SDK checkout. Do not attest downloaded releases as local builds.

The helper checks the original and packaged artifact hashes, then invokes the
SDK's existing `materializeRuntimeBundle` into `.runtime-host-materialized`
beside the manifest and rechecks the materialized hashes. The expected lite
path comes from that validated bundle, not `COPILOTD_LITE_PATH`.
The runtime child environment explicitly omits both lite and provider development
overrides—even if they are set in the invoking shell. `/proc` assertions verify
the overrides are absent and the actual loaded provider and supervised child
are the validated package copies. This exercises bundled lookup rather than
another source-path override.
