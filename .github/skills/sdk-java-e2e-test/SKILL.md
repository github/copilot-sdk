---
name: sdk-java-e2e-test
description: "Use this skill when creating a Java SDK surface-area E2E integration test backed by a replay proxy YAML snapshot"
---

# Creating a New Java E2E Test with a Replay Proxy YAML Snapshot

## Repository layout

These instructions work in both the runtime monorepo and the standalone public
SDK repository. From the repository root, use `src/sdk` as the SDK root when
that directory exists; otherwise use the repository root itself. All paths below
are relative to that SDK root.

Use this skill only when the new E2E tests Java SDK surface area; shared
runtime functionality available entirely through the SDK belongs in the
TypeScript SDK suite (`nodejs/test/e2e/`).

## Overview

The Java E2E tests use a **replay proxy** (`test/harness/replayingCapiProxy.ts`)
that intercepts HTTP calls to the Copilot API and returns pre-recorded responses
from YAML snapshot files. This avoids needing real authentication in CI.

**Key constraint:** Java's `CapiProxy.start()` always sets `GITHUB_ACTIONS=true`,
which forces the replay proxy into read-only mode: a Java test cannot record a
snapshot, and every request must match an existing one.

## Step-by-Step Workflow

### Step 1: Choose a snapshot category and snapshot base name

- Category = a directory under `test/snapshots/` (e.g., `system_message_sections`)
- Snapshot base name = the exact filename stem, already lowercase snake_case,
  e.g., `should_use_replaced_identity_section_in_response`. Pass it verbatim to
  `configureForTest`: it lowercases and replaces non-alphanumerics with `_` but
  does not split camelCase, so `myTestMethod` would look for `mytestmethod.yaml`.
- Resulting file: `test/snapshots/<category>/<snapshot_base_name>.yaml`

### Step 2: Use a recorded snapshot

The SDK guide forbids hand-authored model responses (`CONTRIBUTING.md`,
"Recording and replaying SDK tests"). Reuse a capture that another SDK suite
recorded for the same conversation, as most Java ITs do; if none exists, record
one through the TypeScript SDK suite with the same snapshot name and prompt,
following that guide.

**Replay rules:**
- Every request's system message is replaced with `${system}` before matching,
  so system content is never compared, and an assertion on the reply cannot
  prove that a system-message setting reached the model.
- User content must **exactly match** the snapshot's (after normalization), so
  copy the prompt from the YAML.
- `${workdir}` stands for the test's temp workDir in tool arguments and results.
- A request matches when its messages equal a conversation up to an assistant
  message; the proxy replies with that assistant message and any assistant
  messages directly after it.
- A request that matches nothing fails. The proxy's stderr, echoed as
  `[CapiProxy stderr]`, reports "No cached response found" with the first
  mismatching message per conversation, and `configureForTest` logs the prompts
  the snapshot expects.

### Step 3: Create the Java IT test class

Place it in `java/sdk/src/test/java/com/github/copilot/` with an `IT` suffix
(e.g., `MyFeatureIT.java`). The failsafe plugin picks up `*IT.java` files.

**Template:**

```java
package com.github.copilot;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.github.copilot.generated.AssistantMessageEvent;
import com.github.copilot.rpc.MessageOptions;
import com.github.copilot.rpc.PermissionHandler;
import com.github.copilot.rpc.SessionConfig;
// ... other imports as needed

class MyFeatureIT {

    private static E2ETestContext ctx;

    @BeforeAll
    static void setUp() throws Exception {
        ctx = E2ETestContext.create();
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (ctx != null) {
            ctx.close();
        }
    }

    @Test
    void myTestMethod() throws Exception {
        // 1. Configure the proxy to use your snapshot
        ctx.configureForTest("my_category", "my_test_method");

        // 2. Create a client (uses fake token + proxy automatically)
        try (CopilotClient client = ctx.createClient()) {

            // 3. Create a session with desired config
            CopilotSession session = client.createSession(new SessionConfig()
                    .setOnPermissionRequest(PermissionHandler.APPROVE_ALL))
                    .get(30, TimeUnit.SECONDS);

            try {
                // 4. Send the prompt (must match YAML exactly)
                AssistantMessageEvent response = session
                        .sendAndWait(new MessageOptions().setPrompt("Your prompt here"), 60_000)
                        .get(90, TimeUnit.SECONDS);

                // 5. Assert on the response
                assertNotNull(response);
                String content = response.getData().content();
                assertTrue(content.contains("expected text"));
            } finally {
                session.close();
            }
        }
    }
}
```

### Step 4: Verify

In the runtime repository, prepare the checked-out Java projection and CLI
from the runtime root:

```sh
pnpm run generate:sdk:java
pnpm run build:cli
```

`pnpm run test:sdk:java` runs the whole suite and takes no test filter. For a
focused Maven run, pass the same-checkout wrapper explicitly (without
`copilot.cli.path` the tests use the release pinned by `nodejs/package.json`),
then use the `verify` lifecycle so test-resource setup and `failsafe:verify`
both run:

```sh
cd src/sdk/java
./mvnw -pl sdk spotless:apply
COPILOT_CLI_PATH=<runtime-root>/dist-cli/prebuilds/<target>/copilot-runtime
./mvnw -pl sdk verify -Dit.test="MyFeatureIT#myTestMethod" -Dcopilot.cli.path="$COPILOT_CLI_PATH"
```

`<target>` is Node's `<process.platform>-<process.arch>` (for example
`linux-x64`, `darwin-arm64`, `win32-x64`); use `copilot-runtime.exe` for the
wrapper name on Windows.

In the standalone SDK repository, start at `java/` and use the pinned runtime:

```sh
./mvnw -pl sdk spotless:apply
./mvnw -pl sdk verify -Dit.test="MyFeatureIT#myTestMethod"
```

## Key Classes and Files

| What | Where |
|------|-------|
| Test context (manages proxy, workDir, CLI) | `java/sdk/src/test/java/com/github/copilot/E2ETestContext.java` |
| Java proxy wrapper | `java/sdk/src/test/java/com/github/copilot/CapiProxy.java` |
| Replay proxy (TypeScript) | `test/harness/replayingCapiProxy.ts` |
| Proxy server entry point | `test/harness/server.ts` |
| Snapshot files | `test/snapshots/<category>/<name>.yaml` |
| Existing IT tests for reference | `java/sdk/src/test/java/com/github/copilot/**/*IT.java`; `SystemMessageSectionsIT` reuses Node.js suite captures, including a `view` tool call |

## Tests that call built-in tools

When the snapshot has the model call a built-in tool such as `view`, the CLI
really executes it in the test's workDir, and the next request carries its
result, which must match the snapshot's. Create any file the tool reads before
sending the prompt, with exactly the content the snapshot's tool result shows:

```java
Files.writeString(ctx.getWorkDir().resolve("test.txt"), "Hello transform!");
```
