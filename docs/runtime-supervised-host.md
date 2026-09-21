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

## Durable catalog and single lite owner

The runtime passes its actual resolved data directory to lite; the single AHP
catalog lives at `<effective Copilot home>/ahp/sessions`. It follows the same
default `~/.copilot`, `COPILOT_HOME`, and SDK `baseDirectory` resolution as that
runtime. The catalog contains sessions previously created through AHP, not all
SDK/CLI sessions. Listener disposal, owner disconnect, and restart retain it.
A replacement listener can list and resume these sessions after authenticating.

Only one **lite AHP server** may own a catalog at a time. A second start for the
same location fails, including from another runtime. Ordinary runtimes, SDK
clients, and sessions do not acquire this lock and remain usable. The lock is
kernel-managed, non-blocking, held until shutdown writes finish, and released
even after forced process termination. Different effective homes have separate
catalogs. This does not change standalone `copilotd` defaults or concurrency
behavior, and does not add standalone/lite shared-writer support.

## Current GHES shell limitation

Lite does not yet provide traditional copilotd's propagation of an AHP session's
GHES credential to shell `gh` commands. Ordinary per-session authentication is
unchanged. General support belongs in the runtime's existing per-session shell
credential capability and is tracked in
[runtime #22077](https://github.com/github/copilot-agent-runtime/issues/22077).
The lite-specific credential workaround and new command-target policy have been
removed. Traditional copilotd retains its existing Enterprise-token subprocess
seeding and is unaffected.

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

To bootstrap the accepted cross-repository dependency, the host pins the
runtime-free Rust SDK implementation to an immutable companion commit recorded
in its Cargo manifest and lockfile. This is a source dependency, not
a claim that a corresponding SDK or runtime release has been published.

The host release must publish runtime-free lite artifacts before a runtime
release can consume their versioned assets and checksums. The SDK can then pin
that runtime release through its existing runtime-distribution mechanism.
Unreleased local candidates must be staged explicitly; substituting a released
runtime or host does not validate these changes.

The SDK's existing released runtime pin must be advanced only after the
companion runtime is published. Until then, `startHost()` requires the local
source-built runtime or an assembled candidate; the currently released runtime
is not claimed to implement the new host operations. Generated bindings in this
branch come from the companion runtime's local schema, so release-based code
generation must use that same companion release when its pin is advanced.

The coordinated follow-up is: publish host lite artifacts, provision private
release-read credentials, update and publish the runtime acquisition pin,
advance the SDK runtime pin, **then enable the opt-in AHP E2Es in CI**.
CI activation is not a prerequisite for these implementation drafts; local
source and assembled-candidate runs provide current integration evidence.
Actual platform ABI/signing/notarization release jobs still need to execute.
Unsigned debug candidates do not establish signed-release behavior.

Application create/resume callbacks, application-owned AHP transport,
projection relocation, general multi-harness composition, and exhaustive AHP
compatibility coverage are separate workstreams.
