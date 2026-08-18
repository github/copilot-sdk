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

## Requesting sensitive environment variables

The CLI strips sensitive environment variables (for example `GITHUB_TOKEN`) from every extension process before it starts. An extension that needs one asks for it by name:

```js
import { joinSession } from "@github/copilot-sdk/extension";

const session = await joinSession({ env: ["GITHUB_TOKEN"] });

// Granted values are in process.env once joinSession resolves.
const token = process.env.GITHUB_TOKEN;
```

The CLI prompts the user with the extension's name and the exact list of variables requested. If the user approves, only those variables reach this extension and their values are written into `process.env` before `joinSession()` resolves. If the user denies, `joinSession()` rejects, the extension does not load, and its tools never reach the model.

An approval is remembered against the exact set of names the user saw, so an extension that later asks for an additional variable prompts again. Names that are unset, or that the CLI does not filter from extensions, are not prompted for.

An approved extension can pass a granted value to anything it starts, so ask only for what the extension genuinely needs.

## Further Reading

- `examples.md` — Practical code examples for tools, hooks, events, and complete extensions
- `factories.md`: Authoring, running, resuming, and observing Agent Factories
- `agent-author.md` — Step-by-step workflow for agents authoring extensions programmatically
