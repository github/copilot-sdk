# Error Handling Patterns

Handle errors gracefully in your Copilot SDK applications.

## Prerequisites

Install [JBang](https://www.jbang.dev/) to run these examples:

```bash
# macOS (using Homebrew)
brew install jbangdev/tap/jbang

# Linux/macOS (using curl)
curl -Ls https://sh.jbang.dev | bash -s - app setup

# Windows (using Scoop)
scoop install jbang
```

## Example scenario

You need to handle various error conditions like connection failures, timeouts, and invalid responses.

## Basic error handling

**Usage:**
```bash
jbang BasicErrorHandling.java
```

**Code:**
```java
//DEPS com.github:copilot-sdk-java:1.0.0-beta-java.4
import com.github.copilot.sdk.CopilotClient;
import com.github.copilot.sdk.generated.AssistantMessageEvent;
import com.github.copilot.sdk.json.MessageOptions;
import com.github.copilot.sdk.json.PermissionHandler;
import com.github.copilot.sdk.json.SessionConfig;

public class BasicErrorHandling {
    public static void main(String[] args) {
        try (var client = new CopilotClient()) {
            client.start().get();

            var session = client.createSession(
                new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL).setModel("gpt-5")).get();

            session.on(AssistantMessageEvent.class, msg -> {
                System.out.println(msg.getData().content());
            });

            session.sendAndWait(new MessageOptions()
                .setPrompt("Hello!")).get();

            session.close();
        } catch (Exception ex) {
            System.err.println("Error: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}
```

## Handling specific error types

```java
//DEPS com.github:copilot-sdk-java:1.0.0-beta-java.4
import com.github.copilot.sdk.CopilotClient;
import java.util.concurrent.ExecutionException;

public class SpecificErrorHandling {
    public static void startClient() {
        try (var client = new CopilotClient()) {
            client.start().get();
            // ... use client ...
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof java.io.IOException) {
                System.err.println("Copilot CLI not found. Please install it first.");
                System.err.println("Details: " + cause.getMessage());
            } else if (cause instanceof java.util.concurrent.TimeoutException) {
                System.err.println("Could not connect to Copilot CLI server.");
                System.err.println("Details: " + cause.getMessage());
            } else {
                System.err.println("Unexpected error: " + cause.getMessage());
                cause.printStackTrace();
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            System.err.println("Operation interrupted: " + ex.getMessage());
        } catch (Exception ex) {
            System.err.println("Unexpected error: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}
```

## Timeout handling

```java
//DEPS com.github:copilot-sdk-java:1.0.0-beta-java.4
import com.github.copilot.sdk.CopilotSession;
import com.github.copilot.sdk.generated.AssistantMessageEvent;
import com.github.copilot.sdk.json.MessageOptions;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class TimeoutHandling {
    public static void sendWithTimeout(CopilotSession session) {
        try {
            session.on(AssistantMessageEvent.class, msg -> {
                System.out.println(msg.getData().content());
            });

            // Wait up to 30 seconds for response
            session.sendAndWait(new MessageOptions()
                .setPrompt("Complex question..."))
                .get(30, TimeUnit.SECONDS);

        } catch (TimeoutException ex) {
            System.err.println("Request timed out");
        } catch (Exception ex) {
            System.err.println("Error: " + ex.getMessage());
        }
    }
}
```

## Aborting a request

```java
//DEPS com.github:copilot-sdk-java:1.0.0-beta-java.4
import com.github.copilot.sdk.CopilotSession;
import com.github.copilot.sdk.json.MessageOptions;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AbortRequest {
    public static void abortAfterDelay(CopilotSession session) {
        // Start a request (non-blocking)
        session.send(new MessageOptions()
            .setPrompt("Write a very long story..."));

        // Schedule abort after 5 seconds
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.schedule(() -> {
            try {
                session.abort().get();
                System.out.println("Request aborted");
            } catch (Exception ex) {
                System.err.println("Failed to abort: " + ex.getMessage());
            } finally {
                scheduler.shutdown();
            }
        }, 5, TimeUnit.SECONDS);
    }
}
```

## Graceful shutdown

```java
//DEPS com.github:copilot-sdk-java:1.0.0-beta-java.4
import com.github.copilot.sdk.CopilotClient;

public class GracefulShutdown {
    public static void main(String[] args) {
        var client = new CopilotClient();

        // Set up shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nShutting down...");
            try {
                client.close();
            } catch (Exception ex) {
                System.err.println("Error during shutdown: " + ex.getMessage());
            }
        }));

        try {
            client.start().get();
            // ... do work ...
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
```

## Try-with-resources pattern

```java
//DEPS com.github:copilot-sdk-java:1.0.0-beta-java.4
import com.github.copilot.sdk.CopilotClient;
import com.github.copilot.sdk.generated.AssistantMessageEvent;
import com.github.copilot.sdk.json.MessageOptions;
import com.github.copilot.sdk.json.PermissionHandler;
import com.github.copilot.sdk.json.SessionConfig;

public class TryWithResources {
    public static void doWork() throws Exception {
        try (var client = new CopilotClient()) {
            client.start().get();

            try (var session = client.createSession(
                    new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL).setModel("gpt-5")).get()) {

                session.on(AssistantMessageEvent.class, msg -> {
                    System.out.println(msg.getData().content());
                });

                session.sendAndWait(new MessageOptions()
                    .setPrompt("Hello!")).get();

                // Session and client are automatically closed
            }
        }
    }
}
```

## Handling tool errors

```java
//DEPS com.github:copilot-sdk-java:1.0.0-beta-java.4
import com.github.copilot.sdk.CopilotClient;
import com.github.copilot.sdk.generated.AssistantMessageEvent;
import com.github.copilot.sdk.json.MessageOptions;
import com.github.copilot.sdk.json.PermissionHandler;
import com.github.copilot.sdk.json.SessionConfig;
import com.github.copilot.sdk.json.ToolDefinition;
import com.github.copilot.sdk.json.ToolResultObject;
import java.util.Map;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ToolErrorHandling {
    public static void handleToolErrors() throws Exception {
        var errorTool = ToolDefinition.create(
            "get_user_location",
            "Gets the user's location",
            Map.of("type", "object", "properties", Map.of()),
            invocation -> {
                // Return an error result
                return CompletableFuture.completedFuture(
                    ToolResultObject.error("Location service unavailable")
                );
            }
        );

        try (var client = new CopilotClient()) {
            client.start().get();

            var session = client.createSession(
                new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL).setTools(List.of(errorTool))).get();

            session.on(AssistantMessageEvent.class, msg -> {
                System.out.println(msg.getData().content());
            });

            // Session continues even when tool fails
            session.sendAndWait(new MessageOptions()
                .setPrompt("What is my location? If you can't find out, just say 'unknown'."))
                .get();

            session.close();
        }
    }
}
```

## Best practices

1. **Always clean up**: Use try-with-resources to ensure `close()` is called
2. **Handle connection errors**: The CLI might not be installed or running
3. **Set appropriate timeouts**: Use `get(timeout, TimeUnit)` for long-running requests
4. **Log errors**: Capture error details for debugging
5. **Wrap operations**: Consider wrapping SDK operations in methods that handle common errors
6. **Check error causes**: Use `ExecutionException.getCause()` to get the actual error from `CompletableFuture`
