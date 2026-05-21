# Changelog

All notable changes to the GitHub Copilot SDK for Java will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

> Note: This file is automatically modified by scripts and coding agents. Do not change it manually.

## [Unreleased]

> **Reference implementation sync:** [`github/copilot-sdk@e20f5be`](https://github.com/github/copilot-sdk/commit/e20f5bef125860accb30c60d1b35109371a77f16)

## [1.0.0-beta-java.4] - 2026-05-16

> **Reference implementation sync:** [`github/copilot-sdk@e20f5be`](https://github.com/github/copilot-sdk/commit/e20f5bef125860accb30c60d1b35109371a77f16)
## [1.0.0-beta-java.3] - 2026-05-11

> **Reference implementation sync:** [`github/copilot-sdk@4a0437b`](https://github.com/github/copilot-sdk/commit/4a0437bb03a0b60a1867f14ae8e3faf053afa5aa)
## [1.0.0-beta-java.2] - 2026-05-08

> **Reference implementation sync:** [`github/copilot-sdk@066a69c`](https://github.com/github/copilot-sdk/commit/066a69c1e849adf1bd98564ab1b52316ec471182)
## [1.0.0-beta-java.1] - 2026-05-05

> **Reference implementation sync:** [`github/copilot-sdk@c063458`](https://github.com/github/copilot-sdk/commit/c063458ecc3d606766f04cf203b11b08de672cc8)
## [0.3.0-java.2] - 2026-04-26

> **Reference implementation sync:** [`github/copilot-sdk@dd2dcbc`](https://github.com/github/copilot-sdk/commit/dd2dcbc439256acfb9feb2cff07c0b9c820091b8)
## [0.3.0-java-preview.1] - 2026-04-21

> **Reference implementation sync:** [`github/copilot-sdk@922959f`](https://github.com/github/copilot-sdk/commit/922959f4a7b83509c3620d4881733c6c5677f00c)
## [0.3.0-java-preview.0] - 2026-04-21

> **Reference implementation sync:** [`github/copilot-sdk@c3fa6cb`](https://github.com/github/copilot-sdk/commit/c3fa6cbfb83d4a20b7912b1a17013d48f5a277a1)
## [0.2.2-java.1] - 2026-04-07

> **Reference implementation sync:** [`github/copilot-sdk@c3fa6cb`](https://github.com/github/copilot-sdk/commit/c3fa6cbfb83d4a20b7912b1a17013d48f5a277a1)
### Added

- Slash commands — register `/command` handlers invoked from the CLI TUI via `SessionConfig.setCommands()` (reference implementation: [`f7fd757`](https://github.com/github/copilot-sdk/commit/f7fd757))
- `CommandDefinition`, `CommandContext`, `CommandHandler`, `CommandWireDefinition` — types for defining and handling slash commands
- `CommandExecuteEvent` — event dispatched when a registered slash command is executed
- Elicitation (UI dialogs) — incoming handler via `SessionConfig.setOnElicitationRequest()` and outgoing convenience methods via `session.getUi()` (reference implementation: [`f7fd757`](https://github.com/github/copilot-sdk/commit/f7fd757))
- `ElicitationContext`, `ElicitationHandler`, `ElicitationParams`, `ElicitationResult`, `ElicitationResultAction`, `ElicitationSchema`, `InputOptions` — types for elicitation
- `ElicitationRequestedEvent` — event dispatched when an elicitation request is received
- `SessionUiApi` — convenience API on `session.getUi()` for `confirm()`, `select()`, `input()`, and `elicitation()` calls
- `SessionCapabilities` and `SessionUiCapabilities` — session capability reporting populated from create/resume response
- `CapabilitiesChangedEvent` — event dispatched when session capabilities are updated
- `CopilotClient.getSessionMetadata(String)` — O(1) session lookup by ID
- `GetSessionMetadataResponse` — response type for `getSessionMetadata`

### Fixed

- Permission events already resolved by a pre-hook now short-circuit before invoking the client-side handler
- `SessionUiApi` Javadoc now uses valid Java null-check syntax instead of `?.`
- README updated to say "GitHub Copilot CLI 1.0.17" instead of "GitHub Copilot 1.0.17"

## [0.2.1-java.1] - 2026-04-02

> **Reference implementation sync:** [`github/copilot-sdk@4088739`](https://github.com/github/copilot-sdk/commit/40887393a9e687dacc141a645799441b0313ff15)
## [0.2.1-java.0] - 2026-03-26

> **Reference implementation sync:** [`github/copilot-sdk@4088739`](https://github.com/github/copilot-sdk/commit/40887393a9e687dacc141a645799441b0313ff15)
### Added

- `UnknownSessionEvent` — forward-compatible placeholder for event types not yet known to the SDK; unknown events are now dispatched to handlers instead of being silently dropped (reference implementation: [`d82fd62`](https://github.com/github/copilot-sdk/commit/d82fd62))
- `PermissionRequestResultKind.NO_RESULT` — new constant that signals the handler intentionally abstains from answering a permission request, leaving it unanswered for another client (reference implementation: [`df59a0e`](https://github.com/github/copilot-sdk/commit/df59a0e))
- `ToolDefinition.skipPermission` field and `ToolDefinition.createSkipPermission()` factory — marks a tool to skip the permission prompt (reference implementation: [`10c4d02`](https://github.com/github/copilot-sdk/commit/10c4d02))
- `SystemMessageMode.CUSTOMIZE` — new enum value for fine-grained system prompt customization (reference implementation: [`005b780`](https://github.com/github/copilot-sdk/commit/005b780))
- `SectionOverrideAction` enum — specifies the operation on a system prompt section (replace, remove, append, prepend, transform) (reference implementation: [`005b780`](https://github.com/github/copilot-sdk/commit/005b780))
- `SectionOverride` class — describes how one section of the system prompt should be modified, with optional transform callback (reference implementation: [`005b780`](https://github.com/github/copilot-sdk/commit/005b780))
- `SystemPromptSections` constants — well-known section identifier strings for use with CUSTOMIZE mode (reference implementation: [`005b780`](https://github.com/github/copilot-sdk/commit/005b780))
- `SystemMessageConfig.setSections(Map<String,SectionOverride>)` — section-level overrides for CUSTOMIZE mode (reference implementation: [`005b780`](https://github.com/github/copilot-sdk/commit/005b780))
- `systemMessage.transform` RPC handler — the SDK now registers a handler that invokes transform callbacks registered in the session config (reference implementation: [`005b780`](https://github.com/github/copilot-sdk/commit/005b780))
- `CopilotSession.setModel(String, String)` — new overload that accepts an optional reasoning effort level (reference implementation: [`ea90f07`](https://github.com/github/copilot-sdk/commit/ea90f07))
- `CopilotSession.log(String, String, Boolean, String)` — new overload with an optional `url` parameter (minor addition)
- `BlobAttachment` class — inline base64-encoded binary attachment for messages (e.g., images) (reference implementation: [`698b259`](https://github.com/github/copilot-sdk/commit/698b259))
- `MessageAttachment` sealed interface — type-safe base for all attachment types (`Attachment`, `BlobAttachment`), with Jackson polymorphic serialization support
- `TelemetryConfig` class — OpenTelemetry configuration for the CLI server; set on `CopilotClientOptions.setTelemetry()` (reference implementation: [`f2d21a0`](https://github.com/github/copilot-sdk/commit/f2d21a0))
- `CopilotClientOptions.setTelemetry(TelemetryConfig)` — enables OpenTelemetry instrumentation in the CLI server (reference implementation: [`f2d21a0`](https://github.com/github/copilot-sdk/commit/f2d21a0))

### Changed

- `Attachment` record now implements `MessageAttachment` sealed interface
- `BlobAttachment` class now implements `MessageAttachment` sealed interface and is `final`
- `MessageOptions.setAttachments(List<? extends MessageAttachment>)` — parameter type changed from `List<Attachment>` to `List<? extends MessageAttachment>` to support both `Attachment` and `BlobAttachment` in the same list with full compile-time safety
- `SendMessageRequest.setAttachments(List<MessageAttachment>)` — matching change for the internal request type

### Deprecated

- `CopilotClientOptions.setAutoRestart(boolean)` — this option has no effect and will be removed in a future release

## [0.1.32-java.0] - 2026-03-17

> **Reference implementation sync:** [`github/copilot-sdk@062b61c`](https://github.com/github/copilot-sdk/commit/062b61c8aa63b9b5d45fa1d7b01723e6660ffa83)
## [1.0.11] - 2026-03-12

> **Reference implementation sync:** [`github/copilot-sdk@062b61c`](https://github.com/github/copilot-sdk/commit/062b61c8aa63b9b5d45fa1d7b01723e6660ffa83)
### Added

- `CopilotClientOptions.setOnListModels(Supplier<CompletableFuture<List<ModelInfo>>>)` — custom handler for `listModels()` used in BYOK mode to return models from a custom provider instead of querying the CLI (reference implementation: [`e478657`](https://github.com/github/copilot-sdk/commit/e478657))
- `SessionConfig.setAgent(String)` — pre-selects a custom agent by name when creating a session (reference implementation: [`7766b1a`](https://github.com/github/copilot-sdk/commit/7766b1a))
- `ResumeSessionConfig.setAgent(String)` — pre-selects a custom agent by name when resuming a session (reference implementation: [`7766b1a`](https://github.com/github/copilot-sdk/commit/7766b1a))
- `SessionConfig.setOnEvent(Consumer<AbstractSessionEvent>)` — registers an event handler before the `session.create` RPC is issued, ensuring no early events are missed (reference implementation: [`4125fe7`](https://github.com/github/copilot-sdk/commit/4125fe7))
- `ResumeSessionConfig.setOnEvent(Consumer<AbstractSessionEvent>)` — registers an event handler before the `session.resume` RPC is issued (reference implementation: [`4125fe7`](https://github.com/github/copilot-sdk/commit/4125fe7))
- New broadcast session event types (protocol v3): `ExternalToolRequestedEvent` (`external_tool.requested`), `ExternalToolCompletedEvent` (`external_tool.completed`), `PermissionRequestedEvent` (`permission.requested`), `PermissionCompletedEvent` (`permission.completed`), `CommandQueuedEvent` (`command.queued`), `CommandCompletedEvent` (`command.completed`), `ExitPlanModeRequestedEvent` (`exit_plan_mode.requested`), `ExitPlanModeCompletedEvent` (`exit_plan_mode.completed`), `SystemNotificationEvent` (`system.notification`) (reference implementation: [`1653812`](https://github.com/github/copilot-sdk/commit/1653812), [`396e8b3`](https://github.com/github/copilot-sdk/commit/396e8b3))
- `CopilotSession.log(String)` and `CopilotSession.log(String, String, Boolean)` — log a message to the session timeline (reference implementation: [`4125fe7`](https://github.com/github/copilot-sdk/commit/4125fe7))

### Changed

- **Protocol version bumped to v3.** The SDK now supports CLI servers running v2 or v3 (backward-compatible range). Sessions are now registered in the client's session map *before* the `session.create`/`session.resume` RPC is issued, ensuring broadcast events emitted immediately on session start are never dropped (reference implementation: [`4125fe7`](https://github.com/github/copilot-sdk/commit/4125fe7), [`1653812`](https://github.com/github/copilot-sdk/commit/1653812))
- In protocol v3, tool calls and permission requests that have a registered handler are now handled automatically via `ExternalToolRequestedEvent` and `PermissionRequestedEvent` broadcast events; results are sent back via `session.tools.handlePendingToolCall` and `session.permissions.handlePendingPermissionRequest` RPC calls (reference implementation: [`1653812`](https://github.com/github/copilot-sdk/commit/1653812))

## [1.0.10] - 2026-03-03

> **Reference implementation sync:** [`github/copilot-sdk@dcd86c1`](https://github.com/github/copilot-sdk/commit/dcd86c189501ce1b46b787ca60d90f3f315f3079)
### Added

- `CopilotSession.setModel(String)` — changes the model for an existing session mid-conversation; the new model takes effect for the next message, and conversation history is preserved (reference implementation: [`bd98e3a`](https://github.com/github/copilot-sdk/commit/bd98e3a))
- `ToolDefinition.createOverride(String, String, Map, ToolHandler)` — creates a tool definition that overrides a built-in CLI tool with the same name (reference implementation: [`f843c80`](https://github.com/github/copilot-sdk/commit/f843c80))
- `ToolDefinition` record now includes `overridesBuiltInTool` field; when `true`, signals to the CLI that the custom tool intentionally replaces a built-in (reference implementation: [`f843c80`](https://github.com/github/copilot-sdk/commit/f843c80))
- `CopilotSession.listAgents()` — lists custom agents available for selection (reference implementation: [`9d998fb`](https://github.com/github/copilot-sdk/commit/9d998fb))
- `CopilotSession.getCurrentAgent()` — gets the currently selected custom agent (reference implementation: [`9d998fb`](https://github.com/github/copilot-sdk/commit/9d998fb))
- `CopilotSession.selectAgent(String)` — selects a custom agent for the session (reference implementation: [`9d998fb`](https://github.com/github/copilot-sdk/commit/9d998fb))
- `CopilotSession.deselectAgent()` — deselects the current custom agent (reference implementation: [`9d998fb`](https://github.com/github/copilot-sdk/commit/9d998fb))
- `CopilotSession.compact()` — triggers immediate session context compaction (reference implementation: [`9d998fb`](https://github.com/github/copilot-sdk/commit/9d998fb))
- `AgentInfo` — new JSON type representing a custom agent with `name`, `displayName`, and `description` (reference implementation: [`9d998fb`](https://github.com/github/copilot-sdk/commit/9d998fb))
- New event types: `SessionTaskCompleteEvent` (`session.task_complete`), `AssistantStreamingDeltaEvent` (`assistant.streaming_delta`), `SubagentDeselectedEvent` (`subagent.deselected`) (reference implementation: various commits)
- `AssistantTurnStartEvent` data now includes `interactionId` field
- `AssistantMessageEvent` data now includes `interactionId` field
- `ToolExecutionCompleteEvent` data now includes `model` and `interactionId` fields
- `SkillInvokedEvent` data now includes `pluginName` and `pluginVersion` fields
- `AssistantUsageEvent` data now includes `copilotUsage` field with `CopilotUsage` and `TokenDetails` nested types
- E2E tests for custom tool permission approval and denial flows (reference implementation: [`388f2f3`](https://github.com/github/copilot-sdk/commit/388f2f3))

### Changed

- **Breaking:** `createSession(SessionConfig)` now requires a non-null `onPermissionRequest` handler; throws `IllegalArgumentException` if not provided (reference implementation: [`279f6c4`](https://github.com/github/copilot-sdk/commit/279f6c4))
- **Breaking:** `resumeSession(String, ResumeSessionConfig)` now requires a non-null `onPermissionRequest` handler; throws `IllegalArgumentException` if not provided (reference implementation: [`279f6c4`](https://github.com/github/copilot-sdk/commit/279f6c4))
- **Breaking:** The no-arg `createSession()` and `resumeSession(String)` overloads were removed (reference implementation: [`279f6c4`](https://github.com/github/copilot-sdk/commit/279f6c4))
- `AssistantMessageDeltaEvent` data: `totalResponseSizeBytes` field moved to new `AssistantStreamingDeltaEvent` (reference implementation: various)

### Fixed

- Permission checks now also apply to SDK-registered custom tools, invoking the `onPermissionRequest` handler with `kind="custom-tool"` before executing tools (reference implementation: [`388f2f3`](https://github.com/github/copilot-sdk/commit/388f2f3))

## [1.0.9] - 2026-02-16

> **Reference implementation sync:** [`github/copilot-sdk@e40d57c`](https://github.com/github/copilot-sdk/commit/e40d57c86e18b495722adbf42045288c03924342)
### Added

#### Cookbook with Practical Recipes

Added a comprehensive cookbook with 5 practical recipes demonstrating common SDK usage patterns. All examples are JBang-compatible and can be run directly without a full Maven project setup.

**Recipes:**
- **Error Handling** - Connection failures, timeouts, cleanup patterns, tool errors
- **Multiple Sessions** - Parallel conversations, custom session IDs, lifecycle management  
- **Managing Local Files** - AI-powered file organization with grouping strategies
- **PR Visualization** - Interactive CLI tool for analyzing PR age distribution via GitHub MCP Server
- **Persisting Sessions** - Save and resume conversations across restarts

**Location:** `src/site/markdown/cookbook/`

**Usage:**
```bash
jbang BasicErrorHandling.java
jbang MultipleSessions.java
jbang PRVisualization.java github/copilot-sdk
```

Each recipe includes JBang prerequisites, usage instructions, and best practices.

#### Session Context and Filtering

Added session context tracking and filtering capabilities to help manage multiple Copilot sessions across different repositories and working directories.

**New Classes:**
- `SessionContext` - Represents working directory context (cwd, gitRoot, repository, branch) with fluent setters
- `SessionListFilter` - Filter sessions by context fields (extends SessionContext)
- `SessionContextChangedEvent` - Event fired when working directory context changes between turns

**Updated APIs:**
- `SessionMetadata.getContext()` - Returns optional context information for persisted sessions
- `CopilotClient.listSessions(SessionListFilter)` - New overload to filter sessions by context criteria

**Example:**
```java
// List sessions for a specific repository
var filter = new SessionListFilter()
    .setRepository("owner/repo")
    .setBranch("main");
var sessions = client.listSessions(filter).get();

// Access context information
for (var session : sessions) {
    var ctx = session.getContext();
    if (ctx != null) {
        System.out.println("CWD: " + ctx.getCwd());
        System.out.println("Repo: " + ctx.getRepository());
    }
}

// Listen for context changes
session.on(SessionContextChangedEvent.class, event -> {
    SessionContext newContext = event.getData();
    System.out.println("Working directory changed to: " + newContext.getCwd());
});
```

**Requirements:**
- GitHub Copilot CLI 0.0.409 or later

## [1.0.8] - 2026-02-08

> **Reference implementation sync:** [`github/copilot-sdk@05e3c46`](https://github.com/github/copilot-sdk/commit/05e3c46c8c23130c9c064dc43d00ec78f7a75eab)

### Added

#### ResumeSessionConfig Parity with SessionConfig
Added missing options to `ResumeSessionConfig` for parity with `SessionConfig` when resuming sessions. You can now change the model, system message, tool filters, and other settings when resuming:

- `model` - Change the AI model when resuming
- `systemMessage` - Override or extend the system prompt
- `availableTools` - Restrict which tools are available
- `excludedTools` - Disable specific tools
- `configDir` - Override configuration directory
- `infiniteSessions` - Configure infinite session behavior

**Example:**
```java
var config = new ResumeSessionConfig()
    .setModel("claude-sonnet-4")
    .setReasoningEffort("high")
    .setSystemMessage(new SystemMessageConfig()
        .setMode(SystemMessageMode.APPEND)
        .setContent("Focus on security."));

var session = client.resumeSession(sessionId, config).get();
```

#### EventErrorHandler for Custom Error Handling
Added `EventErrorHandler` interface for custom handling of exceptions thrown by event handlers. Set via `session.setEventErrorHandler()` to receive the event and exception when a handler fails.

```java
session.setEventErrorHandler((event, exception) -> {
    logger.error("Handler failed for event: " + event.getType(), exception);
});
```

#### EventErrorPolicy for Dispatch Control
Added `EventErrorPolicy` enum to control whether event dispatch continues or stops when a handler throws an exception. Errors are always logged at `WARNING` level. The default policy is `PROPAGATE_AND_LOG_ERRORS` which stops dispatch on the first error. Set `SUPPRESS_AND_LOG_ERRORS` to continue dispatching despite errors:

```java
session.setEventErrorPolicy(EventErrorPolicy.SUPPRESS_AND_LOG_ERRORS);
```

The `EventErrorHandler` is always invoked regardless of the policy.

#### Type-Safe Event Handlers
Promoted type-safe `on(Class<T>, Consumer<T>)` event handlers as the primary API. Handlers now receive strongly-typed events instead of raw `AbstractSessionEvent`.

```java
session.on(AssistantMessageEvent.class, msg -> {
    System.out.println(msg.getData().getContent());
});
```

#### SpotBugs Static Analysis
Integrated SpotBugs for static code analysis with exclusion filters for `events` and `json` packages.

### Changed

- **Copilot CLI**: Minimum version updated to **0.0.405**
- **CopilotClient**: Made `final` to prevent Finalizer attacks (security hardening)
- **JBang Example**: Refactored `jbang-example.java` with streamlined session creation and usage metrics display
- **Code Style**: Use `var` for local variable type inference throughout the codebase

### Fixed

- **SpotBugs OS_OPEN_STREAM**: Wrap `BufferedReader` in try-with-resources to prevent resource leaks
- **SpotBugs REC_CATCH_EXCEPTION**: Narrow exception catch in `JsonRpcClient.handleMessage()`
- **SpotBugs DM_DEFAULT_ENCODING**: Add explicit UTF-8 charset to `InputStreamReader`
- **SpotBugs EI_EXPOSE_REP**: Add defensive copies to collection getters in events and JSON packages

## [1.0.7] - 2026-02-05

### Added

#### Session Lifecycle Hooks
Extended the hooks system with three new hook types for session lifecycle control:
- **`onSessionStart`** - Called when a session starts (new or resumed)
- **`onSessionEnd`** - Called when a session ends
- **`onUserPromptSubmitted`** - Called when the user submits a prompt

New types:
- `SessionStartHandler`, `SessionStartHookInput`, `SessionStartHookOutput`
- `SessionEndHandler`, `SessionEndHookInput`, `SessionEndHookOutput`
- `UserPromptSubmittedHandler`, `UserPromptSubmittedHookInput`, `UserPromptSubmittedHookOutput`

#### Session Lifecycle Events (Client-Level)
Added client-level lifecycle event subscriptions:
- `client.onLifecycle(handler)` - Subscribe to all session lifecycle events
- `client.onLifecycle(eventType, handler)` - Subscribe to specific event types
- `SessionLifecycleEventTypes.CREATED`, `DELETED`, `UPDATED`, `FOREGROUND`, `BACKGROUND`

New types: `SessionLifecycleEvent`, `SessionLifecycleEventMetadata`, `SessionLifecycleHandler`

#### Foreground Session Control (TUI+Server Mode)
For servers running with `--ui-server`:
- `client.getForegroundSessionId()` - Get the session displayed in TUI
- `client.setForegroundSessionId(sessionId)` - Switch TUI display to a session

New types: `GetForegroundSessionResponse`, `SetForegroundSessionResponse`

#### New Event Types
- **`SessionShutdownEvent`** - Emitted when session is shutting down, includes reason and exit code
- **`SkillInvokedEvent`** - Emitted when a skill is invoked, includes skill name and context

#### Extended Event Data
- `AssistantMessageEvent.Data` - Added `id`, `isLastReply`, `thinkingContent` fields
- `AssistantUsageEvent.Data` - Added `outputReasoningTokens` field
- `SessionCompactionCompleteEvent.Data` - Added `success`, `messagesRemoved`, `tokensRemoved` fields
- `SessionErrorEvent.Data` - Extended with additional error context

#### Documentation
- New **[hooks.md](src/site/markdown/hooks.md)** - Comprehensive guide covering all 5 session hooks with examples for security gates, logging, result enrichment, and lifecycle management
- Expanded **[documentation.md](src/site/markdown/documentation.md)** with all 33 event types, `getMessages()`, `abort()`, and custom timeout examples
- Enhanced **[advanced.md](src/site/markdown/advanced.md)** with session hooks, lifecycle events, and foreground session control
- Added **[.github/copilot-instructions.md](.github/copilot-instructions.md)** for AI assistants

#### Testing
- `SessionEventParserTest` - 850+ lines of unit tests for JSON event deserialization
- `SessionEventsE2ETest` - End-to-end tests for session event lifecycle
- `ErrorHandlingTest` - Tests for error handling scenarios
- Enhanced `E2ETestContext` with snapshot validation and expected prompt logging
- Added logging configuration (`logging.properties`)

#### Build & CI
- JaCoCo 0.8.14 for test coverage reporting
- Coverage reports generated at `target/site/jacoco-coverage/`
- New test report action at `.github/actions/test-report/`
- JaCoCo coverage summary in workflow summary
- Coverage report artifact upload

### Changed

- **Copilot CLI**: Minimum version updated from 0.0.400 to **0.0.404**
- Refactored `ProcessInfo` and `Connection` to use records
- Extended `SessionHooks` to support 5 hook types (was 2)
- Renamed test methods to match snapshot naming conventions with Javadoc

### Fixed

- Improved timeout exception handling with detailed logging
- Test infrastructure improvements for proxy resilience

## [1.0.6] - 2026-02-02

### Added

- Auth options for BYOK configuration (`authType`, `apiKey`, `organizationId`, `endpoint`)
- Reasoning effort configuration (`reasoningEffort` in session config)
- User input handler for freeform user prompts (`UserInputHandler`, `UserInputRequest`, `UserInputResponse`)
- Pre-tool use and post-tool use hooks (`PreToolUseHandler`, `PostToolUseHandler`)
- VSCode launch and debug configurations
- Logging configuration for test debugging

### Changed

- Enhanced permission request handling with graceful error recovery
- Updated test harness integration to clone from reference implementation SDK
- Improved logging for session events and user input requests

### Fixed

- Non-null answer enforcement in user input responses for CLI compatibility
- Permission handler error handling improvements

## [1.0.5] - 2026-01-29

### Added

- Skills configuration: `skillDirectories` and `disabledSkills` in `SessionConfig`
- Skill events handling (`SkillInvokedEvent`)
- Javadoc verification step in build workflow
- Deploy-site job for automatic documentation deployment after releases

### Changed

- Merged reference implementation SDK changes (commit 87ff5510)
- Added agentic-merge-reference-impl Claude skill for tracking reference implementation changes

### Fixed

- Resume session handling to keep first client alive
- Build workflow updated to use `test-compile` instead of `compile`
- NPM dependency installation in CI workflow
- Enhanced error handling in permission request processing
- Checkstyle and Maven Resources Plugin version updates
- Test harness CLI installation to match reference implementation version

## [1.0.4] - 2026-01-27

### Added

- Advanced usage documentation with comprehensive examples
- Getting started guide with Maven and JBang instructions
- Package-info.java files for `com.github.copilot.sdk`, `events`, and `json` packages
- `@since` annotations on all public classes
- Versioned documentation with version selector on GitHub Pages
- Maven resources plugin for site markdown filtering

### Changed

- Refactored tool argument handling for improved type safety
- Optimized event listener registration in examples
- Enhanced site navigation with documentation links
- Merged reference implementation SDK changes from commit f902b76

### Fixed

- BufferedReader replaced with BufferedInputStream for accurate JSON-RPC byte reading
- Timeout thread now uses daemon thread to prevent JVM exit blocking
- XML root element corrected from `<project>` to `<site>` in site.xml
- Badge titles in README for consistency

## [1.0.3] - 2026-01-26

### Added

- MCP Servers documentation and integration examples
- Infinite sessions documentation section
- Versioned documentation template with version selector
- Guidelines for porting reference implementation SDK changes to Java
- Configuration for automatically generated release notes

### Changed

- Renamed and retitled GitHub Actions workflows for clarity
- Improved gh-pages initialization and remote setup

### Fixed

- Documentation navigation to include MCP Servers section
- GitHub Pages deployment workflow to use correct branch
- Enhanced version handling in documentation build steps
- Rollback mechanism added for release failures

## [1.0.2] - 2026-01-25

### Added

- Infinite sessions support with `InfiniteSessionConfig` and workspace persistence
- GitHub Actions workflow for GitHub Pages deployment
- Daily schedule trigger for SDK E2E tests
- Checkstyle configuration and Maven integration

### Changed

- Updated GitHub Actions to latest action versions
- Enhanced Maven site deployment with documentation versioning
- Simplified GitHub release title naming convention

### Fixed

- Documentation links in site.xml and README for consistency
- Maven build step to include `clean` for fresh builds
- Image handling in README and site generation

## [1.0.1] - 2026-01-22

### Added

- Metadata APIs implementation
- Tool execution progress event (`ToolExecutionProgressEvent`)
- SDK protocol version 2 support
- Image in README for visual representation
- Detailed sections in README with usage examples
- Badges for build status, Maven Central, Java version, and license

### Changed

- Enhanced version handling in Maven release workflow
- Updated SCM connection URLs to use HTTPS

### Fixed

- GitHub release command version formatting and title
- Documentation commit messages to include version information
- JBang dependency declaration with correct group ID

## [1.0.0] - 2026-01-21

### Added

- Initial release of the GitHub Copilot SDK for Java
- Core classes: `CopilotClient`, `CopilotSession`, `JsonRpcClient`
- Session configuration with `SessionConfig`
- Custom tools with `ToolDefinition` and `ToolHandler`
- Event system with 30+ event types extending `AbstractSessionEvent`
- Permission handling with `PermissionHandler`
- BYOK (Bring Your Own Key) support with `ProviderConfig`
- MCP server integration via `McpServerConfig`
- System message customization with `SystemMessageConfig`
- File attachments support
- Streaming responses with delta events
- JBang example for quick testing
- GitHub Actions workflows for testing and Maven Central publishing
- Pre-commit hook for Spotless code formatting
- Comprehensive API documentation

[Unreleased]: https://github.com/github/copilot-sdk-java/compare/v1.0.0-beta-java.4...HEAD
[1.0.0-beta-java.4]: https://github.com/github/copilot-sdk-java/compare/v1.0.0-beta-java.3...v1.0.0-beta-java.4
[1.0.0-beta-java.3]: https://github.com/github/copilot-sdk-java/compare/v1.0.0-beta-java.2...v1.0.0-beta-java.3
[1.0.0-beta-java.2]: https://github.com/github/copilot-sdk-java/compare/v1.0.0-beta-java.1...v1.0.0-beta-java.2
[1.0.0-beta-java.1]: https://github.com/github/copilot-sdk-java/compare/v0.3.0-java.2...v1.0.0-beta-java.1
[0.3.0-java.2]: https://github.com/github/copilot-sdk-java/compare/v0.2.2-java.1...v0.3.0-java.2
[0.3.0-java-preview.1]: https://github.com/github/copilot-sdk-java/compare/v0.2.2-java.1...v0.3.0-java-preview.1
[0.3.0-java-preview.0]: https://github.com/github/copilot-sdk-java/compare/v0.2.2-java.1...v0.3.0-java-preview.0
[0.2.2-java.1]: https://github.com/github/copilot-sdk-java/compare/v0.2.1-java.1...v0.2.2-java.1
[0.2.1-java.1]: https://github.com/github/copilot-sdk-java/compare/v0.2.1-java.0...v0.2.1-java.1
[0.2.1-java.0]: https://github.com/github/copilot-sdk-java/compare/v0.1.32-java.0...v0.2.1-java.0
[0.1.32-java.0]: https://github.com/github/copilot-sdk-java/compare/v1.0.11...v0.1.32-java.0
[1.0.11]: https://github.com/github/copilot-sdk-java/compare/v1.0.10...v1.0.11
[1.0.10]: https://github.com/github/copilot-sdk-java/compare/v1.0.9...v1.0.10
[1.0.9]: https://github.com/github/copilot-sdk-java/compare/v1.0.8...v1.0.9
[1.0.8]: https://github.com/github/copilot-sdk-java/compare/v1.0.7...v1.0.8
[1.0.7]: https://github.com/github/copilot-sdk-java/compare/v1.0.6...v1.0.7
[1.0.6]: https://github.com/github/copilot-sdk-java/compare/v1.0.5...v1.0.6
[1.0.5]: https://github.com/github/copilot-sdk-java/compare/v1.0.4...v1.0.5
[1.0.4]: https://github.com/github/copilot-sdk-java/compare/v1.0.3...v1.0.4
[1.0.3]: https://github.com/github/copilot-sdk-java/compare/v1.0.2...v1.0.3
[1.0.2]: https://github.com/github/copilot-sdk-java/compare/v1.0.1...v1.0.2
[1.0.1]: https://github.com/github/copilot-sdk-java/compare/1.0.0...v1.0.1
[1.0.0]: https://github.com/github/copilot-sdk-java/releases/tag/1.0.0
