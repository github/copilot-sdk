# One live session, two clients

This MVP starts an AHP WebSocket endpoint **inside the Copilot runtime**. The
Node SDK creates Bert through the ordinary `createSession` API. A separate
process uses the standard `@microsoft/agent-host-protocol` **0.7.0** client to
list and subscribe to that same session and send a prompt. There is no
SDK-hosted WebSocket server or custom protocol client.

## Verified MVP

The two-process example was verified against the local debug runtime on
2026-09-11: the standard AHP 0.7.0 client listed and attached to the SDK-created
session, sent `who are you`, and received `I am Bert.` The SDK host stays alive
and logs assistant messages through its normal session observer.

## Requirements

Requires Node.js 22.12+ and a local runtime build implementing `ahp.start` and
`ahp.stop`. Keep `libruntime.so` next to `copilot-runtime`. The host uses
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
npm run client -- 'ws://127.0.0.1:PORT' 'SESSION-ID'
```

The session ID is optional; omitted, the client selects the first listed
session. To supply another prompt, append it after the session ID:

```sh
npm run client -- 'ws://127.0.0.1:PORT' 'SESSION-ID' 'who are you'
```

The AHP process prints `AHP response: I am Bert.` and exits. The SDK process
prints `[SDK observed SESSION-ID] I am Bert.` and remains alive until Ctrl+C.
This proves the AHP turn uses the SDK-created session's system prompt and
event stream, rather than creating another session. Permission requests are
denied by the SDK's normal `onPermissionRequest` callback.
Set `COPILOT_MODEL` to override the sample's `gpt-4.1` model.

The public SDK facade is:

```js
await client.start();
const { url } = await client.startAhpHost();
// Create sessions normally; connect an AHP client to url.
await client.stopAhpHost(); // Stops the endpoint, not the SDK sessions.
```

This is a local MVP, not a remote hosting deployment guide. Do not expose the
endpoint to untrusted networks. It demonstrates a single text turn; it does
not demonstrate full AHP features, reconnection, or remote authentication.

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
