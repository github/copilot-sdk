# GitHub Copilot SDK for Java

[![Build](https://github.com/github/copilot-sdk/actions/workflows/java-sdk-tests.yml/badge.svg)](https://github.com/github/copilot-sdk/actions/workflows/java-sdk-tests.yml)
[![Java 17+](https://img.shields.io/badge/Java-17%2B-blue?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

#### Latest release

[![GitHub Release Date](https://img.shields.io/github/release-date/github/copilot-sdk)](https://github.com/github/copilot-sdk/releases)
[![GitHub Release](https://img.shields.io/github/v/release/github/copilot-sdk)](https://github.com/github/copilot-sdk/releases)
[![Maven Central](https://img.shields.io/maven-central/v/com.github/copilot-sdk-java)](https://central.sonatype.com/artifact/com.github/copilot-sdk-java)
[![Javadoc](https://javadoc.io/badge2/com.github/copilot-sdk-java/javadoc.svg?q=1)](https://javadoc.io/doc/com.github/copilot-sdk-java/latest/index.html)

## Background

Java SDK for programmatic control of GitHub Copilot CLI, enabling you to build AI-powered applications and agentic workflows. The Java SDK tracks the official GitHub Copilot SDK family (TypeScript, Python, Go, .NET, and Rust).

## Prerequisites

To use the SDK, you'll need:

- Java 17 or later. **JDK 25 recommended**. The distributed jar is a multi-release jar (MR-JAR) and is compiled on JDK 25 with `maven.compiler.release` set to 17. This means, when run on JDK 25 and later, the SDK automatically uses virtual threads for its default internal executor.
- GitHub Copilot CLI 1.0.55-5 or later installed and in `PATH` (or provide custom `cliPath`)

## Installation

### Maven

Replace `${copilot.sdk.version}` with the latest release from Maven Central.

```xml
<dependency>
    <groupId>com.github</groupId>
    <artifactId>copilot-sdk-java</artifactId>
    <version>1.0.12-preview.0</version>
</dependency>
```

### Gradle

```groovy
implementation 'com.github:copilot-sdk-java:1.0.12-preview.0'
```

#### Snapshot Builds

Snapshot builds of the next development version are published to Maven Central Snapshots. To use them, add the repository and update the dependency version in your `pom.xml`:

```xml
<repositories>
    <repository>
        <id>central-snapshots</id>
        <url>https://central.sonatype.com/repository/maven-snapshots/</url>
        <snapshots><enabled>true</enabled></snapshots>
    </repository>
</repositories>

<dependency>
    <groupId>com.github</groupId>
    <artifactId>copilot-sdk-java</artifactId>
    <version>1.0.13-preview.0-SNAPSHOT</version>
</dependency>
```

### Gradle

Replace `${copilot.sdk.version}` with the latest release from Maven Central.

```groovy
implementation 'com.github:copilot-sdk-java:1.0.13-preview.0-SNAPSHOT'
```

## In-process mode (experimental)

The SDK supports running the Copilot runtime **in-process** as a native library instead of spawning a separate CLI process. This eliminates process management overhead and simplifies deployment. In-process mode is currently experimental and supported on **linux-x64** (glibc) and **win32-x64**.

Because in-process mode is experimental, see the [Using experimental APIs](#using-experimental-apis) section for how to opt in.

### Additional dependency

Add both the SDK and the platform-specific native runtime to your project:

```xml
<dependencies>
    <!-- Pure-Java SDK (~1.5 MB) -->
    <dependency>
        <groupId>com.github</groupId>
        <artifactId>copilot-sdk-java</artifactId>
        <version>${copilot.version}</version>
    </dependency>
    <!-- Add the native runtime for the target platform -->
    <dependency>
        <groupId>com.github</groupId>
        <artifactId>copilot-sdk-java-runtime</artifactId>
        <version>${copilot.version}</version>
        <classifier>linux-x64</classifier>
    </dependency>
    <!-- Use win32-x64 instead when the application runs on Windows x64 -->
    <!-- JNA (required for in-process mode) -->
    <dependency>
        <groupId>net.java.dev.jna</groupId>
        <artifactId>jna</artifactId>
        <version>5.19.1</version>
    </dependency>
</dependencies>
```

### Usage

Configure the client to use the in-process connection:

```java
CopilotClientOptions options = new CopilotClientOptions()
    .setConnection(RuntimeConnection.forInProcess());

CopilotClient client = new CopilotClient(options);
client.start().get();
```

## Quick Start

```java
import com.github.copilot.CopilotClient;
import com.github.copilot.generated.AssistantMessageEvent;
import com.github.copilot.generated.SessionUsageInfoEvent;
import com.github.copilot.rpc.MessageOptions;
import com.github.copilot.rpc.PermissionHandler;
import com.github.copilot.rpc.SessionConfig;

public class CopilotSDK {
    public static void main(String[] args) throws Exception {
        var lastMessage = new String[]{null};

        // Create and start client
        try (var client = new CopilotClient()) {
            client.start().get();

            // Create a session
            var session = client.createSession(
                new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL).setModel("claude-sonnet-4.5")).get();


            // Handle assistant message events
            session.on(AssistantMessageEvent.class, msg -> {
                lastMessage[0] = msg.getData().content();
                System.out.println(lastMessage[0]);
            });

            // Handle session usage info events
            session.on(SessionUsageInfoEvent.class, usage -> {
                var data = usage.getData();
                System.out.println("\n--- Usage Metrics ---");
                System.out.println("Current tokens: " + data.currentTokens().intValue());
                System.out.println("Token limit: " + data.tokenLimit().intValue());
                System.out.println("Messages count: " + data.messagesLength().intValue());
            });

            // Send a message
            var completable = session.sendAndWait(new MessageOptions().setPrompt("What is 2+2?"));
            // and wait for completion
            completable.get();
        }

        boolean success = lastMessage[0] != null && lastMessage[0].contains("4");
        System.exit(success ? 0 : -1);
    }
}
```

When targeting MCP tools configured through `setMcpServers(...)`, remember the
runtime tool name is `<server-key>-<tool-name>`. For `setAvailableTools(...)`
and `setExcludedTools(...)`, prefer the source-qualified filter form
`mcp:<server-key>-<tool-name>`. For `CustomAgentConfig.setTools(...)` and
`DefaultAgentConfig.setExcludedTools(...)`, use `<server-key>-<tool-name>`
directly.

`CopilotClientOptions.setCwd(...)` sets the runtime process working directory, which otherwise inherits the current process working directory. `SessionConfig.setWorkingDirectory(...)` sets the session working directory, which otherwise defaults to the runtime process working directory.

## Permission Handling

`PermissionHandler.APPROVE_ALL` approves requests when managed settings are disabled. When `enableManagedSettings` is true, it completes exceptionally. Custom handlers can inspect `request.getManagedApprovalRequired()` for human-facing confirmation logic.

When handling `PermissionRequestedEvent` directly, convert its generated event value with `PermissionRequest.fromJsonValue(event.getData().permissionRequest())` to access the typed metadata.

Custom handlers must check managed approval before applying kind-specific automatic decisions:

```java
import java.util.concurrent.CompletableFuture;

import com.github.copilot.rpc.PermissionHandler;
import com.github.copilot.rpc.PermissionRequestResult;

PermissionHandler handler = (request, invocation) -> {
    if (Boolean.TRUE.equals(request.getManagedApprovalRequired())) {
        return CompletableFuture.completedFuture(PermissionRequestResult.noResult());
    }

    return CompletableFuture.completedFuture(PermissionRequestResult.approveOnce());
};
```

## Try it with JBang

You can run the SDK without setting up a full Java project, by using [JBang](https://www.jbang.dev/).

See the full source of [`jbang-example.java`](sdk/jbang-example.java) for a complete example with more features like session idle handling and usage info events.

Or run it directly from the repository:

```bash
jbang https://github.com/github/copilot-sdk/blob/main/java/sdk/jbang-example.java
```

## Annotation-based tools and `ToolInvocation` context

When you define tools with `@CopilotTool`, parameters of type `ToolInvocation` are injected as runtime context and are not exposed in the tool schema.
`ToolInvocation` can appear before, between, or after schema-visible parameters.

```java
import com.github.copilot.rpc.ToolInvocation;
import com.github.copilot.tool.CopilotTool;
import com.github.copilot.tool.CopilotToolParam;

class ProgressTools {
    @CopilotTool("Reports the current phase and session")
    public String reportProgress(
            @CopilotToolParam("Current phase") String phase,
            ToolInvocation invocation) {
        return "phase=" + phase + ", sessionId=" + invocation.getSessionId();
    }
}
```

Position examples:

```java
@CopilotTool("Invocation first")
public String report(ToolInvocation invocation, @CopilotToolParam("Phase") String phase) { ... }

@CopilotTool("Invocation only")
public String onlyContext(ToolInvocation invocation) { ... }

@CopilotTool("Invocation middle")
public String report(@CopilotToolParam("Phase") String phase, ToolInvocation invocation, @CopilotToolParam("Limit") int limit) { ... }
```

## Inline lambda tool definitions (experimental)

For inline tool authoring at the session construction site, use `ToolDefinition.from(...)` with explicit parameter metadata:

```java
import com.github.copilot.rpc.ToolDefinition;
import com.github.copilot.rpc.ToolDefer;
import com.github.copilot.tool.Param;

ToolDefinition search = ToolDefinition
    .from(
        "search_items",
        "Searches indexed items by keyword",
        Param.of(String.class, "keyword", "Search keyword"),
        keyword -> "Searching for: " + keyword)
    .skipPermission(true)
    .defer(ToolDefer.AUTO);
```

### Parameter metadata with `Param.of(...)`

`Param.of(type, name, description)` creates a required parameter. For optional parameters with defaults:

```java
Param<Integer> limit = Param.of(Integer.class, "limit", "Max results", false, "10");
```

### Async handlers

Use `fromAsync` for asynchronous tool handlers:

```java
import java.util.concurrent.CompletableFuture;

ToolDefinition fetchData = ToolDefinition.fromAsync(
    "fetch_data",
    "Fetches data from remote source",
    Param.of(String.class, "url", "Data source URL"),
    url -> CompletableFuture.supplyAsync(() -> fetchRemote(url))
);
```

### ToolInvocation context injection

Inline tools can access `ToolInvocation` runtime context using `fromWithToolInvocation`:

```java
ToolDefinition reportPhase = ToolDefinition.fromWithToolInvocation(
    "report_phase",
    "Reports the current phase with invocation context",
    Param.of(String.class, "phase", "The current phase"),
    (phase, invocation) -> "phase=" + phase + ", toolCallId=" + invocation.getToolCallId()
);
```

For async with `ToolInvocation`, use `fromAsyncWithToolInvocation`.

### Fluent option modifiers

Chain fluent modifiers to set tool options:

- `.skipPermission(boolean)` — bypass permission prompts
- `.defer(ToolDefer)` — control deferred execution (`AUTO`, `NEVER`)
- `.overridesBuiltInTool(boolean)` — shadow built-in tools

For design context and decision rationale, see [ADR-006](docs/adr/adr-006-tool-definition-inline.md).

## Session Store

`enableSessionStore` on `SessionConfig` enables the cross-session store for search and retrieval across sessions. When unset in the default `CopilotClientMode.COPILOT_CLI` mode, the runtime default applies (enabled). In `CopilotClientMode.EMPTY` mode, defaults to disabled.

## Memory

Sessions can opt into persistent memory, allowing the agent to read and write memory across turns. Memory is configured per session and applies to both `createSession` and `resumeSession`.
For more background, see [About GitHub Copilot Memory](https://docs.github.com/en/copilot/concepts/agents/copilot-memory).

```java
import com.github.copilot.rpc.MemoryConfiguration;
import com.github.copilot.rpc.PermissionHandler;
import com.github.copilot.rpc.ResumeSessionConfig;
import com.github.copilot.rpc.SessionConfig;

// Enable memory for a new session
var session = client.createSession(new SessionConfig()
    .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
    .setModel("gpt-5")
    .setMemory(new MemoryConfiguration().setEnabled(true))
).get();

// Disable memory for a new session
var sessionNoMemory = client.createSession(new SessionConfig()
    .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
    .setModel("gpt-5")
    .setMemory(new MemoryConfiguration().setEnabled(false))
).get();

// Configure memory while resuming
var resumed = client.resumeSession(sessionId, new ResumeSessionConfig()
    .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
    .setMemory(new MemoryConfiguration().setEnabled(true))
).get();
```

When `memory` is left unset, no memory configuration is sent and the runtime default applies. In the default `CopilotClientMode.COPILOT_CLI` the SDK leaves `memory` unset so the runtime applies its own default, while `CopilotClientMode.EMPTY` defaults `memory` to disabled unless you set it explicitly.

## Using experimental APIs

Some SDK APIs are marked as experimental with `@CopilotExperimental`. These APIs may change or be removed in future versions without notice.

By default, referencing an experimental API from your code causes a **compile-time error**:

```
error: Use of experimental API 'ExperimentalType' in field type is not allowed.
       Add @AllowCopilotExperimental or compiler option -Acopilot.experimental.allowed=true to opt in.
```

To opt in and use experimental APIs, either:

- annotate the consuming class, method, or constructor with `@AllowCopilotExperimental`, or
- pass the annotation processor option `-Acopilot.experimental.allowed=true` to the Java compiler.

### In code

```java
import com.github.copilot.AllowCopilotExperimental;
import test.ExperimentalType;

@AllowCopilotExperimental
public class Consumer {
    private ExperimentalType field;

    public ExperimentalType getIt() {
        return field;
    }

    @AllowCopilotExperimental
    public ExperimentalType echo(ExperimentalType value) {
        return value;
    }
}
```

### Maven

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <compilerArgs>
            <arg>-Acopilot.experimental.allowed=true</arg>
        </compilerArgs>
    </configuration>
</plugin>
```

### Gradle

```groovy
tasks.withType(JavaCompile) {
    options.compilerArgs += ['-Acopilot.experimental.allowed=true']
}
```

### What the processor catches

The processor detects usage of experimental types in **declarations**:

| Usage pattern | Caught? |
|---|---|
| Field declared with experimental type | ✅ |
| Method parameter of experimental type | ✅ |
| Method return type is experimental | ✅ |
| `extends` / `implements` experimental type | ✅ |
| `throws` an experimental exception type | ✅ |
| Generic type argument is experimental (e.g., `List<ExperimentalType>`) | ✅ |

### Known limitations

The processor uses standard JSR 269 annotation processing APIs for maximum portability (works with javac, ECJ/Eclipse, and any compliant compiler). This means it inspects **declarations only**, not expressions inside method bodies. The following patterns are **not caught** by the processor:

| Usage pattern | Caught? | Workaround |
|---|---|---|
| `new ExperimentalType()` in a method body (no field/param declaration) | ❌ | Use the compiler flag for a whole-compilation opt-in |
| `ExperimentalType.staticMethod()` inline call | ❌ | Use the compiler flag for a whole-compilation opt-in |
| Method reference `ExperimentalType::method` | ❌ | Use the compiler flag for a whole-compilation opt-in |
| Local variable with experimental type (including `var` inference) | ❌ | Move the usage into a declaration the processor can see, or use the compiler flag |
| Cast to experimental type | ❌ | Use the compiler flag for a whole-compilation opt-in |

In practice, these gaps rarely matter: any meaningful use of an experimental SDK type almost always appears in a field declaration, method signature, or type hierarchy — all of which are caught. A purely inline expression with no declaration footprint (e.g., `session.rpc().experimental.foo().join()`) is the only case that would slip through. See [ADR-004](docs/adr/adr-004-copilotexperimental.md) for the design rationale.

### Example

```java
import com.github.copilot.CopilotExperimental;

// This type is experimental — consumer code that references it
// in declarations will fail to compile unless the opt-in flag is provided.
@CopilotExperimental
public class ExperimentalType {
    public void doSomething() {}
}

// Consumer code — compiles only with -Acopilot.experimental.allowed=true
import test.ExperimentalType;

public class Consumer {
    private ExperimentalType field;                      // ← caught: field type
    public ExperimentalType getIt() { return field; }   // ← caught: return type
    public void setIt(ExperimentalType v) { }           // ← caught: parameter type
}
```

The gate also applies to individual methods annotated with `@CopilotExperimental` on otherwise stable types. When a type-level annotation is present, all member accesses through that type are considered experimental. `@AllowCopilotExperimental` mirrors the same declaration-level boundary: annotating a class opts in that class and its enclosed declarations, while annotating a method or constructor opts in just that executable signature.

## Projects Using This SDK

| Project                                                                       | Description                                |
| ----------------------------------------------------------------------------- | ------------------------------------------ |
| [JMeter Copilot Plugin](https://github.com/brunoborges/jmeter-copilot-plugin) | JMeter plugin for AI-assisted load testing |

> Want to add your project? Open a PR!

### Development Setup

Requires JDK 25 or later and a supported [Node.js version](../nodejs/README.md#prerequisites) for development. The following steps validate the artifact built with JDK 25 runs on both 25 and 17, preserving the MR-JAR behavior.

```bash
# Clone the repository
git clone https://github.com/github/copilot-sdk.git
cd copilot-sdk/java

# Enable git hooks for code formatting
git config core.hooksPath .githooks

# Build and test with JDK 25
mvn test-compile jar:jar
mvn verify -Dskip.test.harness=true

# Set your paths for JDK 17
# Run the JDK 25 built jar with JDK 17 JVM for tests. Do not re-compile the jar.
mvn jacoco:prepare-agent@wire-up-coverage-instrumentation antrun:run@print-test-jdk-banner surefire:test failsafe:integration-test failsafe:verify jacoco:report@build-coverage-report-from-tests -Denforcer.skip=true
```

#### Development Setup for native embedding

Run native-runtime Maven commands from the `java` directory. Native packaging requires Node.js and npm in addition to JDK 25 and Maven because `copilot-native/scripts/fetch-native.mjs` retrieves the pinned npm runtime package.

On a native Linux x64 glibc host, Maven activates the `native-linux-x64` profile when `copilot.native.libc=glibc` is set. On Windows x64, Maven activates `native-win32-x64` automatically. The matching profile validates the host, runs the native script tests, fetches the pinned `@github/copilot-<classifier>` package during `generate-resources`, packages the classifier JAR during `package`, and verifies its native contents. Ensure npm can authenticate to the package registry before running the build.

Before opting in, validate that Node.js reports glibc for the build host:

```bash
node copilot-native/scripts/validate-native-host.mjs linux-x64
mvn -pl copilot-native clean verify -Dcopilot.native.libc=glibc
```

The `inprocess` test profile performs the same validation and native packaging automatically, so the full in-process test command remains:

```bash
mvn -Pinprocess clean verify
```

On Windows PowerShell, initialize Java and run the same profile:

```powershell
mvn -Pinprocess clean verify
```

On macOS, Linux ARM64, Linux x64 musl, and other unsupported hosts, do not set `copilot.native.libc=glibc`. A normal build produces only the OS-neutral primary, sources, and Javadoc JARs; it does not run native script tests, download or stage native files, or produce a platform classifier JAR.

To build only the OS-neutral artifacts on any host, or override the glibc opt-in, disable native download and packaging:

```bash
mvn -pl copilot-native clean package -DskipTests -Dcopilot.native.libc=glibc -Dcopilot.native.skip.download=true
```

The verified Linux x64 checks are:

```bash
node --test copilot-native/scripts/fetch-native.test.mjs copilot-native/scripts/validate-native-host.test.mjs
mvn -pl copilot-native help:active-profiles -Dcopilot.native.libc=glibc -Dcopilot.native.skip.download=false
mvn -pl copilot-native test -Dcopilot.native.libc=glibc
mvn clean verify -Dcopilot.native.libc=glibc
mvn clean package -pl copilot-native -DskipTests -Dcopilot.native.libc=glibc -Dcopilot.native.skip.download=true
```

On Linux x64, the classifier JAR contains `native/linux-x64/runtime.node`, `native/linux-x64/platform.properties`, and `native/linux-x64/copilot`. On Windows x64, it contains `native/win32-x64/runtime.node`, `native/win32-x64/platform.properties`, and `native/win32-x64/copilot.exe`. The placeholder JAR remains OS-neutral and contains no native binaries. Unsupported hosts retain the placeholder-only behavior.

## License

MIT — see [LICENSE](sdk/LICENSE) for details.
