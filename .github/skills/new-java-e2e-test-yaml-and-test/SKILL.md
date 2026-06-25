---
name: new-java-e2e-test-yaml-and-test
description: "Use this skill when creating a new Java E2E integration test (failsafe IT) that requires a new replay proxy YAML snapshot file in test/snapshots/"
---

# Creating a New Java E2E Test with a Replay Proxy YAML Snapshot

This skill covers the complete workflow for adding a new Java failsafe
integration test backed by a handcrafted YAML snapshot for the replay proxy.

## Overview

The Java E2E tests use a **replay proxy** (`test/harness/replayingCapiProxy.ts`)
that intercepts HTTP calls to the Copilot API and returns pre-recorded responses
from YAML snapshot files. This avoids needing real authentication in CI.

**Key constraint:** Java's `CapiProxy.java` always sets `GITHUB_ACTIONS=true`
(line 104), which forces the replay proxy into read-only mode. You **cannot**
record snapshots by running Java tests — you must handcraft the YAML.

## Step-by-Step Workflow

### Step 1: Choose a snapshot category and snapshot base name

- Category = a directory under `test/snapshots/` (e.g., `system_message_sections`)
- Snapshot base name = the exact filename stem to use (already lowercase/underscore-separated),
  e.g., `should_use_replaced_identity_section_in_response`
- Resulting file: `test/snapshots/<category>/<snapshot_base_name>.yaml`

### Step 2: Create the YAML snapshot file

The format is:

```yaml
models:
  - claude-sonnet-4.5
conversations:
  - messages:
      - role: system
        content: ${system}
      - role: user
        content: <the exact prompt your test will send>
      - role: assistant
        content: <the response the proxy will return>
```

**Rules:**
- `${system}` is a placeholder that matches ANY system message content
- `${workdir}` in tool arguments is substituted with the actual temp workDir
- Each conversation entry represents one request-response exchange
- For multi-turn, add multiple conversation entries
- For tool calls, include `tool_calls` on assistant messages and `role: tool` for results
- The user content must **exactly match** what your test sends (after normalization)

### Step 3: Create the Java IT test class

Place it in `java/src/test/java/com/github/copilot/` with an `IT` suffix
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

```sh
cd java
mvn spotless:apply
mvn failsafe:integration-test -Dit.test="MyFeatureIT#myTestMethod" -Denforcer.skip=true
```

Then run the full build to confirm no regressions:

```sh
mvn clean verify
```

## Key Classes and Files

| What | Where |
|------|-------|
| Test context (manages proxy, workDir, CLI) | `java/src/test/java/com/github/copilot/E2ETestContext.java` |
| Java proxy wrapper | `java/src/test/java/com/github/copilot/CapiProxy.java` |
| Replay proxy (TypeScript) | `test/harness/replayingCapiProxy.ts` |
| Proxy server entry point | `test/harness/server.ts` |
| Snapshot files | `test/snapshots/<category>/<name>.yaml` |
| Existing IT tests for reference | `java/src/test/java/com/github/copilot/*IT.java` |

## How the Proxy Matches Requests

1. The proxy normalizes the incoming request's messages
2. It compares against each conversation in the YAML:
   - System message matches if YAML has `${system}` (wildcard)
   - User messages are compared by content (exact text match)
   - Tool results are compared after normalizing `${workdir}` paths
3. If a match is found, the proxy returns the **next assistant message after the matched request prefix**
4. If no match, in CI mode (`GITHUB_ACTIONS=true`) it errors with "No cached response found"

## YAML Format for Tool Calls

If your test involves tool use:

```yaml
conversations:
  # First exchange: model wants to call a tool
  - messages:
      - role: system
        content: ${system}
      - role: user
        content: Read the file test.txt
      - role: assistant
        content: I'll read that file.
        tool_calls:
          - id: toolcall_0
            type: function
            function:
              name: view
              arguments: '{"path":"${workdir}/test.txt"}'
  # Second exchange: after tool result is provided, model gives final answer
  - messages:
      - role: system
        content: ${system}
      - role: user
        content: Read the file test.txt
      - role: assistant
        content: I'll read that file.
        tool_calls:
          - id: toolcall_0
            type: function
            function:
              name: view
              arguments: '{"path":"${workdir}/test.txt"}'
      - role: tool
        tool_call_id: toolcall_0
        content: "1. Hello world!"
      - role: assistant
        content: The file test.txt contains "Hello world!"
```

**Important:** When the model calls tools like `view`, the CLI actually executes
them locally. The file must exist in the test's workDir. Create it in your test
before sending the prompt:

```java
Files.writeString(ctx.getWorkDir().resolve("test.txt"), "Hello world!\n");
```

## Common Pitfalls

1. **Prompt mismatch** — The user content in YAML must exactly match what
   `session.sendAndWait(new MessageOptions().setPrompt("..."))` sends.
2. **Forgetting `${system}`** — Always use `${system}` for the system role content
   unless testing a specific system message matching scenario.
3. **Tool execution** — If the snapshot has the model calling `view` or other
   built-in tools, the CLI will actually execute those tools. Files must exist.
4. **Snapshot name parameter** — pass the explicit snapshot base name to
   `configureForTest`, e.g., `configureForTest("category", "my_method_name")`.
   Do not rely on camelCase-to-snake_case conversion.
5. **Cannot record via Java** — `CapiProxy.java` forces `GITHUB_ACTIONS=true`.
   Always handcraft snapshots or use the Node.js proxy directly for recording.
