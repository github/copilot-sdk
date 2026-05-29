# Plan: Add Java to `scenario-builds.yml` (copilot-sdk #1423)

## Context

The monorepo's `scenario-builds.yml` workflow verifies that example scenarios under `test/scenarios/` compile for each SDK language. Java is currently absent — zero Java implementations exist in any scenario directory and the workflow has no Java job. This plan adds Java parity with C# and Go.

**This work must NOT change anything in the Java SDK itself (`java/src/`, `java/pom.xml`, etc.).** All changes are confined to:

- `test/scenarios/` (new Java scenario implementations)
- `.github/workflows/scenario-builds.yml` (new `build-java` job)

---

## Pre-requisites: Files you MUST read before starting

1. **`.github/copilot-instructions.md`** — Read the Java-specific sections (build commands, conventions, coding patterns).
2. **`.github/workflows/copilot-setup-steps.yml`** — Read the Java setup steps (JDK 17, Maven cache, codegen deps) to understand environment assumptions.
3. **`.github/workflows/java-codegen-fix.md`** — Read to understand the constraint: NEVER hand-edit `java/src/generated/java/`. This work does not touch that directory at all, but be aware of the boundary.
4. **`.github/workflows/scenario-builds.yml`** — Read to understand the existing pattern for other languages.
5. **`java/README.md`** — Read the Quick Start section for SDK usage patterns.
6. **One existing C# scenario** — e.g., `test/scenarios/modes/default/csharp/Program.cs` — to understand the translation target.

---

## Constraints

- ❌❌❌ **DO NOT modify any file under `java/src/` or `java/pom.xml`.** This work is purely about adding Java scenarios under `test/scenarios/` and updating the workflow.
- ❌❌❌ **DO NOT modify any existing scenario** in another language.
- ✅ Work in the current topic branch. Make fine-grained commits with reasonable messages (e.g., "Add Java scenario: modes/default", "Add build-java job to scenario-builds.yml").
- ✅ Each Java scenario must **compile** with `mvn compile` from its directory. It does NOT need to run successfully (no Copilot CLI available in CI for scenarios).
- ✅ Use JDK 17 language level (`<maven.compiler.release>17</maven.compiler.release>`).
- ✅ Follow Java SDK conventions: 4-space indent, no wildcard imports, use `CompletableFuture` for async.

---

## Phase 1: Size S scenarios (trivial config translations)

These are ~20-40 line programs that demonstrate a single configuration option. Create Java implementations for each:

| #   | Scenario path                                     | What it demonstrates                          |
| --- | ------------------------------------------------- | --------------------------------------------- |
| 1   | `test/scenarios/modes/default/java/`              | Basic session with default tools              |
| 2   | `test/scenarios/modes/minimal/java/`              | `availableTools = []`, custom system message  |
| 3   | `test/scenarios/prompts/system-message/java/`     | `SystemMessageConfig` with REPLACE mode       |
| 4   | `test/scenarios/prompts/reasoning-effort/java/`   | `setReasoningEffort("low")`                   |
| 5   | `test/scenarios/sessions/streaming/java/`         | `setStreaming(true)`, listen for delta events |
| 6   | `test/scenarios/sessions/infinite-sessions/java/` | `InfiniteSessionConfig`                       |
| 7   | `test/scenarios/tools/no-tools/java/`             | Empty available tools list                    |
| 8   | `test/scenarios/tools/tool-filtering/java/`       | Whitelist specific tools                      |
| 9   | `test/scenarios/transport/stdio/java/`            | Default stdio transport (simplest possible)   |
| 10  | `test/scenarios/transport/tcp/java/`              | `setCliUrl(...)` for TCP connection           |
| 11  | `test/scenarios/callbacks/user-input/java/`       | `UserInputHandler` callback                   |
| 12  | `test/scenarios/auth/byok-openai/java/`           | BYOK with OpenAI provider config              |
| 13  | `test/scenarios/auth/byok-azure/java/`            | BYOK with Azure provider config               |
| 14  | `test/scenarios/auth/byok-anthropic/java/`        | BYOK with Anthropic provider config           |
| 15  | `test/scenarios/bundling/fully-bundled/java/`     | Default stdio (same as transport/stdio)       |
| 16  | `test/scenarios/bundling/app-direct-server/java/` | TCP connection to pre-running server          |

---

## Phase 2: Size M scenarios (moderate implementation)

These are ~40-70 line programs demonstrating more complex features:

| #   | Scenario path                                       | What it demonstrates                                           |
| --- | --------------------------------------------------- | -------------------------------------------------------------- |
| 17  | `test/scenarios/callbacks/hooks/java/`              | Multiple `SessionHooks` (pre/post tool use, session start/end) |
| 18  | `test/scenarios/callbacks/permissions/java/`        | `PermissionHandler` / `onPermissionRequest`                    |
| 19  | `test/scenarios/prompts/attachments/java/`          | `MessageAttachment` with file content                          |
| 20  | `test/scenarios/sessions/concurrent-sessions/java/` | Two sessions with different system prompts                     |
| 21  | `test/scenarios/sessions/session-resume/java/`      | `resumeSession()` with session ID                              |
| 22  | `test/scenarios/tools/custom-agents/java/`          | `CustomAgentConfig`, `DefaultAgentConfig.excludedTools`        |
| 23  | `test/scenarios/tools/tool-overrides/java/`         | Custom tool with `overridesBuiltInTool(true)`                  |
| 24  | `test/scenarios/tools/mcp-servers/java/`            | `McpServerConfig` (stdio type)                                 |
| 25  | `test/scenarios/tools/skills/java/`                 | `skillDirectories` configuration                               |
| 26  | `test/scenarios/auth/gh-app/java/`                  | OAuth Device Flow + Copilot session                            |

---

## Scenarios explicitly SKIPPED (do not implement)

| Scenario                          | Reason                                            |
| --------------------------------- | ------------------------------------------------- |
| `transport/reconnect`             | TypeScript-only by design (SDK-internal concern)  |
| `tools/virtual-filesystem`        | Java SDK lacks virtual FS API (only TS + Go)      |
| `sessions/multi-user-short-lived` | Depends on virtual filesystem pattern             |
| `sessions/multi-user-long-lived`  | Size L; only TS/Py/Go have it; defer              |
| `bundling/app-backend-to-server`  | Size L; needs HTTP framework (Spring Boot); defer |
| `bundling/container-proxy`        | Size XL; multi-file Docker setup; defer           |
| `auth/byok-ollama`                | Nice-to-have; defer (same pattern as byok-openai) |

---

## File structure for each Java scenario

Each scenario gets exactly two files:

### `pom.xml` template

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                        http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.github.copilot.sdk.scenarios</groupId>
    <artifactId>scenario-REPLACE_WITH_SCENARIO_NAME</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>

    <properties>
        <maven.compiler.release>17</maven.compiler.release>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencies>
        <dependency>
            <groupId>com.github</groupId>
            <artifactId>copilot-sdk-java</artifactId>
            <version>1.0.0-beta-java.5-SNAPSHOT</version>
        </dependency>
    </dependencies>

    <repositories>
        <!-- Required to resolve the local SDK from the monorepo build -->
        <repository>
            <id>local-repo</id>
            <url>file://${project.basedir}/../../../../../java/target/local-repo</url>
        </repository>
    </repositories>
</project>
```

**IMPORTANT**: The `pom.xml` above assumes the SDK JAR has been installed to a local repo. However, for `scenario-builds.yml`, the build job must first run `mvn install -DskipTests` from `java/` to make the artifact available. See the workflow job below.

### `Main.java` location

```
test/scenarios/<category>/<scenario>/java/src/main/java/Main.java
```

Use the default package (no `package` statement) for simplicity, matching how some other languages keep scenarios minimal.

---

## Java API patterns to use (reference)

### Basic session (modes/default):

```java
import com.github.copilot.sdk.CopilotClient;
import com.github.copilot.sdk.json.CopilotClientOptions;
import com.github.copilot.sdk.json.MessageOptions;
import com.github.copilot.sdk.json.PermissionHandler;
import com.github.copilot.sdk.json.SessionConfig;
import com.github.copilot.sdk.generated.AssistantMessageEvent;

public class Main {
    public static void main(String[] args) throws Exception {
        try (var client = new CopilotClient()) {
            client.start().get();
            var session = client.createSession(
                new SessionConfig()
                    .setModel("claude-haiku-4.5")
                    .setOnPermissionRequest(PermissionHandler.APPROVE_ALL))
                .get();
            var response = session.sendAndWait(
                new MessageOptions().setPrompt("What is 2+2?"))
                .get();
            if (response != null) {
                System.out.println(response.getData().content());
            }
            client.stop().get();
        }
    }
}
```

### Streaming:

```java
import com.github.copilot.sdk.CopilotClient;
import com.github.copilot.sdk.json.*;
import com.github.copilot.sdk.generated.AssistantMessageDeltaEvent;

public class Main {
    public static void main(String[] args) throws Exception {
        try (var client = new CopilotClient()) {
            client.start().get();
            var session = client.createSession(
                new SessionConfig()
                    .setModel("claude-haiku-4.5")
                    .setStreaming(true)
                    .setOnPermissionRequest(PermissionHandler.APPROVE_ALL))
                .get();
            int[] chunkCount = {0};
            session.on(AssistantMessageDeltaEvent.class, evt -> chunkCount[0]++);
            session.sendAndWait(new MessageOptions().setPrompt("What is the capital of France?")).get();
            System.out.println("Streaming chunks received: " + chunkCount[0]);
            client.stop().get();
        }
    }
}
```

### Tools:

```java
import com.github.copilot.sdk.CopilotClient;
import com.github.copilot.sdk.json.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class Main {
    public static void main(String[] args) throws Exception {
        var parameters = Map.of(
            "type", "object",
            "properties", Map.of(
                "query", Map.of("type", "string", "description", "Search query")),
            "required", List.of("query"));

        var tool = ToolDefinition.create("custom_grep", "Custom grep tool", parameters,
            invocation -> {
                String query = (String) invocation.getArguments().get("query");
                return CompletableFuture.completedFuture("CUSTOM_GREP_RESULT: " + query);
            });

        try (var client = new CopilotClient()) {
            client.start().get();
            var session = client.createSession(
                new SessionConfig()
                    .setModel("claude-haiku-4.5")
                    .setTools(List.of(tool))
                    .setOnPermissionRequest(PermissionHandler.APPROVE_ALL))
                .get();
            session.sendAndWait(new MessageOptions().setPrompt("Search for 'hello'")).get();
            client.stop().get();
        }
    }
}
```

### Hooks:

```java
import com.github.copilot.sdk.CopilotClient;
import com.github.copilot.sdk.json.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class Main {
    public static void main(String[] args) throws Exception {
        var hookLog = new ArrayList<String>();
        try (var client = new CopilotClient()) {
            client.start().get();
            var session = client.createSession(
                new SessionConfig()
                    .setModel("claude-haiku-4.5")
                    .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                    .setHooks(new SessionHooks()
                        .setOnPreToolUse((input, inv) -> {
                            hookLog.add("preToolUse:" + input.getToolName());
                            return CompletableFuture.completedFuture(PreToolUseHookOutput.allow());
                        })
                        .setOnPostToolUse((input, inv) -> {
                            hookLog.add("postToolUse:" + input.getToolName());
                            return CompletableFuture.completedFuture(null);
                        })))
                .get();
            session.sendAndWait(new MessageOptions().setPrompt("List files with glob '*.md'")).get();
            hookLog.forEach(entry -> System.out.println("  " + entry));
            client.stop().get();
        }
    }
}
```

### BYOK Provider:

```java
import com.github.copilot.sdk.CopilotClient;
import com.github.copilot.sdk.json.*;

public class Main {
    public static void main(String[] args) throws Exception {
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null) apiKey = "sk-placeholder";

        try (var client = new CopilotClient(new CopilotClientOptions()
                .setProvider(new ProviderConfig()
                    .setType("openai")
                    .setApiKey(apiKey)))) {
            client.start().get();
            var session = client.createSession(
                new SessionConfig().setModel("gpt-4o-mini"))
                .get();
            session.sendAndWait(new MessageOptions().setPrompt("Hello")).get();
            client.stop().get();
        }
    }
}
```

---

## Update to `scenario-builds.yml`

Add a new `build-java` job after the existing `build-rust` job. Model it after the C# job but use Maven:

```yaml
# ── Java ────────────────────────────────────────────────────────────
build-java:
  name: "Java scenarios"
  if: github.event.repository.fork == false
  runs-on: ubuntu-latest
  steps:
    - uses: actions/checkout@v6

    - uses: actions/setup-java@v4
      with:
        distribution: "temurin"
        java-version: "17"
        cache: "maven"

    # Install SDK to local Maven repo so scenarios can resolve it
    - name: Install SDK to local repo
      working-directory: java
      run: mvn install -DskipTests -q

    - name: Build all Java scenarios
      run: |
        PASS=0; FAIL=0; FAILURES=""
        for pom in $(find test/scenarios -path '*/java/pom.xml' | sort); do
          dir=$(dirname "$pom")
          scenario="${dir#test/scenarios/}"
          echo "::group::$scenario"
          if (cd "$dir" && mvn compile -q 2>&1); then
            echo "✅ $scenario"
            PASS=$((PASS + 1))
          else
            echo "❌ $scenario"
            FAIL=$((FAIL + 1))
            FAILURES="$FAILURES\n  $scenario"
          fi
          echo "::endgroup::"
        done
        echo ""
        echo "Java builds: $PASS passed, $FAIL failed"
        if [ "$FAIL" -gt 0 ]; then
          echo -e "Failures:$FAILURES"
          exit 1
        fi
```

Also add `"java/src/**"` to the `paths` trigger list (both `pull_request` and `push` sections).

---

## Execution order

1. First, add the `build-java` job to `.github/workflows/scenario-builds.yml` (including path triggers).
2. Create Phase 1 scenarios (size S, #1-#16). Commit after each 3-4 related scenarios.
3. Create Phase 2 scenarios (size M, #17-#26). Commit after each 2-3 related scenarios.
4. Final commit: verify all scenarios compile by running `cd test/scenarios/<category>/<scenario>/java && mvn compile -q` for each.

---

## Verification

After all scenarios are created, run this from the repo root to confirm they all compile:

```bash
cd java && mvn install -DskipTests -q
cd ..
for pom in $(find test/scenarios -path '*/java/pom.xml' | sort); do
  dir=$(dirname "$pom")
  echo -n "$dir: "
  (cd "$dir" && mvn compile -q 2>&1 && echo "✅") || echo "❌"
done
```

Every scenario must show ✅. If any fails, fix the Java source code in that scenario (NOT in `java/src/`).

---

## Dependency resolution approach

The scenarios reference `com.github:copilot-sdk-java:1.0.0-beta-java.5-SNAPSHOT`. For the workflow build to succeed, the SDK must be installed to the local Maven repository first. The `build-java` job handles this with `mvn install -DskipTests -q` in the `java/` directory before iterating scenarios.

For local development, run:

```bash
cd java && mvn install -DskipTests
```

The scenario `pom.xml` files do NOT need a `<repositories>` section pointing to a file-based repo — the standard `~/.m2/repository` local repo is sufficient after `mvn install`.

**Corrected `pom.xml` template** (simpler, no file-based repo needed):

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                        http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.github.copilot.sdk.scenarios</groupId>
    <artifactId>scenario-REPLACE_WITH_SCENARIO_NAME</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>

    <properties>
        <maven.compiler.release>17</maven.compiler.release>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <dependencies>
        <dependency>
            <groupId>com.github</groupId>
            <artifactId>copilot-sdk-java</artifactId>
            <version>1.0.0-beta-java.5-SNAPSHOT</version>
        </dependency>
    </dependencies>
</project>
```

This is sufficient because `mvn install` in `java/` puts the artifact into `~/.m2/repository` which Maven resolves automatically.
