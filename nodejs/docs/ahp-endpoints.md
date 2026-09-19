<!-- Copyright (c) Microsoft Corporation. All rights reserved. -->

# Native AHP endpoints (experimental)

`CopilotClient.createAhpEndpoint()` registers a native AHP 0.9 endpoint on the
client's **existing runtime JSON-RPC connection**. It does not start a second
runtime, an HTTP server, or a WebSocket listener. The application owns its physical
transport. Only the native runtime parses AHP, translates actions, and tracks AHP
request IDs and session state; the Node SDK transports opaque strings.

This requires a matching locally built runtime that implements the `ahp.*` RPC
methods. Published runtimes without those methods fail with an explicit
"does not support native AHP endpoints" error. Older AHP versions are not supported
by this API. Mock-wire tests do **not** establish real native runtime acceptance.

The [Rust SDK](../../rust/README.md#native-ahp-endpoints-experimental) exposes the
same transport-neutral feature. Its recorded E2E suite hosts a Rust WebSocket
listener and drives real agent/tool turns with the standard AHP client, using the
shared CapiProxy record-replay harness.

## API

```ts
const endpoint = await client.createAhpEndpoint({
    onCreateSession: (request, { signal }) => {
        signal.throwIfAborted();
        return client.createSession({
            ...applicationSessionConfig, // prompt, local tools, permission callbacks
            sessionId: request.requestedSessionId,
        });
    },
    onResumeSession: ({ sessionId }) => client.resumeSession(sessionId, applicationSessionConfig),
    onListSessions: () => applicationVisibleSessions,
});
const connection = await endpoint.openConnection({
    onMessage: (text) => applicationTransport.write(text),
    onClose: (error) => applicationTransport.close(error),
});
await connection.send(incomingText);
await endpoint.refreshExposure();
await endpoint.setCapabilities(applicationCapabilities);
await connection.close();
await endpoint.dispose();
```

Create/resume callbacks return a `CopilotSession` or `{ sessionId }`; list returns
an array of those identities. The runtime attaches to the returned session, not a
second newly created one. `onSessionControl(request, { signal })` optionally
returns `{ applied, reason?, result? }`. Application functions never cross the
wire. Rejection or cancellation fails the native operation; no fallback creation
occurs. Ordinary SDK methods are safe inside callbacks; recursive endpoint
operations are rejected. Respect `signal` when doing application work. Cancellation
does not roll back an ordinary session creation that has already completed.

Omitting callbacks delegates policy to native runtime defaults, rather than
installing SDK policy. Omitted `allowSessionCreation` and `capabilities` are not
sent, preserving native defaults. Explicit `allowSessionCreation: false` disables
creation at the endpoint. **`onListSessions` is endpoint-wide authorization.** The
native endpoint enforces the returned session set for subscriptions, history,
actions, disposal, and root notifications, including attempts to use an excluded
session's URI directly. Applications do not need to duplicate those access checks
in resume or control callbacks. Authenticate the physical listener in production.
`refreshExposure()` asks the runtime to refresh the authorized session set after
application policy changes.

Attaching an already-live session does not invoke `onResumeSession`. A cold
authorized session invokes that callback so the application can restore its
ordinary SDK configuration, including local tools and permission handlers.

Persistence follows ordinary SDK lifecycle rules. An unnamed session with no user activity
is not saved simply by disconnecting. `createSession({ name: "..." })` opts into persistence
before the first turn. The sample also sets the workspace name and calls `sessions.save`
before disconnecting, so its cold-resume case has a durable journal and working-directory
metadata rather than relying on an unused in-memory session.

`send()` resolves when the runtime admits the message, not when the AHP request
finishes. Output arrives via `onMessage`, with IDs and text unchanged. Output
callbacks run serially per logical connection but never block another connection
or the SDK reader. Resolve `onMessage` only after the physical transport has
delivered the message, not merely queued it in an unbounded application buffer.
The WebSocket sample awaits the `ws.send` completion callback.

Internally, `ahp.message` is a **server-to-SDK request**, returning `null` only after
`onMessage` resolves. This is a transport-delivery acknowledgment, not an AHP
response; the opaque payload's request IDs remain untouched. Waiting for the
acknowledgment preserves native output backpressure across the runtime's ordinary
SDK writer. Failed delivery, cancellation, and logical connection closure reject
pending acknowledgments, including queued messages. `ahp.connectionClosed` and
`ahp.endpointClosed` remain notifications; `ahp.send` still acknowledges admission.

Default per-direction limits (including in-flight delivery):
1 MiB UTF-8 per message, 128 outstanding messages, 8 MiB total buffered bytes.
`limits` can tighten local bounds; it does not change the runtime's own limits.
Values above these hard ceilings are rejected before endpoint registration.
Overflow, admission failure, or a rejected output callback closes the affected
logical connection and reports an error through `onClose`, without disconnecting
or cancelling its owning session.

Close/dispose are idempotent and clean up locally even if runtime cleanup fails.
Explicit cleanup calls still reject on remote errors; repeat calls return the same
result. Runtime EOF and SDK stop close endpoints and abort application callbacks.
As with ordinary SDK sessions, stopping the client itself also disconnects its
session handles.

## Run the SDK-owned WebSocket sample

Requires Node 22.12+ (the standard AHP client's WebSocket transport uses Node's
global WebSocket), a runtime checkout with this slice's native changes, and normal
Copilot authentication if using model-driven turns.

Build the matching runtime in its checkout:

```sh
cd /workspace/copilot-agent-runtime
node_modules/.bin/tsx script/build-addons-bazel.ts --profile debug
# Export the COPILOT_NAPI_ADDONS_PREBUILT, COPILOT_BUILD_RUNTIME_BIN and
# COPILOT_REPO_ROOT values printed by the staging command, then:
corepack pnpm run build
```

Then run the **SDK application**, which owns the localhost-only WebSocket listener:

```sh
cd /workspace/copilot-sdk/nodejs
npm ci
npm run build
COPILOT_RUNTIME_PATH=/workspace/copilot-agent-runtime/dist-cli/index.js \
  npx tsx examples/ahp-websocket-server.ts
```

The path must point to the matching runtime executable/JavaScript entry point, not
an installed older CLI. This is the ordinary `RuntimeConnection.forStdio({ path })`
choice, not an additional host server.

To use the staged native executable directly instead of `dist-cli/index.js`, set
both `COPILOT_RUNTIME_PATH` to `src/native/runtime/copilot-runtime.linux-x64-gnu`
and `COPILOT_RUNTIME_PROVIDER_LIB` to the absolute path of the adjacent
`runtime.linux-x64-gnu.node` (adjust platform names as appropriate). The staging
filename differs from the packaged `runtime.node` name the executable otherwise
looks for.

The server prints `COLD_SESSION_ID` and `EXCLUDED_SESSION_ID`. Copy their values
into the command in a second terminal:

```sh
cd /workspace/copilot-sdk/nodejs
COLD_SESSION_ID=<printed-authorized-id> EXCLUDED_SESSION_ID=<printed-excluded-id> \
  npx tsx examples/ahp-websocket-client.ts
```

The standard `@microsoft/agent-host-protocol` 0.9 client initializes, lists sessions,
tries subscribing directly to the excluded URI, subscribes to the authorized
persisted session, creates through the application override, and attaches the
new live session. The excluded-URI probe requires a native authorization/not-found
error; transport failures or unexpected success fail the sample.

The SDK app creates two configured sessions. It disconnects the ordinary SDK owner
of the authorized session **before** registering the endpoint, while retaining
that session ID in `onListSessions`. The first authorized subscription exercises
the cold-resume callback; its invocation is logged in the server terminal. Later
subscriptions while the session is live attach without invoking resume. The other
session is omitted from `onListSessions`; native authorization rejects it, with no
duplicate access check in the application's resume callback.

Create and cold-resume callbacks both supply the distinct application prompt and
local, harmless `demo_label` tool. Permission and ask-user callbacks prompt in the
server terminal; the sample does not use `approveAll`.

The default client run makes no model calls. To opt into two model-driven turns
(one on the resumed session, one on the newly created session):

```sh
COLD_SESSION_ID=<printed-authorized-id> EXCLUDED_SESSION_ID=<printed-excluded-id> \
  AHP_RUN_MODEL=1 npx tsx examples/ahp-websocket-client.ts
```

This optional exercise uses the standard AHP client, state mirror, chat
subscription, and typed actions. It requests `demo_label` and checks for its
completed tool event and the `SDK demo:` response prefix. Model/authentication
failures, missing evidence, and a 120-second timeout fail the exercise rather than
claiming success. It is not run in deterministic CI. Neither the listener nor the
SDK adds any AHP-to-runtime protocol mapping.

`PORT` changes the listener port; `AHP_URL` changes the client URL. Press Ctrl+C in
the server terminal to dispose the endpoint and stop the SDK. Do not expose this
unauthenticated demonstration listener beyond loopback.

## Deterministic validation

From `nodejs`:

```sh
npm test -- test/ahp.test.ts
npm run typecheck
npm run build
```

These test the actual JSON-RPC duplex reader with a mocked runtime, including
reentrant ordinary SDK calls, cancellation, local tool retention, opaque frames,
queue limits, independent delivery, cleanup, and unsupported-runtime errors.
Real runtime and model-backed integration acceptance is a separate requirement.

The runtime's native integration suite exercises real SDK transport, session
participation and pending-request settlement. Its offline CLI scenarios also
cross the physical sharing listener and the same endpoint API. Use the sample's
optional real-model mode separately to exercise application-owned tools and
permissions against the matching runtime.
