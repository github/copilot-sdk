!# Build Your First Copilot-Powered App

In this tutorial, you'll use the GitHub Copilot SDK for Java to build a command-line assistant. You'll start with the basics, add streaming responses, then add custom tools - giving Copilot the ability to call your code.

**What you'll build:**

```
You: What's the weather like in Seattle?
Copilot: Let me check the weather for Seattle...
         Currently 62°F and cloudy with a chance of rain.
         Typical Seattle weather!

You: How about Tokyo?
Copilot: In Tokyo it's 75°F and sunny. Great day to be outside!
```

## Prerequisites

Before you begin, make sure you have:

- **Java 17+** installed
- **GitHub Copilot CLI** installed and authenticated ([Installation guide](https://docs.github.com/en/copilot/how-tos/set-up/install-copilot-cli))

Verify the CLI is working:

```bash
copilot --version
```

## Step 1: Install the SDK

> If you want to test a _-SNAPSHOT_ version, you may have to clone the project's repository and install the library locally with _mvn install_.

### Apache Maven

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.github</groupId>
    <artifactId>copilot-sdk-java</artifactId>
    <version>${project.version}</version>
</dependency>
```

### Gradle

Add the dependency to your `build.gradle`:

```groovy
implementation 'com.github:copilot-sdk-java:${project.version}'
```

### JBang (Quick Start)

For the fastest way to try the SDK without setting up a project, use [JBang](https://www.jbang.dev/). Create a file named `HelloCopilot.java` with the following header, and then proceed to Step 2 by appending the proposed content into this same file.

```bash
//DEPS com.github:copilot-sdk-java:${project.version}
```

## Step 2: Send Your First Message

Create a new file and add the following code. This is the simplest way to use the SDK.

```java
import com.github.copilot.sdk.CopilotClient;
import com.github.copilot.sdk.json.MessageOptions;
import com.github.copilot.sdk.json.PermissionHandler;
import com.github.copilot.sdk.json.SessionConfig;

public class HelloCopilot {
    public static void main(String[] args) throws Exception {
        try (var client = new CopilotClient()) {
            client.start().get();
            
            var session = client.createSession(
                new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL).setModel("gpt-4.1")
            ).get();

            var response = session.sendAndWait(
                new MessageOptions().setPrompt("What is 2 + 2?")
            ).get();

            System.out.println(response.getData().content());
        }
    }
}
```

Run the code with your preferred build tool or IDE. 

Or run it directly with JBang:

```bash
jbang HelloCopilot.java
```

**You should see:**

```
2 + 2 = 4
```

Congratulations! You just built your first Copilot-powered app.

## Step 3: Add Streaming Responses

Right now, you wait for the complete response before seeing anything. Let's make it interactive by streaming the response as it's generated.

```java
import com.github.copilot.sdk.CopilotClient;
import com.github.copilot.sdk.generated.AssistantMessageDeltaEvent;
import com.github.copilot.sdk.generated.SessionIdleEvent;
import com.github.copilot.sdk.json.MessageOptions;
import com.github.copilot.sdk.json.PermissionHandler;
import com.github.copilot.sdk.json.SessionConfig;
import java.util.concurrent.CompletableFuture;

public class StreamingExample {
    public static void main(String[] args) throws Exception {
        try (var client = new CopilotClient()) {
            client.start().get();
            
            var session = client.createSession(
                new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                    .setModel("gpt-4.1")
                    .setStreaming(true)
            ).get();

            var done = new CompletableFuture<Void>();

            // Listen for response chunks
            session.on(AssistantMessageDeltaEvent.class, delta -> {
                System.out.print(delta.getData().deltaContent());
            });
            session.on(SessionIdleEvent.class, idle -> {
                System.out.println(); // New line when done
                done.complete(null);
            });

            session.send(
                new MessageOptions().setPrompt("Tell me a short joke")
            ).get();
            
            done.get(); // Wait for completion
        }
    }
}
```

Run the code again. You'll see the response appear word by word.

## Step 4: Add a Custom Tool

Now for the powerful part. Let's give Copilot the ability to call your code by defining a custom tool. We'll create a simple weather lookup tool.

```java
import com.github.copilot.sdk.CopilotClient;
import com.github.copilot.sdk.generated.AssistantMessageDeltaEvent;
import com.github.copilot.sdk.generated.SessionIdleEvent;
import com.github.copilot.sdk.json.MessageOptions;
import com.github.copilot.sdk.json.PermissionHandler;
import com.github.copilot.sdk.json.SessionConfig;
import com.github.copilot.sdk.json.ToolDefinition;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

public class ToolExample {
    public static void main(String[] args) throws Exception {
        try (var client = new CopilotClient()) {
            client.start().get();

            // Define a tool that Copilot can call
            var getWeather = ToolDefinition.create(
                "get_weather",
                "Get the current weather for a city",
                Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "city", Map.of(
                            "type", "string",
                            "description", "The city name"
                        )
                    ),
                    "required", List.of("city")
                ),
                invocation -> {
                    var params = invocation.getArguments();
                    var city = (String) params.get("city");
                    
                    // In a real app, you'd call a weather API here
                    String[] conditions = {"sunny", "cloudy", "rainy", "partly cloudy"};
                    int temp = new Random().nextInt(30) + 50;
                    var condition = conditions[new Random().nextInt(conditions.length)];
                    
                    return CompletableFuture.completedFuture(Map.of(
                        "city", city,
                        "temperature", temp + "°F",
                        "condition", condition
                    ));
                }
            );

            var session = client.createSession(
                new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                    .setModel("gpt-4.1")
                    .setStreaming(true)
                    .setTools(List.of(getWeather))
            ).get();

            var done = new CompletableFuture<Void>();

            session.on(AssistantMessageDeltaEvent.class, delta -> {
                System.out.print(delta.getData().deltaContent());
            });
            session.on(SessionIdleEvent.class, idle -> {
                System.out.println();
                done.complete(null);
            });

            session.send(
                new MessageOptions().setPrompt("What's the weather like in Seattle and Tokyo?")
            ).get();
            
            done.get();
        }
    }
}
```

Run it and you'll see Copilot call your tool to get weather data, then respond with the results!

## Step 5: Build an Interactive Assistant

Let's put it all together into a useful interactive assistant:

```java
import com.github.copilot.sdk.CopilotClient;
import com.github.copilot.sdk.generated.AssistantMessageDeltaEvent;
import com.github.copilot.sdk.generated.SessionIdleEvent;
import com.github.copilot.sdk.json.MessageOptions;
import com.github.copilot.sdk.json.PermissionHandler;
import com.github.copilot.sdk.json.SessionConfig;
import com.github.copilot.sdk.json.ToolDefinition;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

public class WeatherAssistant {
    public static void main(String[] args) throws Exception {
        try (var client = new CopilotClient();
             var scanner = new Scanner(System.in)) {
            
            client.start().get();

            var getWeather = ToolDefinition.create(
                "get_weather",
                "Get the current weather for a city",
                Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "city", Map.of(
                            "type", "string",
                            "description", "The city name"
                        )
                    ),
                    "required", List.of("city")
                ),
                invocation -> {
                    var params = invocation.getArguments();
                    var city = (String) params.get("city");
                    String[] conditions = {"sunny", "cloudy", "rainy", "partly cloudy"};
                    int temp = new Random().nextInt(30) + 50;
                    var condition = conditions[new Random().nextInt(conditions.length)];
                    return CompletableFuture.completedFuture(Map.of(
                        "city", city,
                        "temperature", temp + "°F",
                        "condition", condition
                    ));
                }
            );

            var session = client.createSession(
                new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                    .setModel("gpt-4.1")
                    .setStreaming(true)
                    .setTools(List.of(getWeather))
            ).get();

            System.out.println("🌤️  Weather Assistant (type 'exit' to quit)");
            System.out.println("   Try: 'What's the weather in Paris?'\n");

            var done = new AtomicReference<CompletableFuture<Void>>();

            // Register listener once, outside the loop
            session.on(AssistantMessageDeltaEvent.class, delta -> {
                System.out.print(delta.getData().deltaContent());
            });
            session.on(SessionIdleEvent.class, idle -> {
                System.out.println();
                var f = done.get();
                if (f != null) f.complete(null);
            });

            while (true) {
                System.out.print("You: ");
                String input = scanner.nextLine();
                
                if ("exit".equalsIgnoreCase(input.trim())) {
                    break;
                }

                done.set(new CompletableFuture<>());

                System.out.print("Assistant: ");
                session.send(new MessageOptions().setPrompt(input)).get();
                done.get().get();
            }
        }
    }
}
```

## What's Next?

You've learned the core concepts of the Copilot SDK:

- ✅ Creating a client and session
- ✅ Sending messages and receiving responses
- ✅ Streaming for real-time output
- ✅ Custom tools for extending Copilot's capabilities
- ✅ Building interactive applications

**Explore more:**

- [Documentation](documentation.html) - Basic usage and session management
- [Advanced Usage](advanced.html) - Tools, BYOK, infinite sessions, and more
- [MCP Servers](mcp.html) - Connect to Model Context Protocol servers

## Troubleshooting

### "Copilot CLI not found"

Make sure the GitHub Copilot CLI is installed and in your PATH:

```bash
copilot --version
```

If not installed, follow the [GitHub Copilot CLI installation guide](https://docs.github.com/en/copilot/how-tos/set-up/install-copilot-cli).

### "Authentication failed"

The CLI needs to be authenticated with your GitHub account:

```bash
copilot auth login
```

### "Connection timeout"

Check your internet connection and firewall settings. The SDK communicates with GitHub's Copilot service.

---

## Next Steps

- 📖 **[Documentation](documentation.html)** - Core concepts, events, streaming, session management
- 📖 **[Advanced Usage](advanced.html)** - Tools, BYOK, MCP Servers, System Messages, Custom Agents
- 📖 **[Session Hooks](hooks.html)** - Intercept tool execution and session lifecycle events
- 📖 **[Setup & Deployment](setup.html)** - OAuth, backend services, scaling
- 📖 **[API Javadoc](apidocs/index.html)** - Complete API reference
