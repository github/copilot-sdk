# GitHub Copilot SDK for Java

Java SDK for programmatic control of GitHub Copilot CLI via JSON-RPC.

> **📦 The Java SDK is maintained in a separate repository: [`github/copilot-sdk-java`](https://github.com/github/copilot-sdk-java)**
>
> **Note:** This SDK is in technical preview and may change in breaking ways.

[![Build](https://github.com/github/copilot-sdk-java/actions/workflows/build-test.yml/badge.svg)](https://github.com/github/copilot-sdk-java/actions/workflows/build-test.yml)
[![Maven Central](https://img.shields.io/maven-central/v/com.github/copilot-sdk-java)](https://central.sonatype.com/artifact/com.github/copilot-sdk-java)
[![Java 17+](https://img.shields.io/badge/Java-17%2B-blue?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Documentation](https://img.shields.io/badge/docs-online-brightgreen)](https://github.github.io/copilot-sdk-java/)
[![Javadoc](https://javadoc.io/badge2/com.github/copilot-sdk-java/javadoc.svg)](https://javadoc.io/doc/com.github/copilot-sdk-java/latest/index.html)

## Quick Start

```java
import com.github.copilot.sdk.CopilotClient;
import com.github.copilot.sdk.events.AssistantMessageEvent;
import com.github.copilot.sdk.events.SessionIdleEvent;
import com.github.copilot.sdk.json.MessageOptions;
import com.github.copilot.sdk.json.PermissionHandler;
import com.github.copilot.sdk.json.SessionConfig;

public class QuickStart {
    public static void main(String[] args) throws Exception {
        // Create and start client
        try (var client = new CopilotClient()) {
            client.start().get();

            // Create a session (onPermissionRequest is required)
            var session = client.createSession(
                new SessionConfig()
                    .setModel("gpt-5")
                    .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
            ).get();

            var done = new java.util.concurrent.CompletableFuture<Void>();

            // Handle events
            session.on(AssistantMessageEvent.class, msg ->
                System.out.println(msg.getData().content()));
            session.on(SessionIdleEvent.class, idle ->
                done.complete(null));

            // Send a message and wait for completion
            session.send(new MessageOptions().setPrompt("What is 2+2?"));
            done.get();
        }
    }
}
```

## Try it with JBang

Run the SDK without setting up a full project using [JBang](https://www.jbang.dev/):

```bash
jbang https://github.com/github/copilot-sdk-java/blob/main/jbang-example.java
```

## Documentation & Resources

| Resource | Link |
|----------|------|
| **Full Documentation** | [github.github.io/copilot-sdk-java](https://github.github.io/copilot-sdk-java/) |
| **Getting Started Guide** | [Documentation](https://github.github.io/copilot-sdk-java/latest/documentation.html) |
| **API Reference (Javadoc)** | [javadoc.io](https://javadoc.io/doc/com.github/copilot-sdk-java/latest/index.html) |
| **MCP Servers Integration** | [MCP Guide](https://github.github.io/copilot-sdk-java/latest/mcp.html) |
| **Cookbook** | [Recipes](https://github.com/github/copilot-sdk-java/tree/main/src/site/markdown/cookbook) |
| **Source Code** | [github/copilot-sdk-java](https://github.com/github/copilot-sdk-java) |
| **Issues & Feature Requests** | [GitHub Issues](https://github.com/github/copilot-sdk-java/issues) |
| **Releases** | [GitHub Releases](https://github.com/github/copilot-sdk-java/releases) |
| **Copilot Instructions** | [copilot-sdk-java.instructions.md](https://github.com/github/copilot-sdk-java/blob/main/instructions/copilot-sdk-java.instructions.md) |

## Contributing

Contributions are welcome! Please see the [Contributing Guide](https://github.com/github/copilot-sdk-java/blob/main/CONTRIBUTING.md) in the GitHub Copilot SDK for Java repository.

## License

MIT — see [LICENSE](https://github.com/github/copilot-sdk-java/blob/main/LICENSE) for details.
