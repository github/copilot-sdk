# Runtime-supervised AHP host

This workstream exposes copilotd's existing, complete AHP server through a
runtime-supervised, in-process hosting task. All six SDKs control its lifetime
through generated SDK JSON-RPC operations. It does not implement an AHP server,
launch a companion process or second runtime, or relay host traffic through the application.

## Source layout

The implementation spans `github/copilot-agent-runtime` and `github/copilot-host`.
Build from the runtime checkout; Cargo obtains the pinned `copilotd-hosting`
library from the host repository.
The SDK lives in the runtime repository at `src/sdk`; its source revision is
the runtime commit, not a separate `github/copilot-sdk` commit. Run SDK commands
from `src/sdk` unless a command names another working directory.

Both runtime and SDK pin SDK protocol version 3. The standard AHP client is
pinned to `@microsoft/agent-host-protocol@0.9.0`, matching the host's
`rust/v0.9.0` source pin (`60706330f2f351b09f150d9a9c3c0eaedfc8e8b9`).
The host prototype `d62d982`
and SDK sample prototype `8fd2498` were inspected as references, not cherry-picked
as production implementations. Their application-side TCP relay is not the
target architecture; application callbacks use ordinary SDK RPC.

## Ownership and transport

The application connects to a runtime in the usual way. `client.startAhpHost(options)`
asks that runtime to start the host library in a task. An in-memory duplex stream
carries ordinary SDK JSON-RPC on a separate connection to that same runtime.
This is not an AHP transport stream.

The hosting task owns the selected local WebSocket listener and/or GitHub Mission
Control registration and WPS connections. The requesting SDK
connection owns the task. Disposing the handle, losing that connection, or
shutting down the runtime ends the host's participation. Cleanup must not
independently delete sessions or terminate another owner's work. A new SDK
connection has no implicit claim on an old host.

The optional Node `onExit` / Rust `on_exit` callback reports exit at most once.
On owner connection loss, already-received exits take precedence; remaining
callbacks receive `ownerDisconnected` (`OwnerDisconnected` in Rust), without an
exit code and with an explanation that cleanup cannot be acknowledged. That
report cannot prove cleanup through a transport that has already closed.
`host.pid` is absent for in-process listeners (`undefined` in Node, `None` in Rust).
The optional field preserves separate host process IDs from legacy runtimes,
never the hosting runtime PID.
`reason: "exited"` reports hosting-task failure, not runtime process death.
`exitCode` is absent (Rust `exit_code: None`). Use `dispose()` to stop a listener.
Hosting no longer provides process isolation: a runtime crash also ends its host.
End-to-end coverage independently observes listener closure and runtime survival,
except when the owning runtime itself shuts down.
The small `AhpHost` handle forwards each `dispose()` call to the runtime, including
concurrent and repeated calls; the runtime owns idempotence and teardown outcomes.
A successful disposal means the listener is closed, session participation is
detached, and the hosting task has stopped, not merely that shutdown was requested.

```ts
await client.start();
const host = await client.startAhpHost({
    localServer: {},
    onExit: (exit) => console.log(`AHP host stopped: ${exit.reason}`),
});
// Connect an AHP client using host.url and, when defined, host.token.
// Hosting lasts for this SDK client's connection; dispose early only if needed.
await client.stop();
```

`AhpHostOptions` requires at least one explicit transport: `localServer`,
`githubEnvironment`, or both. There is no implicit local listener.
`localServer` accepts `hostname`, `port`, `token`, and
`requireConnectionToken`. The hostname defaults to `127.0.0.1`; explicit
non-loopback addresses are allowed. An omitted or zero port selects an available
port; other values must be integers from 1 through 65535. The returned URL contains
the actual bound address, including IPv6 brackets where needed. The host always
uses the runtime's configured working directory, with no per-host override.

Connection-token authentication defaults on: supply a nonempty token or let the
listener generate one. `requireConnectionToken: false` disables only that
connection gate and returns `token: undefined`; supplying any token alongside
`false` is invalid. An empty token is always invalid. AHP resource authentication
and authorization remain in effect independently. Explicit public binding or
disabling the connection gate is the application's choice; the default remains
loopback with a generated token. Tokens travel over framed SDK RPC, not argv.

Node.js, Rust, Python, Go, .NET, and Java expose experimental thin handles over the generated host RPCs.
`onExit` is a local callback, not part of the serialized start request. There is no
`closed` promise and no public generic notification-registration API.

### GitHub Mission Control hosting

```ts
const host = await client.startAhpHost({
    githubEnvironment: {
        name: "My application",
        computeId: "stable-application-installation-id",
    },
    // Include localServer: {} to also enable a local listener.
});
console.log(host.environmentId);
```

Both `name` and `computeId` are required. Keep the application-supplied compute ID
stable across restarts to re-adopt the environment. The runtime uses its existing
authenticated GitHub identity; a local listener token is not an MC credential.
Startup fails if a requested transport cannot become ready, rather than silently
downgrading to local-only hosting. Select transports at startup; dispose and
recreate the host to change them.

`githubEnvironment.requireConnectionBinding` defaults to `true`: sealed
authentication must bind the current handshake challenge, a fresh nonce and
timestamp. Explicit `false` selects compatibility with clients sending unbound
encrypted tokens. Relay encryption and resource authorization stay mandatory,
but an unbound sealed token is replayable. This is the same transitional policy
supported by `copilotd`, not a switch to plaintext authentication.

GitHub-only hosting does not open a local listener. Its host handle has an
environment ID but no local URL or connection token. `environmentId` is absent
for local-only hosting. Disposal ends transport and registration activity without
deleting the saved MC environment record or application-owned sessions.

Registering an environment does not publish every application session. Use
`publishSession` for an existing resident session, and `listSessions` to read
the host's complete advertised catalog of live and dormant sessions. Listing
does not start a host, publish a session, or paginate results. Factory callbacks
and durable catalog behavior are unchanged. Environment management is independent of a
running host and available only through generated `rpc.environments.list`,
`rpc.environments.get`, and `rpc.environments.delete` operations, using each
language's naming conventions.

```ts
const { environments } = await client.rpc.environments.list({
    kind: "user-local",
    status: "online",
});
const selected = environments[0];
if (selected) {
    const { environment } = await client.rpc.environments.get({
        environmentId: selected.id,
    });
    // Delete only when the application intends to remove this saved environment.
    await client.rpc.environments.delete({ environmentId: environment.id });
}
```

These management operations are experimental and do not require a running host.
Discovery returns safe metadata, not host-side relay credentials. GitHub-managed
environments cannot be deleted through this API.

## Application-owned sessions

Application session factories optionally handle fresh AHP
session creation in your application. The callback receives host-selected
session configuration. Preserve its identity, workspace, and settings, add
application tools, hooks, or prompts, and return a normal session created on the
owning SDK client. Rust supplies a request-scoped `Client` for that purpose.
The host then attaches to the same runtime session through its existing separate
connection. Tool and hook functions stay in your application.

<details open>
<summary><strong>TypeScript</strong></summary>

```ts
import { approveAll, type CopilotClient } from "@github/copilot-sdk";

async function startApplicationHost(client: CopilotClient) {
    return client.startAhpHost({
        localServer: {},
        createSession: ({ config, signal }) => {
            signal.throwIfAborted();
            return client.createSession({
                ...config,
                onPermissionRequest: approveAll,
            });
        },
        onSessionReleased: async (originalSession) => {
            await originalSession.disconnect();
        },
    });
}
```

</details>
<details>
<summary><strong>Rust</strong></summary>

```rust
use std::sync::Arc;
use github_copilot_sdk::{AhpHost, AhpHostOptions, AhpSessionRequest, Client, Error};
use github_copilot_sdk::rpc::HostLocalServerOptions;

async fn start_application_host(owner: &Client) -> Result<AhpHost, Error> {
    owner
        .start_ahp_host(
            AhpHostOptions::new()
                .with_local_server(HostLocalServerOptions::default())
                .with_create_session(
                    |request: AhpSessionRequest, client: Client| async move {
                        Ok(Arc::new(client.create_session(request.config).await?))
                    },
                )
                .with_on_session_released(|original| {
                    tokio::spawn(async move {
                        if let Err(error) = original.disconnect().await {
                            eprintln!("session cleanup failed: {error}");
                        }
                    });
                }),
        )
        .await
}
```

</details>

<details>
<summary><strong>Python</strong></summary>

```python
from copilot import (
    AhpHostOptions, AhpSessionCreateRequest, CopilotClient, CopilotSession,
    PermissionHandler,
)
from copilot.rpc import HostLocalServerOptions

async def start_application_host(client: CopilotClient):
    async def create(request: AhpSessionCreateRequest):
        return await client.create_session(
            **{**request.config, "on_permission_request": PermissionHandler.approve_all}
        )

    async def release(session: CopilotSession):
        await session.disconnect()

    return await client.start_ahp_host(
        AhpHostOptions(
            local_server=HostLocalServerOptions(),
            create_session=create,
            on_session_released=release,
        )
    )
```

</details>
<details>
<summary><strong>Go</strong></summary>

```go
package main

import (
    "context"
    copilot "github.com/github/copilot-sdk/go"
    "github.com/github/copilot-sdk/go/rpc"
)

func startApplicationHost(ctx context.Context, client *copilot.Client) (*copilot.AhpHost, error) {
    return client.StartAhpHost(ctx, &copilot.AhpHostOptions{
        LocalServer: &rpc.HostLocalServerOptions{},
        CreateSession: func(ctx context.Context, request copilot.AhpSessionCreateRequest) (*copilot.Session, error) {
            request.Config.OnPermissionRequest = copilot.PermissionHandler.ApproveAll
            return client.CreateSession(ctx, request.Config)
        },
        OnSessionReleased: func(session *copilot.Session) error {
            return session.Disconnect()
        },
    })
}

func main() {}
```

</details>
<details>
<summary><strong>.NET</strong></summary>

```csharp
#pragma warning disable GHCP001
using System.Threading.Tasks;
using GitHub.Copilot;

static class HostingExample
{
    public static Task<AhpHost> StartApplicationHostAsync(CopilotClient client) =>
        client.StartAhpHostAsync(new AhpHostOptions
        {
            LocalServer = new(),
            CreateSession = request =>
            {
                request.CancellationToken.ThrowIfCancellationRequested();
                request.Config.OnPermissionRequest = PermissionHandler.ApproveAll;
                return client.CreateSessionAsync(request.Config, request.CancellationToken);
            },
            OnSessionReleased = session => session.DisposeAsync().AsTask()
        });
}
```

</details>
<details>
<summary><strong>Java</strong></summary>

```java
import com.github.copilot.AhpHost;
import com.github.copilot.AhpHostOptions;
import com.github.copilot.AllowCopilotExperimental;
import com.github.copilot.CopilotClient;
import com.github.copilot.generated.rpc.HostLocalServerOptions;
import com.github.copilot.rpc.PermissionHandler;
import java.util.concurrent.CompletableFuture;

@AllowCopilotExperimental
class HostingExample {
    static CompletableFuture<AhpHost> startApplicationHost(CopilotClient client) {
        return client.startAhpHost(new AhpHostOptions()
                .setLocalServer(new HostLocalServerOptions(null, null, null, null))
                .setCreateSession(request -> client.createSession(request.config()
                        .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)))
                .setOnSessionReleased(session -> {
                    session.close();
                    return CompletableFuture.completedFuture(null);
                }));
    }
}
```

</details>

The examples choose to disconnect the application session on release. Omit the
release callback to retain it instead. No SDK automatically disconnects or
destroys the returned session.

The SDK retains the original object (the same `Arc<Session>` allocation in Rust)
and invokes Node `onSessionReleased` / Rust `with_on_session_released` at most
once per handoff after participation ends or the handoff fails. This includes a
callback that returns its session after cancellation. Node exposes an
`AbortSignal`; Rust exposes `AhpSessionRequest::cancellation_token`. Cancellation
signals that participation ended without transferring session cleanup ownership
to the SDK.

Python exposes an `asyncio.Event` as `cancellation_event`; Go supplies a cancellable
factory `context.Context`; .NET supplies a `CancellationToken`; Java supplies a
read-only `CompletionStage<Void>` that completes normally on cancellation.
Cancellation ends the handoff RPC even if the factory has not finished. A late
session result still receives its release callback. Lifecycle callback failures
are logged rather than treated as successful cleanup.

Rust's release closure is synchronous. Its example spawns asynchronous cleanup;
that task belongs to the application and host disposal does not await it. Retain
and join cleanup tasks when application shutdown must wait for them.
The other SDKs also dispatch release and exit callbacks independently of listener
disposal. Await application-owned cleanup separately when it must finish before
the application exits.

Omitting the creation factory keeps normal host-owned session creation. There is
no custom session-listing callback.

### Restoring application-owned sessions

Node's optional `resumeSession` callback receives an `AhpSessionResumeRequest`
with `sessionId`, host-selected `config`, and an abort `signal`. Return the
object from `client.resumeSession(sessionId, { ...config, onPermissionRequest, ... })`,
adding the application's tools, hooks, and handlers needed after a runtime restart.
These functions are application code, not serialized catalog data.
If the application retained its original session on the owning client, the
callback can return that object instead of creating another resumed wrapper.
It must still match the requested session identity and workspace.

Rust exposes `with_resume_session` and the `AhpSessionResumeFactory` trait.
Its `AhpSessionResumeRequest` carries a `ResumeSessionConfig` and a cooperative
`cancellation_token`. Use the supplied request-scoped `Client` to call
`resume_session(request.config)` with your application handlers, and return the
result in an `Arc<Session>`.
Returning a retained original `Arc<Session>` is also supported under the same
identity and ownership checks.

The other SDKs support the same create, resume, release, and publication contract:

| SDK | Start | Resume callback | Resume the requested session | Publish |
| --- | --- | --- | --- | --- |
| Python | `client.start_ahp_host(options)` | `AhpHostOptions.resume_session` | `await client.resume_session(request.session_id, **request.config)` | `await host.publish_session(session.session_id)` |
| Go | `client.StartAhpHost(ctx, options)` | `AhpHostOptions.ResumeSession` | `client.ResumeSession(ctx, request.SessionID, request.Config)` | `host.PublishSession(ctx, session.SessionID)` |
| .NET | `client.StartAhpHostAsync(options)` | `AhpHostOptions.ResumeSession` | `client.ResumeSessionAsync(request.SessionId, request.Config, request.CancellationToken)` | `host.PublishSessionAsync(session.SessionId)` |
| Java | `client.startAhpHost(options)` | `AhpHostOptions.setResumeSession(callback)` | `client.resumeSession(request.sessionId(), request.config())` | `host.publishSession(session.getSessionId())` |

Install application tools, hooks, prompts, and permission handlers in the resume
configuration before calling these methods, just as in the creation examples.
Python and .NET handles support asynchronous context management; Java handles
support try-with-resources. Go callers explicitly invoke `Dispose(ctx)`.

Durable catalog entries marked as application-owned, including published
application sessions, invoke the resume
factory. If its resume callback is missing, restoration fails rather than silently
falling back to host-owned creation. Published resident sessions attach directly and do not invoke either
factory. This callback does not provide arbitrary adoption or reconfiguration
of an already-resident session.

Resumed handoffs use the same ownership rules as fresh handoffs: the SDK retains
the exact object returned by this callback and releases it at most once through
`onSessionReleased` / `with_on_session_released`, including cancellation and late
completion. It does not disconnect or destroy that object automatically.

### Publishing an existing resident session

`await host.publishSession(session.sessionId)` (Rust:
`host.publish_session(session.id().to_string()).await`) publishes an existing local session
already attached to the host's owning SDK connection. It returns `sessionId`
and `sessionUri`. Publication does not call the factory, copy history, replace
the native session, or transfer ownership. Its metadata and workspace come
from the resident session, not from an application-supplied configuration.

Publication records the session in the durable, compute-scoped host catalog.
Stopping the listener detaches its participation without deleting the catalog
entry, original session, or transcript. A later host using the same compute
identity can discover it as a dormant session. Restoring an application-owned
session requires the owning application's `resumeSession` callback to configure
its tools, hooks, and handlers; the host does not silently substitute host-owned
session construction. A currently attached resident session still attaches
directly without invoking a factory. The SDK does not expose an unpublish operation.

### Listing host sessions

Call the owner-bound host handle to read every live or dormant session currently
advertised by its host. The runtime returns the complete catalog in one response;
the method does not start a host or publish sessions. The host ID is supplied
by the handle, so the call stays on the original owning connection.
Each method returns a `HostListSessionsResult` with a `sessions` collection,
not the collection directly. In Node.js, use
`const { sessions } = await host.listSessions()`.

| SDK | Method |
| --- | --- |
| Node.js | `host.listSessions()` |
| Python | `await host.list_sessions()` |
| Go | `host.ListSessions(ctx)` |
| .NET | `host.ListSessionsAsync(cancellationToken)` |
| Java | `host.listSessions()` |
| Rust | `host.list_sessions().await` |

### CLI hosting commands

With `AHP_CLIENT` enabled, `/ahp start` starts a runtime-supervised
in-process listener alongside the current CLI session. `/remote share` starts or reuses
that same listener and publishes the foreground local session, preserving its
ID. Neither command starts another runtime. Ordinary new AHP sessions remain
available; the application session factory is not enabled.

Both commands show the loopback endpoint, in-process host ID, and generated connection token.
Sharing also shows the session URI. Use an AHP 0.9 client with the connection
token and ordinary GitHub resource authentication; the connection token alone
does not bypass resource authorization.

`/ahp status` shows connection information. `/ahp stop` stops the entire
listener and its active participation, not its durable catalog. Other clients
must reconnect after hosting restarts; local sessions and their identities
remain unchanged. Exiting the owning CLI stops the listener as well. Hosting
lifetime is separate from durable publication.

`--ahp-host [--listen host:port] [--workspace directory]` serves this same backend
in the CLI process, using normal SDK session construction, managed policy,
hooks, authentication and telemetry. The old promotion backend and duplicate
commands have been removed. Local-owner `/remote on|off|show` retains its independent
Mission Control path; the old attached-AHP-client export toggle is intentionally unavailable.
`COPILOT_AHP_SHARE_BIND` can select a wider listener for sharing;
the CLI's managed remote-control policy still gates off-machine publication.
The CLI `--ahp` attachment client negotiates AHP 0.9 for these listeners, with
0.7 retained for older external hosts. Bare `--ahp` can start an in-process
listener when no local host exists; that listener ends with its owning CLI.
Outbound relay, host-picker and explicitly configured external-daemon controls remain.

## Durable catalog and single host owner

The runtime passes its actual resolved data directory and compute identity to
the host library. Durable catalogs are scoped to both the effective Copilot
home and compute identity. The effective home follows the same
default `~/.copilot`, `COPILOT_HOME`, and SDK `baseDirectory` resolution as that
runtime. Each catalog contains sessions previously created through AHP and
explicitly published resident sessions, not all SDK/CLI sessions.
Listener disposal, owner disconnect, and restart retain it.
A replacement listener can list and resume these sessions after authenticating.

Supply top-level `AhpHostOptions.computeId` to keep the same catalog when switching
between local and Mission Control hosting. If omitted for a local-only start,
the runtime persists a stable identity in its settings; the SDK never generates
one. Mission Control's `githubEnvironment.computeId` remains required. When both
compute IDs are supplied, they must agree or startup fails.

| SDK | Top-level compute identity |
| --- | --- |
| Node.js | `computeId` |
| Python | `compute_id` |
| Go | `ComputeID` |
| .NET | `ComputeId` |
| Java | `setComputeId(...)` |
| Rust | `with_compute_id(...)` |

Only one AHP server may own a catalog at a time. A second start for the
same home and compute identity fails, including from another runtime. Ordinary runtimes, SDK
clients, and sessions do not acquire this lock and remain usable. The lock is
kernel-managed, non-blocking, held until shutdown writes finish, and released
even after forced process termination. Different effective homes or compute
identities have separate catalogs. This does not change standalone `copilotd` defaults or concurrency
behavior, and does not add standalone/in-process shared-writer support.

## Current GHES shell limitation

In-process hosting does not yet provide traditional copilotd's propagation of an AHP session's
GHES credential to shell `gh` commands. Ordinary per-session authentication is
unchanged. General support belongs in the runtime's existing per-session shell
credential capability and is tracked in
[runtime #22077](https://github.com/github/copilot-agent-runtime/issues/22077).
The experimental credential workaround and new command-target policy have been
removed. Traditional copilotd retains its existing Enterprise-token subprocess
seeding and is unaffected.

## Local-development requirements

Use the runtime checkout, including its Rust SDK in
`copilot-agent-runtime/src/sdk/rust`.
The runtime provider links the `copilotd-hosting` library at the immutable
`copilot-host` Git revision recorded in the runtime's `Cargo.lock`. Its own SDK
dependency is runtime-free; do not patch that dependency to the runtime-bearing
SDK build or stage a separate host executable.

Build both the runtime launcher and its native provider from the runtime
checkout. Point the SDK's `RuntimeConnection.forStdio({ path })` at that local
launcher. A local JavaScript launcher loading a released native provider is
not a local runtime build.

Set `COPILOT_RUNTIME_PROVIDER_LIB` to the source-built provider for development-path
integration. The launcher and provider must both be built from the current source.
The host library is linked into the provider.
Candidate package coverage exercises the bundled launcher and adjacent provider;
it does not require a companion executable.

Only model inference is eligible for record/replay. AHP connections, session
creation, streaming events, participant ownership, and listener cleanup must run
against the real local runtime and host.

After building the local runtime provider, run the focused CLI suite
from the runtime repository root (with root dependencies installed):

```sh
corepack pnpm run build
unset COPILOT_RUNTIME_E2E_OOP COPILOT_RUNTIME_OOP
COPILOT_CLI_PATH="$PWD/dist-cli/index.js" \
COPILOT_RUNTIME_PROVIDER_LIB="/absolute/local/runtime/runtime.node" \
COPILOT_RUNTIME_HOST_E2E=1 STRICT_CAPTURES=true \
corepack pnpm run test:cli-e2e test/cli/e2e/supervised-ahp.test.ts
```

This exercises the default embedded CLI runtime. Stage the source-built provider as
`dist-cli/prebuilds/linux-x64/runtime.node` first. The fixture verifies that
this addon matches the source provider, that no host PID is reported, and that the
actual spawned Node process owns the listener socket.
It also rejects companion hosts and additional provider-loaded runtimes.

The existing `COPILOT_RUNTIME_E2E_OOP=1` harness option expects a historical
Rust-parent launcher accepting `node <entrypoint>` arguments. The current
source-built `copilot-runtime` is an SDK-only stdio/TCP server, not that launcher,
and rejects those arguments. Do not enable this option with the current
prototype artifacts or substitute a released launcher to make it pass.

This exercises existing-session sharing with local tool approval and exactly-once
execution, ordinary AHP session creation, unshare, CLI exit, and listener restart
in the same runtime. The Node and Rust SDK suites additionally cover occupied-port
startup failure and recovery. These are not process-crash isolation tests; no
task-failure injection is available. Without the opt-in environment variable, these cross-repository tests
are skipped.

### Cross-language SDK end-to-end tests

The Python, Go, .NET, and Java suites start a listener through their own SDK APIs
and launch the standard TypeScript AHP 0.9 client as a **test-only subprocess**.
`nodejs/test/e2e/harness/ahpTestDriver.ts` provides its JSON-lines test interface.
This process is an AHP client, not a host or additional runtime. It connects
directly to the real listener. The existing CAPI replay proxy handles only
inference traffic and has no AHP orchestration routes or state.

Install the existing Node SDK and replay-harness test dependencies before these
suites (`npm --prefix nodejs ci --ignore-scripts` and
`npm --prefix test/harness ci --ignore-scripts`, from `src/sdk`).
Use a source-built runtime or integrated candidate, not the released runtime pin.
As with the Node and Rust suites, set `COPILOT_RUNTIME_HOST_E2E=1` to enable them.
Set `COPILOT_CLI_PATH` to that runtime's launcher and `GITHUB_ACTIONS=true` to
keep the shared snapshots read-only. Then run the relevant focused command:

```sh
# From src/sdk/python
uv run pytest test_host.py e2e/test_runtime_host_e2e.py

# From src/sdk/go
go test -race ./... -run 'Ahp|RuntimeHost'

# From src/sdk/dotnet
dotnet test test/GitHub.Copilot.SDK.Test.csproj -p:CopilotSkipCliDownload=true \
  --filter 'FullyQualifiedName~ClientSessionLifetimeTests.Ahp_|FullyQualifiedName~RuntimeHostE2ETests'

# From src/sdk/java, using JDK 25+
./mvnw -pl sdk verify -Dtest=AhpHostTest -Dit.test=RuntimeHostIT \
  -Dcopilot.cli.path="$COPILOT_CLI_PATH"
```

Each language reuses the unchanged
`multi_client/both_clients_see_tool_request_and_completion_events.yaml` and
`runtime_host/app_resume_callback_composes_tools_after_history.yaml` snapshots.
The scenarios cover fresh application factories, exact resident publication,
durable application resume after a complete runtime restart, restored history,
composed application/AHP-client tools, preserved prompts and hooks, original-object
release, listener closure, and continued application-session use after disposal.

## Companion release order

To bootstrap the accepted cross-repository dependency, the host pins the
runtime-free Rust SDK implementation to an immutable companion commit recorded
in its Cargo manifest and lockfile. This is a source dependency, not
a claim that a corresponding SDK or runtime release has been published.

The host library must be built into the runtime provider before a runtime
release can publish the integrated artifacts and checksums. The SDK can then pin
that runtime release through its existing runtime-distribution mechanism.
Unreleased local candidates must be staged explicitly; substituting a released
runtime or host does not validate these changes.

The SDK's existing released runtime pin must be advanced only after the
companion runtime is published. Until then, `startAhpHost()` requires the local
source-built runtime or an assembled candidate; the currently released runtime
is not claimed to implement the new host operations. Generated bindings in this
branch come from the companion runtime's local schema, so release-based code
generation must use that same companion release when its pin is advanced.

The canonical `.github/workflows/sdk-rust.yml` runs SDK feature checks in active
monorepo CI and is mirrored to `src/sdk/.github/workflows/sdk-rust.yml` for export.
Its runtime-free feature checks do not activate the opt-in AHP E2Es.

The coordinated follow-up is: publish the integrated runtime artifacts, provision private
release-read credentials, update the runtime acquisition pin,
advance the SDK runtime pin, **then enable the opt-in AHP E2Es in CI**.
CI activation is not a prerequisite for these implementation drafts; local
source and assembled-candidate runs provide current integration evidence.
Actual platform ABI/signing/notarization release jobs still need to execute.
Unsigned debug candidates do not establish signed-release behavior.

Arbitrary existing-session adoption, application-owned AHP transport,
projection relocation, general multi-harness composition, and exhaustive AHP
compatibility coverage are separate workstreams.
