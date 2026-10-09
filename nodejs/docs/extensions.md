# Copilot CLI Extensions

Extensions add custom tools, hooks, and behaviors to the Copilot CLI. They run as separate Node.js processes that communicate with the CLI over JSON-RPC via stdio.

## How Extensions Work

```
┌─────────────────────┐          JSON-RPC / stdio           ┌──────────────────────┐
│   Copilot CLI        │ ◄──────────────────────────────────► │  Extension Process   │
│   (parent process)   │    tool calls, events, hooks        │  (forked child)      │
│                      │                                      │                      │
│  • Discovers exts    │                                      │  • Registers tools   │
│  • Forks processes   │                                      │  • Registers hooks   │
│  • Routes tool calls │                                      │  • Listens to events │
│  • Manages lifecycle │                                      │  • Uses SDK APIs     │
└─────────────────────┘                                      └──────────────────────┘
```

1. **Discovery**: The CLI scans `.github/extensions/` (project) and the user's copilot config extensions directory for subdirectories containing `extension.mjs`.
2. **Launch**: Each extension is forked as a child process with `@github/copilot-sdk` available via an automatic module resolver.
3. **Connection**: The extension calls `joinSession()` which establishes a JSON-RPC connection over stdio to the CLI and attaches to the user's current foreground session.
4. **Registration**: Tools and hooks declared in the session options are registered with the CLI and become available to the agent.
5. **Lifecycle**: Extensions are reloaded on `/clear` (or if the foreground session is replaced) and stopped on CLI exit (SIGTERM, then SIGKILL after 5s).

## File Structure

```
.github/extensions/
  my-extension/
    extension.mjs      ← Entry point (required, must be .mjs)
```

- Only `.mjs` files are supported (ES modules). The file must be named `extension.mjs`.
- Each extension lives in its own subdirectory.
- The `@github/copilot-sdk` import is resolved automatically — you don't install it.

## The SDK

Extensions use `@github/copilot-sdk` for all interactions with the CLI:

```js
import { joinSession } from "@github/copilot-sdk/extension";

const session = await joinSession({
    tools: [
        /* ... */
    ],
    hooks: {
        /* ... */
    },
});
```

The `session` object provides methods for sending messages, logging to the timeline, listening to events, and accessing the RPC API. See the `.d.ts` files in the SDK package for full type information.

## Requesting Sensitive Environment Variables

The CLI strips sensitive environment variables (for example `GITHUB_TOKEN`) from every extension process before it starts. An extension that needs one asks for it by name:

```js
import { joinSession } from "@github/copilot-sdk/extension";

const session = await joinSession({
    requestedEnvironmentVariables: ["GITHUB_TOKEN"],
});

// Granted values are in process.env once joinSession resolves.
const token = process.env.GITHUB_TOKEN;
```

The CLI prompts the user with the extension's name and the exact list of variables requested. If the user approves, only those variables reach this extension and their values are written into `process.env` before `joinSession()` resolves. If the user denies, `joinSession()` rejects, the extension does not load, and its tools never reach the model.

An approval is remembered against the exact set of names the user saw, so an extension that later asks for an additional variable prompts again. Names that are unset, or that the CLI does not filter from extensions, are not prompted for.

An approved extension can pass a granted value to anything it starts, so ask only for what the extension genuinely needs.

## Desktop Notifications (Experimental)

Notifications run in the extension provider process, not in canvas HTML. A
provider can notify while its canvas is hidden, provided the provider and its
session are still running. Declare the opt-in when joining:

```js
const session = await joinSession({ requestNotifications: true });
const capabilities = await session.notifications.getCapabilities();
```

`getCapabilities()` reports `available`, `unsupported`, `unavailable`, or
`failed`. An available result includes the host platform, current extension and
OS permission states, supported click targets, and supported sound modes and
names. The opt-in alone grants no permission. After an explicit user action,
call `requestPermission()` and inspect its result before enabling delivery.
Permission requests and delivery must not run merely because a canvas opened.

```js
const result = await session.notifications.show({
    title: "Pull request updated",
    body: "A review is ready.",
    onClick: { kind: "open-url", url: "https://github.com/example/project/pull/42" },
    sound: { kind: "none" },
});
```

`accepted` means the host handed the notification to the OS; it does not promise
that the OS displayed it. Other results are `denied`, `unsupported`,
`unavailable`, `failed`, or `invalid-request`. Transport errors reject the
promise. Never retry or switch to another notification mechanism after `show()`
fails: the OS may have accepted it before the response was lost.

All three methods accept a final, non-wire `{ signal: AbortSignal }` option.
For example, use `show(notification, { signal: controller.signal })` to cancel a
pending request when the user disables notifications. Cancellation is forwarded
to the runtime; an already-aborted signal sends nothing. It cannot retract a
notification that the OS has already accepted.

Click targets are credential-free HTTP(S) URLs or
`{ kind: "focus-canvas", canvasId, instanceId? }` for a canvas declared by this
provider. There are no JavaScript callbacks, shell commands, or arbitrary canvas
actions. The runtime authenticates the originating provider; callers cannot
supply an identity. Supporting hosts authenticate URL activation data in OS
metadata so URL clicks can survive a restart. Canvas focus is live-session-only.

Sound defaults to `{ kind: "default" }`. `{ kind: "none" }` requests silence;
`{ kind: "named", name }` requires a name advertised by the host. Unsupported
sounds are rejected, not silently substituted.

An older runtime may ignore `requestNotifications`. Only capability discovery
maps JSON-RPC method-not-found to `unsupported`. A consumer may select a legacy
adapter after that negotiation, but must not bypass denied permission or switch
adapters after attempting delivery.

The API does not add notification content to the agent conversation, session
events, or SDK logs. Do not log the notification, URL, callback context, or
host errors in provider code. OS notification retention is the explicit
exception; avoid secrets and sensitive information in notification text.

## Further Reading

- `examples.md` — Practical code examples for tools, hooks, events, and complete extensions
- `workflows.md` — Authoring, running, resuming, and observing Dynamic Workflows
- `agent-author.md` — Step-by-step workflow for agents authoring extensions programmatically
