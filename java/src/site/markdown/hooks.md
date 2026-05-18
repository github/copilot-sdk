# Session Hooks

Session hooks allow you to intercept and modify tool execution, user prompts, and session lifecycle events. Use hooks to implement custom logic like logging, security controls, or context injection.

---

## Overview

| Hook | When It's Called | Can Modify |
|------|------------------|------------|
| [Pre-Tool Use](#Pre-Tool_Use_Hook) | Before a tool executes | Tool arguments, permission decision |
| [Post-Tool Use](#Post-Tool_Use_Hook) | After a tool executes | Tool result, additional context |
| [User Prompt Submitted](#User_Prompt_Submitted_Hook) | When user sends a message | Nothing (observation only) |
| [Session Start](#Session_Start_Hook) | When session begins | Nothing (observation only) |
| [Session End](#Session_End_Hook) | When session ends | Nothing (observation only) |
| [Checking Whether Hooks Are Registered](#Checking_Whether_Hooks_Are_Registered) | Before session creation | Whether any handlers are configured |

---

## Quick Start

Register hooks when creating a session:

```java
var hooks = new SessionHooks()
    .setOnPreToolUse((input, invocation) -> {
        System.out.println("Tool: " + input.getToolName());
        return CompletableFuture.completedFuture(PreToolUseHookOutput.allow());
    })
    .setOnPostToolUse((input, invocation) -> {
        System.out.println("Result: " + input.getToolResult());
        return CompletableFuture.completedFuture(null);
    });

var session = client.createSession(
    new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
        .setModel("gpt-4.1")
        .setHooks(hooks)
).get();
```

---

## Pre-Tool Use Hook

Called **before** a tool executes. Use this to:
- Approve, deny, or prompt for tool execution
- Modify tool arguments
- Add context for the LLM
- Suppress tool output from being shown

### Input

| Field | Type | Description |
|-------|------|-------------|
| `getSessionId()` | `String` | Runtime session ID of the session that triggered the hook |
| `getToolName()` | `String` | Name of the tool being called |
| `getToolArgs()` | `JsonNode` | Arguments passed to the tool |
| `getCwd()` | `String` | Current working directory |
| `getTimestamp()` | `long` | Timestamp in milliseconds |

### Output

| Field | Type | Description |
|-------|------|-------------|
| `setPermissionDecision(String)` | `"allow"`, `"deny"`, `"ask"` | Whether to execute the tool |
| `setPermissionDecisionReason(String)` | `String` | Reason shown to user/LLM |
| `setModifiedArgs(JsonNode)` | `JsonNode` | Modified arguments (optional) |
| `setAdditionalContext(String)` | `String` | Extra context for the LLM |
| `setSuppressOutput(Boolean)` | `Boolean` | Hide output from display |

### Example: Security Gate

Block dangerous tool calls:

```java
var hooks = new SessionHooks()
    .setOnPreToolUse((input, invocation) -> {
        String tool = input.getToolName();
        
        // Block file deletion
        if (tool.equals("delete_file")) {
            return CompletableFuture.completedFuture(
                PreToolUseHookOutput.deny("File deletion is not allowed")
            );
        }
        
        // Require confirmation for shell commands
        if (tool.equals("run_terminal_cmd")) {
            return CompletableFuture.completedFuture(PreToolUseHookOutput.ask());
        }
        
        // Allow everything else
        return CompletableFuture.completedFuture(PreToolUseHookOutput.allow());
    });
```

### Example: Argument Modification

Inject context into tool arguments:

```java
var hooks = new SessionHooks()
    .setOnPreToolUse((input, invocation) -> {
        if (input.getToolName().equals("search_code")) {
            // Add project root to search path
            var mapper = new ObjectMapper();
            var modifiedArgs = mapper.createObjectNode();
            modifiedArgs.put("path", "/my/project/src");
            modifiedArgs.set("query", input.getToolArgs().get("query"));
            
            return CompletableFuture.completedFuture(
                PreToolUseHookOutput.withModifiedArgs("allow", modifiedArgs)
            );
        }
        return CompletableFuture.completedFuture(PreToolUseHookOutput.allow());
    });
```

---

## Post-Tool Use Hook

Called **after** a tool executes. Use this to:
- Log tool results
- Modify the result shown to the LLM
- Add additional context based on results
- Suppress output from display

### Input

| Field | Type | Description |
|-------|------|-------------|
| `getSessionId()` | `String` | Runtime session ID of the session that triggered the hook |
| `getToolName()` | `String` | Name of the tool that was called |
| `getToolArgs()` | `JsonNode` | Arguments that were passed |
| `getToolResult()` | `JsonNode` | Result from the tool |
| `getCwd()` | `String` | Current working directory |
| `getTimestamp()` | `long` | Timestamp in milliseconds |

### Output

| Field | Type | Description |
|-------|------|-------------|
| `setModifiedResult(String)` | `String` | Modified result for the LLM |
| `setAdditionalContext(String)` | `String` | Extra context for the LLM |
| `setSuppressOutput(Boolean)` | `Boolean` | Hide output from display |

### Example: Result Logging

Log all tool executions:

```java
var hooks = new SessionHooks()
    .setOnPostToolUse((input, invocation) -> {
        System.out.printf("[%d] %s completed%n", 
            input.getTimestamp(), 
            input.getToolName());
        System.out.println("Result: " + input.getToolResult());
        
        return CompletableFuture.completedFuture(null);
    });
```

### Example: Result Enrichment

Add context to file read results:

```java
var hooks = new SessionHooks()
    .setOnPostToolUse((input, invocation) -> {
        if (input.getToolName().equals("read_file")) {
            String context = "Note: This file was last modified 2 hours ago.";
            return CompletableFuture.completedFuture(
                new PostToolUseHookOutput(null, context, null)
            );
        }
        return CompletableFuture.completedFuture(null);
    });
```

---

## User Prompt Submitted Hook

Called when the user submits a prompt, before the LLM processes it. This is an observation hook - you cannot modify the prompt.

### Input

| Field | Type | Description |
|-------|------|-------------|
| `sessionId()` | `String` | Runtime session ID of the session that triggered the hook |
| `prompt()` | `String` | The user's prompt text |
| `timestamp()` | `long` | Timestamp in milliseconds |

### Output

Return `null` - this hook is observation-only.

### Example: Prompt Logging

```java
var hooks = new SessionHooks()
    .setOnUserPromptSubmitted((input, invocation) -> {
        System.out.println("User asked: " + input.prompt());
        
        // Track prompts for analytics
        analytics.track("user_prompt", Map.of(
            "sessionId", invocation.getSessionId(),
            "promptLength", input.prompt().length()
        ));
        
        return CompletableFuture.completedFuture(null);
    });
```

---

## Session Start Hook

Called when a session starts (either new or resumed).

### Input

| Field | Type | Description |
|-------|------|-------------|
| `sessionId()` | `String` | Runtime session ID of the session that triggered the hook |
| `source()` | `String` | `"startup"`, `"resume"`, or `"new"` |
| `timestamp()` | `long` | Timestamp in milliseconds |

### Output

Return `null` - this hook is observation-only.

### Example: Session Initialization

```java
var hooks = new SessionHooks()
    .setOnSessionStart((input, invocation) -> {
        System.out.println("Session started: " + invocation.getSessionId());
        System.out.println("Source: " + input.source());
        
        // Initialize session-specific resources
        sessionResources.put(invocation.getSessionId(), new ResourceManager());
        
        return CompletableFuture.completedFuture(null);
    });
```

---

## Session End Hook

Called when a session ends.

### Input

| Field | Type | Description |
|-------|------|-------------|
| `sessionId()` | `String` | Runtime session ID of the session that triggered the hook |
| `reason()` | `String` | Why the session ended |
| `timestamp()` | `long` | Timestamp in milliseconds |

### Output

Return `null` - this hook is observation-only.

### Example: Session Cleanup

```java
var hooks = new SessionHooks()
    .setOnSessionEnd((input, invocation) -> {
        System.out.println("Session ended: " + input.reason());
        
        // Clean up session resources
        var resources = sessionResources.remove(invocation.getSessionId());
        if (resources != null) {
            resources.close();
        }
        
        return CompletableFuture.completedFuture(null);
    });
```

---

## Complete Example

Combining multiple hooks for comprehensive session control:

```java
import com.github.copilot.sdk.CopilotClient;
import com.github.copilot.sdk.json.PermissionHandler;
import com.github.copilot.sdk.json.PreToolUseHookOutput;
import com.github.copilot.sdk.json.SessionConfig;
import com.github.copilot.sdk.json.SessionHooks;
import java.util.concurrent.CompletableFuture;

public class HooksExample {
    public static void main(String[] args) throws Exception {
        try (var client = new CopilotClient()) {
            client.start().get();
            
            var hooks = new SessionHooks()
                // Security: control tool execution
                .setOnPreToolUse((input, invocation) -> {
                    System.out.println("→ " + input.getToolName());
                    
                    // Deny dangerous operations
                    if (input.getToolName().contains("delete")) {
                        return CompletableFuture.completedFuture(
                            PreToolUseHookOutput.deny("Deletion not allowed")
                        );
                    }
                    
                    return CompletableFuture.completedFuture(PreToolUseHookOutput.allow());
                })
                
                // Logging: track tool results
                .setOnPostToolUse((input, invocation) -> {
                    System.out.println("← " + input.getToolName() + " completed");
                    return CompletableFuture.completedFuture(null);
                })
                
                // Analytics: track user prompts
                .setOnUserPromptSubmitted((input, invocation) -> {
                    System.out.println("User: " + input.prompt());
                    return CompletableFuture.completedFuture(null);
                })
                
                // Lifecycle: initialization and cleanup
                .setOnSessionStart((input, invocation) -> {
                    System.out.println("Session started (" + input.source() + ")");
                    return CompletableFuture.completedFuture(null);
                })
                .setOnSessionEnd((input, invocation) -> {
                    System.out.println("Session ended: " + input.reason());
                    return CompletableFuture.completedFuture(null);
                });
            
            var session = client.createSession(
                new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                    .setModel("gpt-4.1")
                    .setHooks(hooks)
            ).get();
            
            var response = session.sendAndWait("List files in /tmp").get();
            System.out.println(response.getData().content());
            
            session.close();
        }
    }
}
```

---

## Hook Invocation Object

All hook handlers receive a `HookInvocation` object as the second parameter:

| Method | Description |
|--------|-------------|
| `getSessionId()` | The session ID where the hook was triggered |

This allows you to correlate hooks with specific sessions when managing multiple concurrent sessions.

## Checking Whether Hooks Are Registered

Use `hasHooks()` to quickly verify that at least one hook handler is configured:

```java
var hooks = new SessionHooks()
    .setOnPreToolUse((input, invocation) ->
        CompletableFuture.completedFuture(PreToolUseHookOutput.allow()));

if (hooks.hasHooks()) {
    var session = client.createSession(
        new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL).setHooks(hooks)
    ).get();
}
```

---

## Error Handling

If a hook throws an exception, the SDK logs the error and continues with default behavior:
- Pre-tool hooks default to allowing execution
- Post-tool hooks have no effect on the result
- Lifecycle hooks are observation-only

To handle errors gracefully in your hooks:

```java
.setOnPreToolUse((input, invocation) -> {
    try {
        // Your logic here
        return CompletableFuture.completedFuture(PreToolUseHookOutput.allow());
    } catch (Exception e) {
        logger.error("Hook error", e);
        // Fail-safe: deny if something goes wrong
        return CompletableFuture.completedFuture(
            PreToolUseHookOutput.deny("Internal error")
        );
    }
})
```

---

## See Also

- [SessionHooks Javadoc](apidocs/com/github/copilot/sdk/json/SessionHooks.html)
- [PreToolUseHookInput Javadoc](apidocs/com/github/copilot/sdk/json/PreToolUseHookInput.html)
- [PreToolUseHookOutput Javadoc](apidocs/com/github/copilot/sdk/json/PreToolUseHookOutput.html)
- [PostToolUseHookInput Javadoc](apidocs/com/github/copilot/sdk/json/PostToolUseHookInput.html)
- [PostToolUseHookOutput Javadoc](apidocs/com/github/copilot/sdk/json/PostToolUseHookOutput.html)

---

## Next Steps

- 📖 **[Documentation](documentation.html)** - Core concepts, events, session management
- 📖 **[Advanced Usage](advanced.html)** - Tools, BYOK, MCP Servers, Custom Agents
- 📖 **[MCP Servers](mcp.html)** - Integrate external tools via Model Context Protocol
- 📖 **[API Javadoc](apidocs/index.html)** - Complete API reference
