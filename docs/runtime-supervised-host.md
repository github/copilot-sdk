# Runtime-supervised AHP host

This workstream exposes copilotd's existing, complete AHP server through a
runtime-supervised `copilotd-lite` process. The Node SDK controls its lifetime
through generated SDK JSON-RPC operations. It does not implement an AHP server,
launch a second runtime, or relay the child's traffic through the application.

## Source baselines

The companion changes start from these current-main revisions:

| Repository | Baseline |
| --- | --- |
| `github/copilot-host` | `0465d7515a3e0c47b7a8cf8b39c4b428c16f6349` |
| `github/copilot-agent-runtime` | `7837f2a9ba8f902f547701f39e22e2c9a45edcac` |
| `github/copilot-sdk` | `cb2a8cc60ee9f4a561b7f36af6eb2163c2caf78e` |

Both runtime and SDK pin SDK protocol version 3. The standard AHP client is
pinned to `@microsoft/agent-host-protocol@0.9.0`, matching the host's
`rust/v0.9.0` source pin (`60706330f2f351b09f150d9a9c3c0eaedfc8e8b9`).
The host prototype `d62d982`
and SDK sample prototype `8fd2498` were inspected as references, not cherry-picked
as production implementations. Their application-side TCP relay and
application-factory callbacks are not the target architecture.

## Ownership and transport

The application connects to a runtime in the usual way. `client.startHost()`
asks that runtime to start a child. The child's stdin and stdout carry ordinary
SDK JSON-RPC on a separate connection to that same runtime. They are not AHP
transport streams and must not carry startup banners or diagnostics.

The child owns the physical, authenticated AHP listener. The requesting SDK
connection owns the child. Disposing the handle, losing that connection, or
shutting down the runtime ends the host's participation. Cleanup must not
independently delete sessions or terminate another owner's work. A new SDK
connection has no implicit claim on an old host.

The high-level handle's `closed` promise distinguishes reported child exit from
owner connection loss. The latter cannot prove reaping through a transport that
has already closed. End-to-end coverage must independently observe listener and
process termination.

## Local-development requirements

Use three local checkouts, including the Rust SDK in `copilot-sdk/rust`.
Configure the host's Cargo dependency override explicitly and inspect
`cargo tree` or `cargo metadata` to confirm that `github-copilot-sdk` resolves
there. Do not commit an absolute machine path into Cargo configuration.

Build both the runtime launcher and its native provider from the runtime
checkout. Point the SDK's `RuntimeConnection.forStdio({ path })` at that local
launcher. A local JavaScript launcher loading a released native provider is
not a local runtime build.

Set `COPILOTD_LITE_PATH` in the runtime's environment for development-path
integration. An explicit missing path must fail rather than discovering a
different binary. Production packaging supplies the executable as part of the
runtime bundle, not as an independently acquired SDK component. Candidate
package coverage must unset the development override and exercise that bundled
lookup.

Only model inference is eligible for record/replay. AHP connections, session
creation, streaming events, participant ownership, and process cleanup must run
against the real local runtime and host.

## Companion release order

The host release must publish runtime-free lite artifacts before a runtime
release can consume their versioned assets and checksums. The SDK can then pin
that runtime release through its existing runtime-distribution mechanism.
Unreleased local candidates must be staged explicitly; substituting a released
runtime or host does not validate these changes.

Application create/resume callbacks, application-owned AHP transport,
projection relocation, general multi-harness composition, and exhaustive AHP
compatibility coverage are separate workstreams.
