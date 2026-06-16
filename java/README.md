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
    <version>1.0.1</version>
</dependency>
```

### Gradle

```groovy
implementation 'com.github:copilot-sdk-java:1.0.1'
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
    <version>1.0.2-SNAPSHOT</version>
</dependency>
```

### Gradle

Replace `${copilot.sdk.version}` with the latest release from Maven Central.

```groovy
implementation 'com.github:copilot-sdk-java:1.0.2-SNAPSHOT'
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

## Try it with JBang

You can run the SDK without setting up a full Java project, by using [JBang](https://www.jbang.dev/).

See the full source of [`jbang-example.java`](jbang-example.java) for a complete example with more features like session idle handling and usage info events.

Or run it directly from the repository:

```bash
jbang https://github.com/github/copilot-sdk/blob/main/java/jbang-example.java
```

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

Requires JDK 25 or later for development. The following steps validate the artifact built with JDK 25 runs on both 25 and 17, preserving the MR-JAR behavior.

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

## License

MIT — see [LICENSE](LICENSE) for details.

