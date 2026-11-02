# tokenjuice integration

[tokenjuice](https://github.com/vincentkoc/tokenjuice) is a third-party, MIT-licensed
output compactor. Its TypeScript adapter uses the SDK's
[post-tool use hook](../hooks/post-tool-use.md) to reduce eligible Bash output
before the next model request. It runs locally and requires no additional model
service or API key.

## Installation

Use Node.js 22 or newer and an application with a working
[Copilot SDK setup](../getting-started.md). Install tokenjuice alongside the SDK:

```shell
npm install @github/copilot-sdk tokenjuice
```

The adapter is available in tokenjuice 0.10.0 and newer. Its native integration
tests are qualified against `@github/copilot-sdk` 1.0.28.

## Add the hook to an existing session configuration

Pass your application's session configuration through this helper before calling
`client.createSession`. It retains permission handling and other hooks. If an
existing post-tool hook replaces or suppresses a result, its decision takes
precedence; otherwise tokenjuice can compact it while retaining additional
context from that hook.

```typescript
import type { SessionConfig } from "@github/copilot-sdk";
import { createCopilotSdkHooks } from "tokenjuice/copilot-sdk";

export function withTokenjuice(config: SessionConfig): SessionConfig {
  const compact = createCopilotSdkHooks().onPostToolUse;
  const previous = config.hooks?.onPostToolUse;

  return {
    ...config,
    hooks: {
      ...config.hooks,
      onPostToolUse: async (input, invocation) => {
        const existing = await previous?.(input, invocation);
        if (existing?.modifiedResult !== undefined || existing?.suppressOutput) {
          return existing;
        }
        const replacement = await compact(input);
        return replacement ? { ...existing, ...replacement } : existing;
      },
    },
  };
}
```

For an application without an existing post-tool hook, use
`hooks: createCopilotSdkHooks()` directly. Retain the rest of your session
configuration, including `onPermissionRequest`. The adapter does not approve or
execute tool calls.

## What changes

Only native `bash` results with a recognized completion footer and shell exit
code zero are eligible. For example, a large `find` inventory can become a compact
summary. The result envelope, completion footer, and unrelated fields are
preserved. Failed or running commands, unknown result formats, other tools,
exact file reads, and explicit raw requests pass through. A successful tool
invocation by itself does not prove that the shell command succeeded.

Remove the wrapper or hook from the session configuration to disable the
integration. See tokenjuice's
[integration guide](https://github.com/vincentkoc/tokenjuice/blob/v0.10.0/docs/framework-integrations.md#github-copilot-sdk)
and [native contract tests](https://github.com/vincentkoc/tokenjuice/blob/v0.10.0/scripts/frameworks-node-e2e.mjs)
for setup details and model-request verification.
