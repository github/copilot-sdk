# Advanced Usage

This guide covers advanced scenarios for extending and customizing your Copilot integration.

## Table of Contents

- [Custom Tools](#Custom_Tools)
  - [Overriding Built-in Tools](#Overriding_Built-in_Tools)
  - [Skipping Permission for Safe Tools](#Skipping_Permission_for_Safe_Tools)
- [Switching Models Mid-Session](#Switching_Models_Mid-Session)
- [System Messages](#System_Messages)
  - [Adding Rules](#Adding_Rules)
  - [Full Control](#Full_Control)
  - [Fine-grained Customization](#Fine-grained_Customization)
- [File Attachments](#File_Attachments)
  - [Inline Blob Attachments](#Inline_Blob_Attachments)
- [OpenTelemetry](#OpenTelemetry)
- [Bring Your Own Key (BYOK)](#Bring_Your_Own_Key_BYOK)
- [Infinite Sessions](#Infinite_Sessions)
  - [Manual Compaction](#Manual_Compaction)
  - [Compaction Events](#Compaction_Events)
- [MCP Servers](#MCP_Servers)
- [Custom Agents](#Custom_Agents)
  - [Programmatic Agent Selection](#Programmatic_Agent_Selection)
- [Skills Configuration](#Skills_Configuration)
  - [Loading Skills](#Loading_Skills)
  - [Disabling Skills](#Disabling_Skills)
- [Custom Configuration Directory](#Custom_Configuration_Directory)
- [Session Logging](#Session_Logging)
- [Early Event Registration](#Early_Event_Registration)
- [User Input Handling](#User_Input_Handling)
- [Permission Handling](#Permission_Handling)
- [Session Hooks](#Session_Hooks)
- [Manual Server Control](#Manual_Server_Control)
- [Session Context and Filtering](#Session_Context_and_Filtering)
  - [Listing Sessions with Context](#Listing_Sessions_with_Context)
  - [Filtering Sessions by Context](#Filtering_Sessions_by_Context)
  - [Context Changed Events](#Context_Changed_Events)
- [Session Lifecycle Events](#Session_Lifecycle_Events)
  - [Subscribing to All Lifecycle Events](#Subscribing_to_All_Lifecycle_Events)
  - [Subscribing to Specific Event Types](#Subscribing_to_Specific_Event_Types)
- [Foreground Session Control (TUI+Server Mode)](#Foreground_Session_Control_TUIServer_Mode)
  - [Getting the Foreground Session](#Getting_the_Foreground_Session)
  - [Setting the Foreground Session](#Setting_the_Foreground_Session)
- [Error Handling](#Error_Handling)
  - [Event Handler Exceptions](#Event_Handler_Exceptions)
  - [Custom Event Error Handler](#Custom_Event_Error_Handler)
  - [Event Error Policy](#Event_Error_Policy)
- [OpenTelemetry](#OpenTelemetry)
- [Slash Commands](#Slash_Commands)
  - [Registering Commands](#Registering_Commands)
- [Elicitation (UI Dialogs)](#Elicitation_UI_Dialogs)
  - [Incoming Elicitation Handler](#Incoming_Elicitation_Handler)
  - [Session Capabilities](#Session_Capabilities)
  - [Outgoing Elicitation via session.getUi()](#Outgoing_Elicitation_via_session.getUi)
- [Mode Handlers](#Mode_Handlers)
  - [Exit Plan Mode](#Exit_Plan_Mode)
  - [Auto Mode Switch](#Auto_Mode_Switch)
- [Getting Session Metadata by ID](#Getting_Session_Metadata_by_ID)

---

## Custom Tools

Let the AI call back into your application to fetch data or perform actions.

```java
// Define strongly-typed arguments with a record
record IssueArgs(String id) {}

var lookupTool = ToolDefinition.create(
    "lookup_issue",
    "Fetch issue details from our tracker",
    Map.of(
        "type", "object",
        "properties", Map.of(
            "id", Map.of("type", "string", "description", "Issue identifier")
        ),
        "required", List.of("id")
    ),
    invocation -> {
        IssueArgs args = invocation.getArgumentsAs(IssueArgs.class);
        return CompletableFuture.completedFuture(fetchIssue(args.id()));
    }
);

var session = client.createSession(
    new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
        .setTools(List.of(lookupTool))
).get();
```

See [ToolDefinition](apidocs/com/github/copilot/rpc/ToolDefinition.html) Javadoc for schema details.

### Overriding Built-in Tools

You can replace a built-in CLI tool (such as `grep` or `read_file`) with your own implementation
by using `ToolDefinition.createOverride()`. This signals to the CLI that the name collision is
intentional and your custom implementation should be used instead.

```java
var customGrep = ToolDefinition.createOverride(
    "grep",
    "Project-aware search with custom filtering",
    Map.of(
        "type", "object",
        "properties", Map.of(
            "query", Map.of("type", "string", "description", "Search query")
        ),
        "required", List.of("query")
    ),
    invocation -> {
        String query = (String) invocation.getArguments().get("query");
        // Your custom search logic here
        return CompletableFuture.completedFuture("Results for: " + query);
    }
);

var session = client.createSession(
    new SessionConfig()
        .setTools(List.of(customGrep))
        .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
).get();
```

### Skipping Permission for Safe Tools

When a tool performs only read-only or non-destructive operations, you can mark it to skip the
permission prompt entirely using `ToolDefinition.createSkipPermission()`:

```java
var safeLookup = ToolDefinition.createSkipPermission(
    "safe_lookup",
    "Look up a record by ID (read-only, no side effects)",
    Map.of(
        "type", "object",
        "properties", Map.of(
            "id", Map.of("type", "string")
        ),
        "required", List.of("id")
    ),
    invocation -> {
        String id = (String) invocation.getArguments().get("id");
        return CompletableFuture.completedFuture("Record: " + lookupRecord(id));
    }
);
```

The CLI bypasses the permission request for this tool invocation, so no `PermissionRequestedEvent`
is emitted and the `onPermissionRequest` handler is not called.

See [ToolDefinition](apidocs/com/github/copilot/rpc/ToolDefinition.html) Javadoc for details.

---

## Switching Models Mid-Session

You can change the model used by an existing session without losing conversation history.
The new model takes effect starting with the next message sent.

```java
var session = client.createSession(
    new SessionConfig()
        .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
).get();

// Switch to a different model mid-conversation
session.setModel("gpt-4.1").get();

// Switch with a specific reasoning effort level
session.setModel("claude-sonnet-4.6", "high").get();

// Next message will use the new model
session.sendAndWait(new MessageOptions().setPrompt("Continue with the new model")).get();
```

The `reasoningEffort` parameter accepts `"low"`, `"medium"`, `"high"`, or `"xhigh"` for models
that support reasoning. Pass `null` (or use the single-argument overload) to use the default.

The session emits a [`SessionModelChangeEvent`](apidocs/com/github/copilot/generated/SessionModelChangeEvent.html)
when the switch completes, which you can observe with `session.on(SessionModelChangeEvent.class, event -> ...)`.

See [CopilotSession.setModel()](apidocs/com/github/copilot/CopilotSession.html#setModel(java.lang.String)) Javadoc for details.

---

## System Messages

Customize the AI's behavior by adding rules or replacing the default prompt.

### Adding Rules

Use `APPEND` mode to add constraints while keeping default guardrails:

```java
var session = client.createSession(
    new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
        .setSystemMessage(new SystemMessageConfig()
            .setMode(SystemMessageMode.APPEND)
            .setContent("""
                <rules>
                - Always check for security vulnerabilities
                - Suggest performance improvements
                </rules>
            """))
).get();
```

### Full Control

Use `REPLACE` mode for complete control (removes default guardrails):

```java
var session = client.createSession(
    new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
        .setSystemMessage(new SystemMessageConfig()
            .setMode(SystemMessageMode.REPLACE)
            .setContent("You are a helpful coding assistant."))
).get();
```

### Fine-grained Customization

Use `CUSTOMIZE` mode to override individual sections of the default system prompt without
replacing it entirely. You can replace, remove, append, prepend, or transform specific sections
using the section identifiers from `SystemPromptSections`.

**Static overrides:**

```java
var session = client.createSession(
    new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
        .setSystemMessage(new SystemMessageConfig()
            .setMode(SystemMessageMode.CUSTOMIZE)
            .setSections(Map.of(
                // Replace the tone section
                SystemPromptSections.TONE,
                    new SectionOverride()
                        .setAction(SectionOverrideAction.REPLACE)
                        .setContent("Be concise and formal in all responses."),
                // Remove the code-change-rules section entirely
                SystemPromptSections.CODE_CHANGE_RULES,
                    new SectionOverride()
                        .setAction(SectionOverrideAction.REMOVE)
            ))
            // Optional: extra content appended after all sections
            .setContent("Always mention quarterly earnings."))
).get();
```

**Transform callbacks** let you inspect and modify section content at runtime:

```java
var session = client.createSession(
    new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
        .setSystemMessage(new SystemMessageConfig()
            .setMode(SystemMessageMode.CUSTOMIZE)
            .setSections(Map.of(
                SystemPromptSections.IDENTITY,
                    new SectionOverride()
                        .setTransform(content ->
                            CompletableFuture.completedFuture(
                                content + "\nAlways end your reply with DONE."))
            )))
).get();
```

See [SystemMessageConfig](apidocs/com/github/copilot/rpc/SystemMessageConfig.html),
[SectionOverride](apidocs/com/github/copilot/rpc/SectionOverride.html), and
[SystemPromptSections](apidocs/com/github/copilot/rpc/SystemPromptSections.html) Javadoc for details.

---

## File Attachments

Include files as context for the AI to analyze. The `Attachment` record takes three parameters:

| Parameter | Type | Description |
|-----------|------|-------------|
| `type` | String | The attachment type — use `"file"` for filesystem files |
| `path` | String | The absolute path to the file on disk |
| `displayName` | String | A human-readable label shown to the AI (e.g., the filename or a description) |

```java
session.send(new MessageOptions()
    .setPrompt("Review this file for bugs")
    .setAttachments(List.of(
        new Attachment("file", "/path/to/file.java", "MyService.java")
    ))
).get();
```

You can attach multiple files in a single message:

```java
session.send(new MessageOptions()
    .setPrompt("Compare these two implementations")
    .setAttachments(List.of(
        new Attachment("file", "/src/main/OldImpl.java", "Old Implementation"),
        new Attachment("file", "/src/main/NewImpl.java", "New Implementation")
    ))
).get();
```

### Inline Blob Attachments

Use `BlobAttachment` to pass inline base64-encoded binary data — for example, an image captured
at runtime — without writing it to disk first:

```java
// Load image bytes and base64-encode them
byte[] imageBytes = Files.readAllBytes(Path.of("/path/to/screenshot.png"));
String base64Data = Base64.getEncoder().encodeToString(imageBytes);

session.send(new MessageOptions()
    .setPrompt("Describe this screenshot")
    .setAttachments(List.of(
        new BlobAttachment()
            .setData(base64Data)
            .setMimeType("image/png")
            .setDisplayName("screenshot.png")
    ))
).get();
```

See [BlobAttachment](apidocs/com/github/copilot/rpc/BlobAttachment.html) Javadoc for details.

Both `Attachment` and `BlobAttachment` implement the sealed `MessageAttachment` interface.
For a mixed list with both types, use an explicit type hint:

```java
session.send(new MessageOptions()
    .setPrompt("Analyze these")
    .setAttachments(List.<MessageAttachment>of(
        new Attachment("file", "/path/to/file.java", "Source"),
        new BlobAttachment()
            .setData(base64Data)
            .setMimeType("image/png")
            .setDisplayName("screenshot.png")
    ))
).get();
```

---

## Bring Your Own Key (BYOK)

Use your own OpenAI or Azure OpenAI API key instead of GitHub Copilot.

Supported providers:

| Provider | Type value | Notes |
|----------|-----------|-------|
| OpenAI | `"openai"` | Standard OpenAI API |
| Azure OpenAI / Azure AI Foundry | `"azure"` | Azure-hosted models |
| Anthropic | `"anthropic"` | Claude models |
| Ollama | `"openai"` | Local models via OpenAI-compatible API |
| Microsoft Foundry Local | `"openai"` | Run AI models locally on your device via OpenAI-compatible API |
| Other OpenAI-compatible | `"openai"` | vLLM, LiteLLM, etc. |

### API Key Authentication

```java
var session = client.createSession(
    new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
        .setProvider(new ProviderConfig()
            .setType("openai")
            .setBaseUrl("https://api.openai.com/v1")
            .setApiKey("sk-..."))
).get();
```

### Bearer Token Authentication

Some providers require bearer token authentication instead of API keys:

```java
var session = client.createSession(
    new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
        .setProvider(new ProviderConfig()
            .setType("openai")
            .setBaseUrl("https://my-custom-endpoint.example.com/v1")
            .setBearerToken(System.getenv("MY_BEARER_TOKEN")))
).get();
```

> **Note:** The `bearerToken` option accepts a **static token string** only. The SDK does not refresh this token automatically. If your token expires, requests will fail and you'll need to create a new session with a fresh token.

### Model Overrides

Use `modelId` and `wireModel` to control model resolution and the model name on the wire:

```java
var session = client.createSession(
    new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
        .setProvider(new ProviderConfig()
            .setType("openai")
            .setBaseUrl("https://api.openai.com/v1")
            .setApiKey("sk-...")
            .setModelId("gpt-4o")           // Runtime config lookup
            .setWireModel("my-finetune-v3") // Sent to the provider API
            .setMaxPromptTokens(100_000)    // Override max prompt tokens
            .setMaxOutputTokens(4096))      // Override max output tokens
).get();
```

- **`modelId`** — Well-known model name used by the runtime to look up agent configuration (tools, prompts, reasoning behavior) and default token limits. Also used as the wire model when `wireModel` is not set.
- **`wireModel`** — Model name sent to the provider API for inference. Use when the provider's model name (e.g., an Azure deployment name or a custom fine-tune name) differs from `modelId`.
- **`maxPromptTokens`** — Overrides the resolved model's default max prompt tokens. The runtime triggers conversation compaction when the prompt would exceed this limit.
- **`maxOutputTokens`** — Overrides the resolved model's default max output tokens.

### Microsoft Foundry Local

[Microsoft Foundry Local](https://foundrylocal.ai) lets you run AI models locally on your own device with an OpenAI-compatible API. Install it via the Foundry Local CLI, then point the SDK at your local endpoint:

```java
var session = client.createSession(
    new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
        .setProvider(new ProviderConfig()
            .setType("openai")
            .setBaseUrl("http://localhost:<PORT>/v1"))
            // No apiKey needed for local Foundry Local
).get();
```

> **Note:** Foundry Local starts on a **dynamic port** — the port is not fixed. Use `foundry service status` to confirm the port the service is currently listening on, then use that port in your `baseUrl`.

To get started with Foundry Local:

```bash
# Windows: Install Foundry Local CLI (requires winget)
winget install Microsoft.FoundryLocal

# macOS / Linux: see https://foundrylocal.ai for installation instructions

# List available models
foundry model list

# Run a model (starts the local server automatically)
foundry model run phi-4-mini

# Check the port the service is running on
foundry service status
```

### Limitations

When using BYOK, be aware of these limitations:

#### Identity Limitations

BYOK authentication uses **static credentials only**. The following identity providers are NOT supported:

- ❌ **Microsoft Entra ID (Azure AD)** - No support for Entra managed identities or service principals
- ❌ **Third-party identity providers** - No OIDC, SAML, or other federated identity
- ❌ **Managed identities** - Azure Managed Identity is not supported

You must use an API key or static bearer token that you manage yourself.

**Why not Entra ID?** While Entra ID does issue bearer tokens, these tokens are short-lived (typically 1 hour) and require automatic refresh via the Azure Identity SDK. The `bearerToken` option only accepts a static string—there is no callback mechanism for the SDK to request fresh tokens. For long-running workloads requiring Entra authentication, you would need to implement your own token refresh logic and create new sessions with updated tokens.

---

## Infinite Sessions

Run long conversations without hitting context limits.

When enabled (default), the session automatically compacts older messages as the context window fills up.

```java
var session = client.createSession(
    new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
        .setInfiniteSessions(new InfiniteSessionConfig()
            .setEnabled(true)
            .setBackgroundCompactionThreshold(0.80)  // Start compacting at 80%
            .setBufferExhaustionThreshold(0.95))     // Block at 95%
).get();

// Access the workspace where session state is persisted
var workspace = session.getWorkspacePath();
```

### Manual Compaction

Trigger compaction immediately when you want to reduce context usage before the automatic threshold is reached:

```java
session.compact().get();
```

### Compaction Events

When compaction occurs, the session emits events that you can listen for:

```java
session.on(SessionCompactionStartEvent.class, start -> {
    System.out.println("Compaction started");
});
session.on(SessionCompactionCompleteEvent.class, complete -> {
    var data = complete.getData();
    System.out.println("Compaction completed - success: " + data.success() 
        + ", tokens removed: " + data.tokensRemoved());
});
```

For short conversations, disable to avoid overhead:

```java
new InfiniteSessionConfig().setEnabled(false)
```

---

## MCP Servers

Extend the AI with external tools via the Model Context Protocol.

```java
Map<String, Object> server = Map.of(
    "type", "local",
    "command", "npx",
    "args", List.of("-y", "@modelcontextprotocol/server-filesystem", "/tmp"),
    "tools", List.of("*")
);

var session = client.createSession(
    new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
        .setMcpServers(Map.of("filesystem", server))
).get();
```

📖 **[Full MCP documentation →](mcp.html)** for local/remote servers and all options.

---

## Custom Agents

Extend the base Copilot assistant with specialized agents that have their own tools, prompts, and behavior. Users can invoke agents using the `@agent-name` mention syntax in messages.

```java
var reviewer = new CustomAgentConfig()
    .setName("code-reviewer")
    .setDisplayName("Code Reviewer")
    .setDescription("Reviews code for best practices and security")
    .setPrompt("You are a code review expert. Focus on security, performance, and maintainability.")
    .setTools(List.of("read_file", "search_code"));

var session = client.createSession(
    new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
        .setCustomAgents(List.of(reviewer))
).get();

// The user can now mention @code-reviewer in messages
session.send("@code-reviewer Review src/Main.java").get();
```

### Configuration Options

| Option | Type | Description |
|--------|------|-------------|
| `name` | String | Unique identifier used for `@mentions` (alphanumeric and hyphens) |
| `displayName` | String | Human-readable name shown to users |
| `description` | String | Describes the agent's capabilities |
| `prompt` | String | System prompt that defines the agent's behavior |
| `tools` | List&lt;String&gt; | Tool names available to this agent |
| `mcpServers` | Map | MCP servers available to this agent |
| `infer` | Boolean | Whether the agent can be auto-selected based on context |
| `skills` | List&lt;String&gt; | Skill names to preload into the agent's context |
| `model` | String | Model identifier for this agent (e.g., "claude-haiku-4.5"); falls back to the parent session model if unavailable |

### Multiple Agents

Register multiple agents for different tasks:

```java
var agents = List.of(
    new CustomAgentConfig()
        .setName("reviewer")
        .setDescription("Code review")
        .setPrompt("You review code for issues."),
    new CustomAgentConfig()
        .setName("documenter")
        .setDescription("Documentation writer")
        .setPrompt("You write clear documentation.")
        .setInfer(true)  // Auto-select when appropriate
);

var session = client.createSession(
    new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL).setCustomAgents(agents)
).get();
```

See [CustomAgentConfig](apidocs/com/github/copilot/rpc/CustomAgentConfig.html) Javadoc for full details.

### Programmatic Agent Selection

You can inspect and switch agents at runtime:

```java
var available = session.listAgents().get();

var current = session.getCurrentAgent().get();
if (current != null) {
    System.out.println("Current agent: " + current.name());
}

var selected = session.selectAgent("reviewer").get();
System.out.println("Selected: " + selected.name());

session.deselectAgent().get(); // Return to the default agent
```

---

## Skills Configuration

Load custom skills from directories to extend the AI's capabilities with domain-specific knowledge.

### Loading Skills

Skills are loaded from `SKILL.md` files in subdirectories of the specified skill directories:

```java
var session = client.createSession(
    new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
        .setSkillDirectories(List.of("/path/to/skills"))
).get();
```

Each skill subdirectory should contain a `SKILL.md` file with YAML frontmatter:

```markdown
---
name: my-skill
description: A skill that provides domain-specific knowledge
---

# Skill Instructions

Your skill instructions go here...
```

### Disabling Skills

Disable specific skills by name:

```java
var session = client.createSession(
    new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
        .setSkillDirectories(List.of("/path/to/skills"))
        .setDisabledSkills(List.of("my-skill"))
).get();
```

---

## Instruction Directories

Provide additional directories containing custom instruction files. These instructions are automatically included in the system message for all conversations in the session.

```java
var session = client.createSession(
    new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
        .setInstructionDirectories(List.of("/path/to/instructions"))
).get();
```

Instruction files are discovered from `.github/instructions/` subdirectories within each specified path and should use the `.instructions.md` extension.

This is also supported on session resume:

```java
var session = client.resumeSession(sessionId,
    new ResumeSessionConfig()
        .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
        .setInstructionDirectories(List.of("/path/to/instructions"))
);
```

---

## Custom Configuration Directory

Use a custom configuration directory for session settings:

```java
var session = client.createSession(
    new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
        .setConfigDir("/path/to/custom/config")
).get();
```

This is useful when you need to isolate session configuration or use different settings for different environments.

---

## Session Logging

Send log messages to the session for debugging, status updates, or UI feedback.

```java
// Simple log message (defaults to "info" level)
session.log("Processing step 1 of 3").get();

// Log with explicit level and ephemeral flag
session.log("Downloading dependencies...", "info", true).get();
```

| Parameter | Type | Description |
|-----------|------|-------------|
| `message` | String | The log message text |
| `level` | String | Log level: `"info"`, `"warning"`, `"error"` |
| `ephemeral` | Boolean | If `true`, the message is transient and may not be persisted |

Use cases:
- Displaying progress in a UI while the session processes a request
- Sending status updates to the session log
- Debugging session behavior with contextual messages

See [CopilotSession.log()](apidocs/com/github/copilot/CopilotSession.html#log(java.lang.String)) Javadoc for details.

---

## Early Event Registration

Register an event handler *before* the `session.create` RPC is issued, ensuring no early events are missed.

When you register handlers with `session.on()` after `createSession()` returns, you may miss events emitted during session creation (e.g., `SessionStartEvent`). Use `SessionConfig.setOnEvent()` to guarantee delivery of all events from the very start:

```java
var events = new CopyOnWriteArrayList<SessionEvent>();

var session = client.createSession(
    new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
        .setOnEvent(events::add)  // Registered before session.create RPC
).get();

// events list now includes SessionStartEvent and any other early events
```

This is equivalent to calling `session.on(handler)` immediately after creation, but executes earlier in the lifecycle. The same option is available on `ResumeSessionConfig.setOnEvent()` for resumed sessions.

---

## User Input Handling

Handle user input requests when the AI uses the `ask_user` tool to gather information from the user.

```java
var session = client.createSession(
    new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
        .setOnUserInputRequest((request, invocation) -> {
            System.out.println("Agent asks: " + request.getQuestion());
            
            // Check if choices are provided
            if (request.getChoices() != null && !request.getChoices().isEmpty()) {
                System.out.println("Options: " + request.getChoices());
                // Return one of the provided choices
                var selectedChoice = request.getChoices().get(0);
                return CompletableFuture.completedFuture(
                    new UserInputResponse()
                        .setAnswer(selectedChoice)
                        .setWasFreeform(false)
                );
            }
            
            // Freeform input
            var userAnswer = getUserInput(); // your input method
            return CompletableFuture.completedFuture(
                new UserInputResponse()
                    .setAnswer(userAnswer)
                    .setWasFreeform(true)
            );
        })
).get();
```

The `UserInputRequest` contains:
- `getQuestion()` - The question the AI is asking
- `getChoices()` - Optional list of choices for the user to select from

The `UserInputResponse` should include:
- `setAnswer(String)` - The user's answer
- `setWasFreeform(boolean)` - `true` if the answer was freeform text, `false` if it was from the provided choices

See [UserInputHandler](apidocs/com/github/copilot/rpc/UserInputHandler.html) Javadoc for more details.

---

## Permission Handling

Approve or deny permission requests from the AI.

```java
var session = client.createSession(
    new SessionConfig().setOnPermissionRequest((request, invocation) -> {
        // Inspect request and approve/deny using typed constants
        var result = new PermissionRequestResult();
        result.setKind(PermissionRequestResultKind.APPROVED);
        return CompletableFuture.completedFuture(result);
    })
).get();
```

The `PermissionRequestResultKind` class provides well-known constants for common outcomes:

| Constant | Value | Meaning |
|---|---|---|
| `PermissionRequestResultKind.APPROVED` | `"approve-once"` | The permission was approved for this one instance |
| `PermissionRequestResultKind.REJECTED` | `"reject"` | The permission was denied interactively by the user |
| `PermissionRequestResultKind.USER_NOT_AVAILABLE` | `"user-not-available"` | Denied because user confirmation was unavailable |
| `PermissionRequestResultKind.NO_RESULT` | `"no-result"` | No permission decision was made (protocol v3 only) |

You can also pass a raw string to `setKind(String)` for custom or extension values. Use
[`PermissionHandler.APPROVE_ALL`](apidocs/com/github/copilot/rpc/PermissionHandler.html) to approve all
requests without writing a handler.

---

## Session Hooks

Intercept tool execution and session lifecycle events using hooks.

```java
var hooks = new SessionHooks()
    .setOnPreToolUse((input, invocation) -> {
        System.out.println("Tool: " + input.getToolName());
        return CompletableFuture.completedFuture(PreToolUseHookOutput.allow());
    })
    .setOnPreMcpToolCall((input, invocation) -> {
        System.out.println("MCP Tool: " + input.getServerName() + "/" + input.getToolName());
        // Set metadata on the MCP request
        JsonNode meta = mapper.valueToTree(Map.of("source", "my-app"));
        return CompletableFuture.completedFuture(PreMcpToolCallHookOutput.withMeta(meta));
    })
    .setOnPostToolUse((input, invocation) -> {
        System.out.println("Result: " + input.getToolResult());
        return CompletableFuture.completedFuture(null);
    });

var session = client.createSession(
    new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL).setHooks(hooks)
).get();
```

📖 **[Full Session Hooks documentation →](hooks.html)** for all 6 hook types, inputs/outputs, and examples.

---

## Manual Server Control

Control the CLI lifecycle yourself instead of auto-start.

```java
var client = new CopilotClient(
    new CopilotClientOptions().setAutoStart(false)
);

client.start().get();   // Start manually
// ... use client ...
client.stop().get();    // Stop manually
```

### Graceful Stop vs Force Stop

The SDK provides two shutdown methods:

| Method | Behavior |
|--------|----------|
| `stop()` | Gracefully closes all open sessions, then shuts down the connection |
| `forceStop()` | Immediately clears sessions and shuts down — no graceful session cleanup |

Use `stop()` for normal shutdown — it ensures each session is properly closed (flushing pending operations) before terminating the connection:

```java
// Graceful: closes all sessions, then disconnects
client.stop().get();
```

Use `forceStop()` when you need to terminate immediately, such as during error recovery or when the server is unresponsive:

```java
// Immediate: skips session cleanup, kills connection
client.forceStop().get();
```

> **Tip:** In `try-with-resources` blocks, `close()` delegates to `stop()`, so graceful session cleanup happens automatically.
> `close()` is blocking and waits up to `CopilotClient.AUTOCLOSEABLE_TIMEOUT_SECONDS` seconds for shutdown to complete.

---

## Session Context and Filtering

Track and filter sessions by their working directory context including the current directory, git repository, and branch information.

### Listing Sessions with Context

Session metadata may include context information for persisted sessions:

```java
var sessions = client.listSessions().get();
for (var session : sessions) {
    var context = session.getContext();
    if (context != null) {
        System.out.println("Session: " + session.getSessionId());
        System.out.println("  Working dir: " + context.getCwd());
        System.out.println("  Repository: " + context.getRepository());
        System.out.println("  Branch: " + context.getBranch());
        System.out.println("  Git root: " + context.getGitRoot());
    }
}
```

### Filtering Sessions by Context

Use `SessionListFilter` to filter sessions by context fields:

```java
// Find sessions for a specific repository
var filter = new SessionListFilter()
    .setRepository("owner/myproject")
    .setBranch("main");

var sessions = client.listSessions(filter).get();
```

Filter options:
- `setCwd(String)` - Filter by exact working directory match
- `setGitRoot(String)` - Filter by git repository root
- `setRepository(String)` - Filter by repository in "owner/repo" format
- `setBranch(String)` - Filter by git branch name

### Context Changed Events

Listen for changes to the working directory context:

```java
session.on(SessionContextChangedEvent.class, event -> {
    var newContext = event.getData();
    System.out.println("Context changed:");
    System.out.println("  New CWD: " + newContext.getCwd());
    System.out.println("  Repository: " + newContext.getRepository());
    System.out.println("  Branch: " + newContext.getBranch());
});
```

The `session.context_changed` event fires when the working directory context changes between conversation turns.

---

## Session Lifecycle Events

Subscribe to lifecycle events to be notified when sessions are created, deleted, updated, or change foreground/background state.

### Subscribing to All Lifecycle Events

```java
var subscription = client.onLifecycle(event -> {
    System.out.println("Session " + event.getSessionId() + ": " + event.getType());
    
    if (event.getMetadata() != null) {
        System.out.println("  Summary: " + event.getMetadata().getSummary());
    }
});

// Later, when done listening:
subscription.close();
```

### Subscribing to Specific Event Types

```java
import com.github.copilot.rpc.SessionLifecycleEventTypes;

// Listen only for session creation
var subscription = client.onLifecycle(
    SessionLifecycleEventTypes.CREATED,
    event -> System.out.println("New session: " + event.getSessionId())
);
```

Available event types:
- `SessionLifecycleEventTypes.CREATED` - Session was created
- `SessionLifecycleEventTypes.DELETED` - Session was deleted
- `SessionLifecycleEventTypes.UPDATED` - Session was updated
- `SessionLifecycleEventTypes.FOREGROUND` - Session moved to foreground (TUI+server mode)
- `SessionLifecycleEventTypes.BACKGROUND` - Session moved to background (TUI+server mode)

---

## Foreground Session Control (TUI+Server Mode)

When connecting to a server running in TUI+server mode (`--ui-server`), you can control which session is displayed in the TUI.

### Getting the Foreground Session

```java
var sessionId = client.getForegroundSessionId().get();
if (sessionId != null) {
    System.out.println("TUI is displaying session: " + sessionId);
}
```

### Setting the Foreground Session

```java
client.setForegroundSessionId("session-123").get();
```

---

## Error Handling

All SDK methods return `CompletableFuture`. Errors surface via `ExecutionException`:

```java
try {
    session.send(new MessageOptions().setPrompt("Hello")).get();
} catch (ExecutionException ex) {
    System.err.println("Error: " + ex.getCause().getMessage());
}
```

For reactive error handling, use `exceptionally()` or `handle()`:

```java
session.send(new MessageOptions().setPrompt("Hello"))
    .exceptionally(ex -> {
        System.err.println("Failed: " + ex.getMessage());
        return null;
    });
```

### Event Handler Exceptions

If an event handler registered via `session.on()` throws an exception, the SDK
catches it and logs it at `WARNING` level. By default, dispatch **stops** after
the first handler error (`PROPAGATE_AND_LOG_ERRORS` policy). You can opt in to
continue dispatching despite errors using `SUPPRESS_AND_LOG_ERRORS`:

```java
// With SUPPRESS_AND_LOG_ERRORS, second handler still runs
session.setEventErrorPolicy(EventErrorPolicy.SUPPRESS_AND_LOG_ERRORS);

session.on(AssistantMessageEvent.class, msg -> {
    throw new RuntimeException("bug in handler 1");
});

session.on(AssistantMessageEvent.class, msg -> {
    // This handler executes normally despite the exception above
    System.out.println(msg.getData().content());
});
```

Errors are **always logged** at `WARNING` level regardless of the policy or
whether a custom error handler is set.

### Custom Event Error Handler

Set a custom `EventErrorHandler` for additional handling beyond the default
logging — such as metrics, alerts, or integration with external
error-reporting systems:

```java
session.setEventErrorHandler((event, exception) -> {
    metrics.increment("handler.errors");
    logger.error("Handler failed on {}: {}",
        event.getType(), exception.getMessage());
});
```

The error handler receives both the event that was being dispatched and the
exception that was thrown. If the error handler itself throws, that exception
is caught and logged at `SEVERE`, and dispatch is stopped to prevent cascading
failures.

Pass `null` to use only the default logging behavior:

```java
session.setEventErrorHandler(null);
```

### Event Error Policy

By default, the SDK propagates errors and stops dispatch on the first handler
error (`EventErrorPolicy.PROPAGATE_AND_LOG_ERRORS`). You can opt in to
**suppress** errors so that all handlers execute despite errors:

```java
session.setEventErrorPolicy(EventErrorPolicy.SUPPRESS_AND_LOG_ERRORS);
```

The `EventErrorHandler` (if set) is always invoked regardless of the policy —
the policy only controls whether remaining handlers execute after the error
handler returns. Errors are always logged at `WARNING` level.

| Policy | Behavior |
|---|---|
| `PROPAGATE_AND_LOG_ERRORS` (default) | Log the error; dispatch halts after the first error |
| `SUPPRESS_AND_LOG_ERRORS` | Log the error; all remaining handlers execute |

You can combine both for full control:

```java
// Log errors via custom handler and suppress (continue dispatching)
session.setEventErrorPolicy(EventErrorPolicy.SUPPRESS_AND_LOG_ERRORS);
session.setEventErrorHandler((event, ex) ->
    logger.error("Handler failed, continuing: {}", ex.getMessage(), ex));
```

Or switch policies dynamically:

```java
// Start strict (propagate errors, stop dispatch)
session.setEventErrorPolicy(EventErrorPolicy.PROPAGATE_AND_LOG_ERRORS);

// Later, switch to lenient mode (suppress errors, continue)
session.setEventErrorPolicy(EventErrorPolicy.SUPPRESS_AND_LOG_ERRORS);
```

See [EventErrorPolicy](apidocs/com/github/copilot/EventErrorPolicy.html) and [EventErrorHandler](apidocs/com/github/copilot/EventErrorHandler.html) Javadoc for details.

---

## OpenTelemetry

Enable OpenTelemetry tracing in the Copilot CLI server by configuring a `TelemetryConfig`
on the `CopilotClientOptions`. This is useful for observability, performance monitoring,
and debugging.

```java
var options = new CopilotClientOptions()
    .setTelemetry(new TelemetryConfig()
        .setOtlpEndpoint("http://localhost:4318")  // OTLP/HTTP exporter
        .setSourceName("my-app"));

var client = new CopilotClient(options);
```

To export to a local file instead:

```java
var options = new CopilotClientOptions()
    .setTelemetry(new TelemetryConfig()
        .setExporterType("file")
        .setFilePath("/tmp/copilot-traces.json")
        .setCaptureContent(true));  // include message content in spans
```

| Property | Environment Variable | Description |
|----------|---------------------|-------------|
| `otlpEndpoint` | `OTEL_EXPORTER_OTLP_ENDPOINT` | OTLP exporter endpoint URL |
| `filePath` | `COPILOT_OTEL_FILE_EXPORTER_PATH` | File path for the file exporter |
| `exporterType` | `COPILOT_OTEL_EXPORTER_TYPE` | `"otlp-http"` or `"file"` |
| `sourceName` | `COPILOT_OTEL_SOURCE_NAME` | Source name for telemetry spans |
| `captureContent` | `OTEL_INSTRUMENTATION_GENAI_CAPTURE_MESSAGE_CONTENT` | Whether to capture message content |

See [TelemetryConfig](apidocs/com/github/copilot/rpc/TelemetryConfig.html) Javadoc for details.

---

## Slash Commands

Register custom slash commands that users can invoke from the CLI TUI with `/commandname`.

### Registering Commands

```java
var config = new SessionConfig()
    .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
    .setCommands(List.of(
        new CommandDefinition()
            .setName("deploy")
            .setDescription("Deploy the current branch")
            .setHandler(context -> {
                System.out.println("Deploying with args: " + context.getArgs());
                // perform deployment ...
                return CompletableFuture.completedFuture(null);
            }),
        new CommandDefinition()
            .setName("rollback")
            .setDescription("Roll back the last deployment")
            .setHandler(context -> {
                // perform rollback ...
                return CompletableFuture.completedFuture(null);
            })
    ));

try (CopilotClient client = new CopilotClient()) {
    client.start().get();
    var session = client.createSession(config).get();
    // Users can now type /deploy or /rollback in the TUI
}
```

Each `CommandDefinition` requires a `name` (without the leading `/`), an optional `description` shown in the TUI's command completion UI, and a `CommandHandler` that is invoked when the user executes the command.

The `CommandContext` passed to the handler provides:
- `getSessionId()` — the ID of the session where the command was invoked
- `getCommand()` — the full command text (e.g., `/deploy production`)
- `getCommandName()` — command name without the leading `/` (e.g., `deploy`)
- `getArgs()` — the argument string after the command name (e.g., `production`)

---

## Elicitation (UI Dialogs)

Elicitation allows your application to present structured UI dialogs to the user. There are two directions:

1. **Incoming** — The server or an MCP tool requests input from the user via your `onElicitationRequest` handler.
2. **Outgoing** — Your session-side code proactively requests input via `session.getUi()`.

### Incoming Elicitation Handler

Register a handler to receive elicitation requests from the server:

```java
var config = new SessionConfig()
    .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
    .setOnElicitationRequest(context -> {
        System.out.println("Elicitation request: " + context.getMessage());
        // Show the form to the user ...
        var content = Map.of("confirmed", true);
        return CompletableFuture.completedFuture(
            new ElicitationResult()
                .setAction(ElicitationResultAction.ACCEPT)
                .setContent(content)
        );
    });
```

When `onElicitationRequest` is set, the SDK reports elicitation as a supported capability and the server will route elicitation requests to your handler.

### Session Capabilities

After `createSession` or `resumeSession`, check `session.getCapabilities()` to see what the host supports:

```java
var session = client.createSession(config).get();

var caps = session.getCapabilities();
if (caps.getUi() != null && Boolean.TRUE.equals(caps.getUi().getElicitation())) {
    System.out.println("Elicitation is supported");
}
```

Capabilities are updated in real time when a `capabilities.changed` event is received.

### Outgoing Elicitation via `session.getUi()`

If the host reports elicitation support, you can call the convenience methods on `session.getUi()`:

```java
var ui = session.getUi();

// Boolean confirmation
boolean confirmed = ui.confirm("Are you sure you want to proceed?").get();

// Selection from options
String choice = ui.select("Choose an environment", new String[]{"dev", "staging", "prod"}).get();

// Text input
String value = ui.input("Enter your name", null).get();

// Custom schema
var result = ui.elicitation(new ElicitationParams()
    .setMessage("Enter deployment details")
    .setRequestedSchema(new ElicitationSchema()
        .setProperties(Map.of(
            "branch", Map.of("type", "string"),
            "environment", Map.of("type", "string", "enum", List.of("dev", "staging", "prod"))
        ))
        .setRequired(List.of("branch", "environment"))
    )).get();
```

All `getUi()` methods throw `IllegalStateException` if the host does not support elicitation. Always check capabilities first.

---

## Mode Handlers

Mode handlers let your application respond to mode transitions requested by the Copilot CLI.

### Exit Plan Mode

When the model finishes creating a plan and wants to transition out of plan mode, it invokes the `exitPlanMode` handler. Register the handler via `SessionConfig.setOnExitPlanMode()`:

```java
var session = client.createSession(new SessionConfig()
    .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
    .setOnExitPlanMode((request, invocation) -> {
        System.out.println("Plan summary: " + request.getSummary());
        System.out.println("Available actions: " + request.getActions());
        System.out.println("Recommended: " + request.getRecommendedAction());

        return CompletableFuture.completedFuture(
            new ExitPlanModeResult()
                .setApproved(true)
                .setSelectedAction("interactive")
                .setFeedback("Looks good, proceed!"));
    })).get();
```

When no handler is registered, the SDK automatically approves the plan (`approved=true`). The handler receives an `ExitPlanModeRequest` with:

| Field               | Description                                     |
|---------------------|-------------------------------------------------|
| `summary`           | Summary of the plan that was created            |
| `planContent`       | Full content of the plan file                   |
| `actions`           | Available actions (e.g., interactive, autopilot) |
| `recommendedAction` | The recommended action for the user             |

### Auto Mode Switch

When the model encounters a rate limit or similar constraint, it may request to switch modes automatically. Register the handler via `SessionConfig.setOnAutoModeSwitch()`:

```java
var session = client.createSession(new SessionConfig()
    .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
    .setOnAutoModeSwitch((request, invocation) -> {
        System.out.println("Error: " + request.getErrorCode());
        System.out.println("Retry after: " + request.getRetryAfterSeconds() + "s");

        return CompletableFuture.completedFuture(AutoModeSwitchResponse.YES);
    })).get();
```

When no handler is registered, the SDK returns `NO` (declining the mode switch). The response options are:

| Response                        | Description                              |
|---------------------------------|------------------------------------------|
| `AutoModeSwitchResponse.YES`       | Allow the mode switch this time          |
| `AutoModeSwitchResponse.YES_ALWAYS`| Always allow automatic mode switches     |
| `AutoModeSwitchResponse.NO`        | Decline the mode switch                  |

Both handlers are also available on `ResumeSessionConfig` for resumed sessions.

---

## Getting Session Metadata by ID

Retrieve metadata for a specific session without listing all sessions:

```java
SessionMetadata metadata = client.getSessionMetadata("session-123").get();
if (metadata != null) {
    System.out.println("Session: " + metadata.getSessionId());
    System.out.println("Started: " + metadata.getStartTime());
} else {
    System.out.println("Session not found");
}
```

This is more efficient than `listSessions()` when you already know the session ID, as it performs a direct O(1) lookup instead of scanning all sessions.

---

## Remote Sessions

Remote sessions enable Mission Control integration, making sessions accessible from GitHub web and mobile. When enabled, sessions in a GitHub repository working directory receive a remote URL.

### Enabling Remote Sessions

Set `remote(true)` on the client options to enable remote session support for all sessions:

```java
var options = new CopilotClientOptions()
    .setRemote(true)
    .setCwd("/path/to/github-repo");

try (var client = new CopilotClient(options)) {
    var session = client.createSession(
        new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
    ).get();

    // Listen for the remote URL info event
    session.on(SessionInfoEvent.class, event -> {
        System.out.println("Remote URL: " + event.getData());
    });
}
```

### Prerequisites

- The user must be authenticated (GitHub token or logged-in user)
- The session's working directory must be a GitHub repository
- This option is only used when the SDK spawns the CLI process; it is ignored when connecting to an external server via `setCliUrl()`

### Cloud Sessions

Cloud sessions create a remote session in the cloud instead of a local session. Optionally associate repository metadata with the cloud session:

```java
var cloudOptions = new CloudSessionOptions()
    .setRepository(new CloudSessionRepository()
        .setOwner("my-org")
        .setName("my-repo")
        .setBranch("main"));

var session = client.createSession(
    new SessionConfig()
        .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
        .setCloud(cloudOptions)
).get();
```

See [CloudSessionOptions](apidocs/com/github/copilot/rpc/CloudSessionOptions.html) and [CloudSessionRepository](apidocs/com/github/copilot/rpc/CloudSessionRepository.html) Javadoc for details.

---

## Next Steps

- 📖 **[Documentation](documentation.html)** - Core concepts, events, streaming, models, tool filtering, reasoning effort
- 📖 **[Session Hooks](hooks.html)** - All 6 hook types with inputs, outputs, and examples
- 📖 **[MCP Servers](mcp.html)** - Local and remote MCP server integration
- 📖 **[Setup & Deployment](setup.html)** - OAuth, backend services, scaling, configuration reference
- 📖 **[API Javadoc](apidocs/index.html)** - Complete API reference
