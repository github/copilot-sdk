# Build Your First Copilot-Powered App

In this tutorial, you'll use the Copilot SDK to build a command-line assistant. You'll start with the basics, add streaming responses, then add custom tools - giving Copilot the ability to call your code.

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

- **GitHub Copilot CLI** installed and authenticated ([Installation guide](https://docs.github.com/en/copilot/how-tos/set-up/install-copilot-cli))
- Your preferred language runtime:
  - **Node.js** 18+ or **Python** 3.8+ or **Go** 1.21+ or **.NET** 8.0+

Verify the CLI is working:

```bash
copilot --version
```

## Step 1: Install the SDK

<details open>
<summary><strong>Node.js / TypeScript</strong></summary>

First, create a new directory and initialize your project:

```bash
mkdir copilot-demo && cd copilot-demo
npm init -y --init-type module
```

Then install the SDK and TypeScript runner:

```bash
npm install @github/copilot-sdk tsx
```

</details>

<details>
<summary><strong>Python</strong></summary>

```bash
pip install github-copilot-sdk
```

</details>

<details>
<summary><strong>Go</strong></summary>

First, create a new directory and initialize your module:

```bash
mkdir copilot-demo && cd copilot-demo
go mod init copilot-demo
```

Then install the SDK:

```bash
go get github.com/github/copilot-sdk/go
```

</details>

<details>
<summary><strong>.NET</strong></summary>

First, create a new console project:

```bash
dotnet new console -n CopilotDemo && cd CopilotDemo
```

Then add the SDK:

```bash
dotnet add package GitHub.Copilot.SDK
```

</details>

## Step 2: Send Your First Message

Create a new file and add the following code. This is the simplest way to use the SDK—about 5 lines of code.

<details open>
<summary><strong>Node.js / TypeScript</strong></summary>

Create `index.ts`:

```typescript
import { CopilotClient } from "@github/copilot-sdk";

const client = new CopilotClient();
const session = await client.createSession({ model: "gpt-4.1" });

const response = await session.sendAndWait({ prompt: "What is 2 + 2?" });
console.log(response?.data.content);

await client.stop();
process.exit(0);
```

Run it:

```bash
npx tsx index.ts
```

</details>

<details>
<summary><strong>Python</strong></summary>

Create `main.py`:

```python
import asyncio
from copilot import CopilotClient

async def main():
    client = CopilotClient()
    await client.start()

    session = await client.create_session({"model": "gpt-4.1"})
    response = await session.send_and_wait({"prompt": "What is 2 + 2?"})

    print(response.data.content)

    await client.stop()

asyncio.run(main())
```

Run it:

```bash
python main.py
```

</details>

<details>
<summary><strong>Go</strong></summary>

Create `main.go`:

```go
package main

import (
	"fmt"
	"log"
	"os"

	copilot "github.com/github/copilot-sdk/go"
)

func main() {
	client := copilot.NewClient(nil)
	if err := client.Start(); err != nil {
		log.Fatal(err)
	}
	defer client.Stop()

	session, err := client.CreateSession(&copilot.SessionConfig{Model: "gpt-4.1"})
	if err != nil {
		log.Fatal(err)
	}

	response, err := session.SendAndWait(copilot.MessageOptions{Prompt: "What is 2 + 2?"}, 0)
	if err != nil {
		log.Fatal(err)
	}

	fmt.Println(*response.Data.Content)
	os.Exit(0)
}
```

Run it:

```bash
go run main.go
```

</details>

<details>
<summary><strong>.NET</strong></summary>

Create a new console project and add this to `Program.cs`:

```csharp
using GitHub.Copilot.SDK;

await using var client = new CopilotClient();
await using var session = await client.CreateSessionAsync(new SessionConfig { Model = "gpt-4.1" });

var response = await session.SendAndWaitAsync(new MessageOptions { Prompt = "What is 2 + 2?" });
Console.WriteLine(response?.Data.Content);
```

Run it:

```bash
dotnet run
```

</details>

**You should see:**

```
4
```

Congratulations! You just built your first Copilot-powered app.

## Step 3: Add Streaming Responses

Right now, you wait for the complete response before seeing anything. Let's make it interactive by streaming the response as it's generated.

<details open>
<summary><strong>Node.js / TypeScript</strong></summary>

Update `index.ts`:

```typescript
import { CopilotClient } from "@github/copilot-sdk";

const client = new CopilotClient();
const session = await client.createSession({
    model: "gpt-4.1",
    streaming: true,
});

// Listen for response chunks
session.on("assistant.message_delta", (event) => {
    process.stdout.write(event.data.deltaContent);
});
session.on("session.idle", () => {
    console.log(); // New line when done
});

await session.sendAndWait({ prompt: "Tell me a short joke" });

await client.stop();
process.exit(0);
```

</details>

<details>
<summary><strong>Python</strong></summary>

Update `main.py`:

```python
import asyncio
import sys
from copilot import CopilotClient
from copilot.generated.session_events import SessionEventType

async def main():
    client = CopilotClient()
    await client.start()

    session = await client.create_session({
        "model": "gpt-4.1",
        "streaming": True,
    })

    # Listen for response chunks
    def handle_event(event):
        if event.type == SessionEventType.ASSISTANT_MESSAGE_DELTA:
            sys.stdout.write(event.data.delta_content)
            sys.stdout.flush()
        if event.type == SessionEventType.SESSION_IDLE:
            print()  # New line when done

    session.on(handle_event)

    await session.send_and_wait({"prompt": "Tell me a short joke"})

    await client.stop()

asyncio.run(main())
```

</details>

<details>
<summary><strong>Go</strong></summary>

Update `main.go`:

```go
package main

import (
	"fmt"
	"log"
	"os"

	copilot "github.com/github/copilot-sdk/go"
)

func main() {
	client := copilot.NewClient(nil)
	if err := client.Start(); err != nil {
		log.Fatal(err)
	}
	defer client.Stop()

	session, err := client.CreateSession(&copilot.SessionConfig{
		Model:     "gpt-4.1",
		Streaming: true,
	})
	if err != nil {
		log.Fatal(err)
	}

	// Listen for response chunks
	session.On(func(event copilot.SessionEvent) {
		if event.Type == "assistant.message_delta" {
			fmt.Print(*event.Data.DeltaContent)
		}
		if event.Type == "session.idle" {
			fmt.Println()
		}
	})

	_, err = session.SendAndWait(copilot.MessageOptions{Prompt: "Tell me a short joke"}, 0)
	if err != nil {
		log.Fatal(err)
	}
	os.Exit(0)
}
```

</details>

<details>
<summary><strong>.NET</strong></summary>

Update `Program.cs`:

```csharp
using GitHub.Copilot.SDK;

await using var client = new CopilotClient();
await using var session = await client.CreateSessionAsync(new SessionConfig
{
    Model = "gpt-4.1",
    Streaming = true,
});

// Listen for response chunks
session.On(ev =>
{
    if (ev is AssistantMessageDeltaEvent deltaEvent)
    {
        Console.Write(deltaEvent.Data.DeltaContent);
    }
    if (ev is SessionIdleEvent)
    {
        Console.WriteLine();
    }
});

await session.SendAndWaitAsync(new MessageOptions { Prompt = "Tell me a short joke" });
```

</details>

Run the code again. You'll see the response appear word by word.

## Step 4: Add a Custom Tool

Now for the powerful part. Let's give Copilot the ability to call your code by defining a custom tool. We'll create a simple weather lookup tool.

<details open>
<summary><strong>Node.js / TypeScript</strong></summary>

Update `index.ts`:

```typescript
import { CopilotClient, defineTool } from "@github/copilot-sdk";

// Define a tool that Copilot can call
const getWeather = defineTool("get_weather", {
    description: "Get the current weather for a city",
    parameters: {
        type: "object",
        properties: {
            city: { type: "string", description: "The city name" },
        },
        required: ["city"],
    },
    handler: async (args: { city: string }) => {
        const { city } = args;
        // In a real app, you'd call a weather API here
        const conditions = ["sunny", "cloudy", "rainy", "partly cloudy"];
        const temp = Math.floor(Math.random() * 30) + 50;
        const condition = conditions[Math.floor(Math.random() * conditions.length)];
        return { city, temperature: `${temp}°F`, condition };
    },
});

const client = new CopilotClient();
const session = await client.createSession({
    model: "gpt-4.1",
    streaming: true,
    tools: [getWeather],
});

session.on("assistant.message_delta", (event) => {
    process.stdout.write(event.data.deltaContent);
});

await session.sendAndWait({
    prompt: "What's the weather like in Seattle and Tokyo?",
});

await client.stop();
process.exit(0);
```

</details>

<details>
<summary><strong>Python</strong></summary>

Update `main.py`:

```python
import asyncio
import random
import sys
from copilot import CopilotClient
from copilot.tools import define_tool
from copilot.generated.session_events import SessionEventType
from pydantic import BaseModel, Field

# Define the parameters for the tool using Pydantic
class GetWeatherParams(BaseModel):
    city: str = Field(description="The name of the city to get weather for")

# Define a tool that Copilot can call
@define_tool(description="Get the current weather for a city")
async def get_weather(params: GetWeatherParams) -> dict:
    city = params.city
    # In a real app, you'd call a weather API here
    conditions = ["sunny", "cloudy", "rainy", "partly cloudy"]
    temp = random.randint(50, 80)
    condition = random.choice(conditions)
    return {"city": city, "temperature": f"{temp}°F", "condition": condition}

async def main():
    client = CopilotClient()
    await client.start()

    session = await client.create_session({
        "model": "gpt-4.1",
        "streaming": True,
        "tools": [get_weather],
    })

    def handle_event(event):
        if event.type == SessionEventType.ASSISTANT_MESSAGE_DELTA:
            sys.stdout.write(event.data.delta_content)
            sys.stdout.flush()
        if event.type == SessionEventType.SESSION_IDLE:
            print()

    session.on(handle_event)

    await session.send_and_wait({
        "prompt": "What's the weather like in Seattle and Tokyo?"
    })

    await client.stop()

asyncio.run(main())
```

</details>

<details>
<summary><strong>Go</strong></summary>

Update `main.go`:

```go
package main

import (
	"fmt"
	"log"
	"math/rand"
	"os"

	copilot "github.com/github/copilot-sdk/go"
)

// Define the parameter type
type WeatherParams struct {
	City string `json:"city" jsonschema:"The city name"`
}

// Define the return type
type WeatherResult struct {
	City        string `json:"city"`
	Temperature string `json:"temperature"`
	Condition   string `json:"condition"`
}

func main() {
	// Define a tool that Copilot can call
	getWeather := copilot.DefineTool(
		"get_weather",
		"Get the current weather for a city",
		func(params WeatherParams, inv copilot.ToolInvocation) (WeatherResult, error) {
			// In a real app, you'd call a weather API here
			conditions := []string{"sunny", "cloudy", "rainy", "partly cloudy"}
			temp := rand.Intn(30) + 50
			condition := conditions[rand.Intn(len(conditions))]
			return WeatherResult{
				City:        params.City,
				Temperature: fmt.Sprintf("%d°F", temp),
				Condition:   condition,
			}, nil
		},
	)

	client := copilot.NewClient(nil)
	if err := client.Start(); err != nil {
		log.Fatal(err)
	}
	defer client.Stop()

	session, err := client.CreateSession(&copilot.SessionConfig{
		Model:     "gpt-4.1",
		Streaming: true,
		Tools:     []copilot.Tool{getWeather},
	})
	if err != nil {
		log.Fatal(err)
	}

	session.On(func(event copilot.SessionEvent) {
		if event.Type == "assistant.message_delta" {
			fmt.Print(*event.Data.DeltaContent)
		}
		if event.Type == "session.idle" {
			fmt.Println()
		}
	})

	_, err = session.SendAndWait(copilot.MessageOptions{
		Prompt: "What's the weather like in Seattle and Tokyo?",
	}, 0)
	if err != nil {
		log.Fatal(err)
	}
	os.Exit(0)
}
```

</details>

<details>
<summary><strong>.NET</strong></summary>

Update `Program.cs`:

```csharp
using GitHub.Copilot.SDK;
using Microsoft.Extensions.AI;
using System.ComponentModel;

await using var client = new CopilotClient();

// Define a tool that Copilot can call
var getWeather = AIFunctionFactory.Create(
    ([Description("The city name")] string city) =>
    {
        // In a real app, you'd call a weather API here
        var conditions = new[] { "sunny", "cloudy", "rainy", "partly cloudy" };
        var temp = Random.Shared.Next(50, 80);
        var condition = conditions[Random.Shared.Next(conditions.Length)];
        return new { city, temperature = $"{temp}°F", condition };
    },
    "get_weather",
    "Get the current weather for a city"
);

await using var session = await client.CreateSessionAsync(new SessionConfig
{
    Model = "gpt-4.1",
    Streaming = true,
    Tools = [getWeather],
});

session.On(ev =>
{
    if (ev is AssistantMessageDeltaEvent deltaEvent)
    {
        Console.Write(deltaEvent.Data.DeltaContent);
    }
    if (ev is SessionIdleEvent)
    {
        Console.WriteLine();
    }
});

await session.SendAndWaitAsync(new MessageOptions
{
    Prompt = "What's the weather like in Seattle and Tokyo?",
});
```

</details>

Run it and you'll see Copilot call your tool to get weather data, then respond with the results!

## Step 5: Build an Interactive Assistant

Let's put it all together into a useful interactive assistant:

<details open>
<summary><strong>Node.js / TypeScript</strong></summary>

```typescript
import { CopilotClient, defineTool } from "@github/copilot-sdk";
import * as readline from "readline";

const getWeather = defineTool("get_weather", {
    description: "Get the current weather for a city",
    parameters: {
        type: "object",
        properties: {
            city: { type: "string", description: "The city name" },
        },
        required: ["city"],
    },
    handler: async ({ city }) => {
        const conditions = ["sunny", "cloudy", "rainy", "partly cloudy"];
        const temp = Math.floor(Math.random() * 30) + 50;
        const condition = conditions[Math.floor(Math.random() * conditions.length)];
        return { city, temperature: `${temp}°F`, condition };
    },
});

const client = new CopilotClient();
const session = await client.createSession({
    model: "gpt-4.1",
    streaming: true,
    tools: [getWeather],
});

session.on("assistant.message_delta", (event) => {
    process.stdout.write(event.data.deltaContent);
});

const rl = readline.createInterface({
    input: process.stdin,
    output: process.stdout,
});

console.log("🌤️  Weather Assistant (type 'exit' to quit)");
console.log("   Try: 'What's the weather in Paris?'\n");

const prompt = () => {
    rl.question("You: ", async (input) => {
        if (input.toLowerCase() === "exit") {
            await client.stop();
            rl.close();
            return;
        }

        process.stdout.write("Assistant: ");
        await session.sendAndWait({ prompt: input });
        console.log("\n");
        prompt();
    });
};

prompt();
```

Run with:

```bash
npx tsx weather-assistant.ts
```

</details>

<details>
<summary><strong>Python</strong></summary>

Create `weather_assistant.py`:

```python
import asyncio
import random
import sys
from copilot import CopilotClient
from copilot.tools import define_tool
from copilot.generated.session_events import SessionEventType
from pydantic import BaseModel, Field

class GetWeatherParams(BaseModel):
    city: str = Field(description="The name of the city to get weather for")

@define_tool(description="Get the current weather for a city")
async def get_weather(params: GetWeatherParams) -> dict:
    city = params.city
    conditions = ["sunny", "cloudy", "rainy", "partly cloudy"]
    temp = random.randint(50, 80)
    condition = random.choice(conditions)
    return {"city": city, "temperature": f"{temp}°F", "condition": condition}

async def main():
    client = CopilotClient()
    await client.start()

    session = await client.create_session({
        "model": "gpt-4.1",
        "streaming": True,
        "tools": [get_weather],
    })

    def handle_event(event):
        if event.type == SessionEventType.ASSISTANT_MESSAGE_DELTA:
            sys.stdout.write(event.data.delta_content)
            sys.stdout.flush()

    session.on(handle_event)

    print("🌤️  Weather Assistant (type 'exit' to quit)")
    print("   Try: 'What's the weather in Paris?' or 'Compare weather in NYC and LA'\n")

    while True:
        try:
            user_input = input("You: ")
        except EOFError:
            break

        if user_input.lower() == "exit":
            break

        sys.stdout.write("Assistant: ")
        await session.send_and_wait({"prompt": user_input})
        print("\n")

    await client.stop()

asyncio.run(main())
```

Run with:

```bash
python weather_assistant.py
```

</details>

<details>
<summary><strong>.NET</strong></summary>

Create a new console project and update `Program.cs`:

```csharp
using GitHub.Copilot.SDK;
using Microsoft.Extensions.AI;
using System.ComponentModel;

// Define the weather tool using AIFunctionFactory
var getWeather = AIFunctionFactory.Create(
    ([Description("The city name")] string city) =>
    {
        var conditions = new[] { "sunny", "cloudy", "rainy", "partly cloudy" };
        var temp = Random.Shared.Next(50, 80);
        var condition = conditions[Random.Shared.Next(conditions.Length)];
        return new { city, temperature = $"{temp}°F", condition };
    },
    "get_weather",
    "Get the current weather for a city");

await using var client = new CopilotClient();
await using var session = await client.CreateSessionAsync(new SessionConfig
{
    Model = "gpt-4.1",
    Streaming = true,
    Tools = [getWeather]
});

// Listen for response chunks
session.On(ev =>
{
    if (ev is AssistantMessageDeltaEvent deltaEvent)
    {
        Console.Write(deltaEvent.Data.DeltaContent);
    }
    if (ev is SessionIdleEvent)
    {
        Console.WriteLine();
    }
});

Console.WriteLine("🌤️  Weather Assistant (type 'exit' to quit)");
Console.WriteLine("   Try: 'What's the weather in Paris?' or 'Compare weather in NYC and LA'\n");

while (true)
{
    Console.Write("You: ");
    var input = Console.ReadLine();

    if (string.IsNullOrEmpty(input) || input.Equals("exit", StringComparison.OrdinalIgnoreCase))
    {
        break;
    }

    Console.Write("Assistant: ");
    await session.SendAndWaitAsync(new MessageOptions { Prompt = input });
    Console.WriteLine("\n");
}
```

Run with:

```bash
dotnet run
```

</details>


**Example session:**

```
🌤️  Weather Assistant (type 'exit' to quit)
   Try: 'What's the weather in Paris?' or 'Compare weather in NYC and LA'

You: What's the weather in Seattle?
Assistant: Let me check the weather for Seattle...
It's currently 62°F and cloudy in Seattle.

You: How about Tokyo and London?
Assistant: I'll check both cities for you:
- Tokyo: 75°F and sunny
- London: 58°F and rainy

You: exit
```

You've built an assistant with a custom tool that Copilot can call!

---

## How Tools Work

When you define a tool, you're telling Copilot:
1. **What the tool does** (description)
2. **What parameters it needs** (schema)
3. **What code to run** (handler)

Copilot decides when to call your tool based on the user's question. When it does:
1. Copilot sends a tool call request with the parameters
2. The SDK runs your handler function
3. The result is sent back to Copilot
4. Copilot incorporates the result into its response

---

## What's Next?

Now that you've got the basics, here are more powerful features to explore:

### Connect to MCP Servers

MCP (Model Context Protocol) servers provide pre-built tools. Connect to GitHub's MCP server to give Copilot access to repositories, issues, and pull requests:

```typescript
const session = await client.createSession({
    mcpServers: {
        github: {
            type: "http",
            url: "https://api.githubcopilot.com/mcp/",
        },
    },
});
```

📖 **[Full MCP documentation →](./mcp.md)** - Learn about local vs remote servers, all configuration options, and troubleshooting.

### Create Custom Agents

Define specialized AI personas for specific tasks:

```typescript
const session = await client.createSession({
    customAgents: [{
        name: "pr-reviewer",
        displayName: "PR Reviewer",
        description: "Reviews pull requests for best practices",
        prompt: "You are an expert code reviewer. Focus on security, performance, and maintainability.",
    }],
});
```

### Customize the System Message

Control the AI's behavior and personality:

```typescript
const session = await client.createSession({
    systemMessage: {
        content: "You are a helpful assistant for our engineering team. Always be concise.",
    },
});
```

---

## Connecting to an External CLI Server

By default, the SDK automatically manages the Copilot CLI process lifecycle, starting and stopping the CLI as needed. However, you can also run the CLI in server mode separately and have the SDK connect to it. This can be useful for:

- **Debugging**: Keep the CLI running between SDK restarts to inspect logs
- **Resource sharing**: Multiple SDK clients can connect to the same CLI server
- **Development**: Run the CLI with custom settings or in a different environment

### Running the CLI in Server Mode

Start the CLI in server mode using the `--server` flag and optionally specify a port:

```bash
copilot --server --port 4321
```

If you don't specify a port, the CLI will choose a random available port.

### Connecting the SDK to the External Server

Once the CLI is running in server mode, configure your SDK client to connect to it using the "cli url" option:

<details open>
<summary><strong>Node.js / TypeScript</strong></summary>

```typescript
import { CopilotClient } from "@github/copilot-sdk";

const client = new CopilotClient({
    cliUrl: "localhost:4321"
});

// Use the client normally
const session = await client.createSession();
// ...
```

</details>

<details>
<summary><strong>Python</strong></summary>

```python
from copilot import CopilotClient

client = CopilotClient({
    "cli_url": "localhost:4321"
})
await client.start()

# Use the client normally
session = await client.create_session()
# ...
```

</details>

<details>
<summary><strong>Go</strong></summary>

```go
import copilot "github.com/github/copilot-sdk/go"

client := copilot.NewClient(&copilot.ClientOptions{
    CLIUrl: "localhost:4321",
})

if err := client.Start(); err != nil {
    log.Fatal(err)
}
defer client.Stop()

// Use the client normally
session, err := client.CreateSession()
// ...
```

</details>

<details>
<summary><strong>.NET</strong></summary>

```csharp
using GitHub.Copilot.SDK;

using var client = new CopilotClient(new CopilotClientOptions
{
    CliUrl = "localhost:4321",
    UseStdio = false
});

// Use the client normally
await using var session = await client.CreateSessionAsync();
// ...
```

</details>

**Note:** When `cli_url` / `cliUrl` / `CLIUrl` is provided, the SDK will not spawn or manage a CLI process - it will only connect to the existing server at the specified URL.

---

## Learn More

- [Node.js SDK Reference](../nodejs/README.md)
- [Python SDK Reference](../python/README.md)
- [Go SDK Reference](../go/README.md)
- [.NET SDK Reference](../dotnet/README.md)
- [Using MCP Servers](./mcp.md) - Integrate external tools via Model Context Protocol
- [GitHub MCP Server Documentation](https://github.com/github/github-mcp-server)
- [MCP Servers Directory](https://github.com/modelcontextprotocol/servers) - Explore more MCP servers

---

**You did it!** You've learned the core concepts of the GitHub Copilot SDK:
- ✅ Creating a client and session
- ✅ Sending messages and receiving responses
- ✅ Streaming for real-time output
- ✅ Defining custom tools that Copilot can call

Now go build something amazing! 🚀
