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

> ℹ️ **Public Preview:** This SDK tracks the [GitHub Copilot SDKs](https://github.com/github/copilot-sdk) for [.NET](https://github.com/github/copilot-sdk/tree/main/dotnet) and [Node.js](https://github.com/github/copilot-sdk/tree/main/nodejs). While in public preview, minor breaking changes may still occur between releases.

Java SDK for programmatic control of GitHub Copilot CLI, enabling you to build AI-powered applications and agentic workflows.

## Installation

### Runtime requirements

- Java 17 or later. **JDK 25 recommended**. The distributed jar is a multi-release jar (MR-JAR) and is compiled on JDK 25 with `maven.compiler.release` set to 17. This means, when run on JDK 25 and later, the SDK automatically uses virtual threads for its default internal executor.
- GitHub Copilot CLI 1.0.55-5. or later installed and in `PATH` (or provide custom `cliPath`)

### Maven

```xml
<dependency>
    <groupId>com.github</groupId>
    <artifactId>copilot-sdk-java</artifactId>
    <version>1.0.0-beta-10-java.1</version>
</dependency>
```

### Gradle

```groovy
implementation 'com.github:copilot-sdk-java:1.0.0-beta-10-java.1'
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
    <version>1.0.0-beta-10-java.2-SNAPSHOT</version>
</dependency>
```

### Gradle

```groovy
implementation 'com.github:copilot-sdk-java:1.0.0-beta-10-java.1-SNAPSHOT'
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

## Projects Using This SDK

| Project                                                                       | Description                                |
| ----------------------------------------------------------------------------- | ------------------------------------------ |
| [JMeter Copilot Plugin](https://github.com/brunoborges/jmeter-copilot-plugin) | JMeter plugin for AI-assisted load testing |

> Want to add your project? Open a PR!

## CI/CD Workflows

This project uses several GitHub Actions workflows for building, testing, releasing, and syncing with the reference implementation SDK.

See [WORKFLOWS.md](docs/WORKFLOWS.md) for a full overview and details on each workflow.

## Contributing

Contributions are welcome! Please see the [Contributing Guide](CONTRIBUTING.md) for details.

### Agentic Reference Implementation Merge and Sync

This SDK tracks the official [Copilot SDK](https://github.com/github/copilot-sdk) (.NET reference implementation) and ports changes to Java. The reference implementation merge process is automated with AI assistance:

**Automated sync** — A [scheduled GitHub Actions workflow](.github/workflows/reference-impl-sync.yml) runs on the schedule specified in that file. It checks for new reference implementation commits since the last merge (tracked in [`.lastmerge`](.lastmerge)), and if changes are found, creates an issue labeled `reference-impl-sync` and assigns it to the GitHub Copilot coding agent. Any previously open `reference-impl-sync` issues are automatically closed. The sync also updates the `@github/copilot` version in both `pom.xml` and `scripts/codegen/package.json` to keep schemas and test CLI in lockstep.

**Reusable prompt** — The merge workflow is defined in [`agentic-merge-reference-impl.prompt.md`](.github/prompts/agentic-merge-reference-impl.prompt.md). It can be triggered manually from:

- **VS Code Copilot Chat** — type `/agentic-merge-reference-impl`
- **GitHub Copilot CLI** — use `copilot` CLI with the same skill reference

### Development Setup

Requires JDK 25 or later for development.

```bash
# Clone the repository
git clone https://github.com/github/copilot-sdk.git
cd copilot-sdk/java

# Enable git hooks for code formatting
git config core.hooksPath .githooks

# Build and test with JDK 25
mvn clean verify

# Set your paths for JDK 17
# Run the JDK 25 built jar with JDK 17 JVM for tests. Do not re-compile the jar.
mvn jacoco:prepare-agent@wire-up-coverage-instrumentation antrun:run@print-test-jdk-banner surefire:test failsafe:integration-test failsafe:verify jacoco:report@build-coverage-report-from-tests -Denforcer.skip=true
```

The tests require the official [copilot-sdk](https://github.com/github/copilot-sdk) test harness, which is automatically cloned during build.

## Support

See [SUPPORT.md](SUPPORT.md) for how to file issues and get help.

## Code of Conduct

This project has adopted the [Contributor Covenant Code of Conduct](CODE_OF_CONDUCT.md). See [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) for details.

## Security

See [SECURITY.md](SECURITY.md) for reporting security vulnerabilities.

## License

MIT — see [LICENSE](LICENSE) for details.

