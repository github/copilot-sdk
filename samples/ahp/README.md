# One live session, two clients

This MVP hosts an AHP WebSocket listener **in the application**, using Express
and `ws` (sample dependencies, not SDK dependencies). The
Node SDK creates Bert through the ordinary `createSession` API. A separate
process uses the standard `@microsoft/agent-host-protocol` **0.7.0** client to
list and subscribe to that same session and send a prompt. There is no
custom protocol client. The runtime parses and serializes AHP; the SDK transports
opaque JSON text and does not own a listener, framework, or authentication policy.

The [equivalent .NET/Kestrel sample](dotnet/README.md) uses the same runtime and
independent AHP client, including streamed output and fragmented WebSocket reads.

## Requirements

Requires Node.js 22.12+ and a local runtime build implementing `ahp.createEndpoint`,
`ahp.disposeEndpoint`, `ahp.openConnection`, `ahp.receive`, and `ahp.closeConnection`,
plus the `ahpTransport.send` request and `ahpTransport.closed` notification.
Keep `libruntime.so` next to `copilot-runtime`. The host uses
`GITHUB_TOKEN` or `GH_TOKEN` when provided, otherwise normal SDK authentication.
The existing credential-injecting proxy environment can be used when testing
locally. Never print or commit credentials.

## Build and run

From the SDK repository root:

```sh
cd nodejs
npm ci
npm run build
cd ../samples/ahp
npm ci
npm run host -- /workspace/copilot-agent-runtime/target/debug/copilot-runtime
```

Keep that terminal running. It prints a WebSocket URL and SDK session ID.
In another terminal:

```sh
cd /workspace/copilot-sdk/samples/ahp
npm run client -- 'ws://127.0.0.1:PORT/ahp?token=TOKEN' 'SESSION-ID'
```

The session ID is optional; omitted, the client selects the first listed
session. To supply another prompt, append it after the session ID:

```sh
npm run client -- 'ws://127.0.0.1:PORT/ahp?token=TOKEN' 'SESSION-ID' 'who are you'
```

The AHP process prints `AHP response: I am Bert.` and exits. The SDK process
prints `[SDK observed SESSION-ID] I am Bert.` and remains alive until Ctrl+C.
This proves the AHP turn uses the SDK-created session's system prompt and
event stream, rather than creating another session. Permission requests are
denied by the SDK's normal `onPermissionRequest` callback.
Streaming is enabled: both processes also print their delta counts. The AHP
client receives successive complete `chat/delta` messages, not fragments of a
single JSON document. Run the client again to attach a new physical connection
to the same still-live session.
Set `COPILOT_MODEL` to override the sample's `gpt-4.1` model.

The public SDK transport API is:

```js
await client.start();
const endpoint = await client.createAhpEndpoint();
// After the application authenticates and accepts a physical connection:
const connection = endpoint.acceptConnection({
    send: (jsonText, signal) => transport.write(jsonText, signal),
    close: (error) => transport.close(error),
}); // Synchronous: wire message handlers immediately, without waiting for runtime open.
await connection.receive(text); // Bounded admission, NOT completion of the AHP operation.
// Alternatively, assemble UTF-8 fragments (including splits within a code point):
await connection.receiveChunk(bytes, { endOfMessage: true });
await connection.end(); // Idempotent; immediately releases local transport ownership.
await endpoint.dispose(); // Disposes all connections, not ordinary SDK sessions.
```

Observe `connection.closed` for normal closure or errors. Consumer writes receive
an `AbortSignal`; a blocked write never prevents local cleanup or physical close.
Writes are ordered, with one in flight per connection. Complete messages and queued
bytes are limited to 8 MiB per direction, with at most 64 pending messages;
transport waits have a 10-second deadline.
Pause incoming reads while awaiting admission, as the sample does. `receive` and
`receiveChunk` are alternative message input paths; do not interleave a complete
message with unfinished fragments. Stop, force-stop, and SDK RPC disconnect retire
all endpoint connections. Runtime cleanup also follows SDK RPC disconnect.

This is a local MVP, not a remote hosting deployment guide. Do not expose the
endpoint to untrusted networks. The application filters upgrades to `/ahp` and
authenticates them with a fresh demo capability in the printed URL; treat that URL
as a secret. Production applications must implement their own authentication and
TLS policy. It demonstrates streaming text turns and fresh connections to a
live session, not full AHP features, automatic replay/reconnection, or remote
authentication.

## Regenerate only Node RPC bindings

After generating the runtime schemas, from the SDK repository root:

```sh
cd scripts/codegen
npm ci
npm run generate:ts -- --rpc-only '' /workspace/copilot-agent-runtime/generated/api.schema.json
```

The empty first positional argument uses the pinned SDK session-event schema
for shared type resolution, keeping the existing Node session-event types
unchanged. Only `nodejs/src/generated/rpc.ts` is regenerated. If your checked-in
session-event types already match the local runtime, the first argument can
instead be `/workspace/copilot-agent-runtime/generated/session-events.schema.json`.
Do not use `npm run generate`: it regenerates other languages too. Never edit
generated wrappers by hand.

## Existing Node E2E tests against a local runtime

From `nodejs`, the smallest transport smoke test is:

```sh
COPILOT_CLI_PATH=/workspace/copilot-agent-runtime/target/debug/copilot-runtime \
COPILOT_SDK_DEFAULT_CONNECTION=stdio \
npm test -- test/e2e/client.e2e.test.ts -t 'should start and connect to server using stdio'
```

Run the existing session and system-prompt suites with the replay harness:

```sh
cd /workspace/copilot-sdk/test/harness
npm ci --ignore-scripts
cd ../../nodejs
COPILOT_CLI_PATH=/workspace/copilot-agent-runtime/target/debug/copilot-runtime \
COPILOT_SDK_DEFAULT_CONNECTION=stdio \
npm test -- test/e2e/session.e2e.test.ts test/e2e/system_message_sections.e2e.test.ts
```

The harness launches its replay proxy automatically. `COPILOT_CLI_PATH` selects
the local native runtime; no downloaded CLI or in-process addon is needed.
