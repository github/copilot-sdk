# GitHub Copilot SDK for Java

[![Build](https://github.com/github/copilot-sdk/actions/workflows/sdk.yml/badge.svg)](https://github.com/github/copilot-sdk/actions/workflows/sdk.yml)
[![Java 17+](https://img.shields.io/badge/Java-17%2B-blue?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

#### Latest release

[![GitHub Release Date](https://img.shields.io/github/release-date/github/copilot-sdk)](https://github.com/github/copilot-sdk/releases)
[![GitHub Release](https://img.shields.io/github/v/release/github/copilot-sdk)](https://github.com/github/copilot-sdk/releases)
[![Maven Central](https://img.shields.io/maven-central/v/com.github/copilot-sdk-java)](https://central.sonatype.com/artifact/com.github/copilot-sdk-java)
[![Javadoc](https://javadoc.io/badge2/com.github/copilot-sdk-java/javadoc.svg?q=1)](https://javadoc.io/doc/com.github/copilot-sdk-java/latest/index.html)

## Background

Java SDK for programmatic control of GitHub Copilot CLI, enabling you to build AI-powered applications and agentic workflows. The Java SDK tracks the official GitHub Copilot SDK family (TypeScript, Python, Go, .NET, and Rust).

The generated `SessionFsSetProviderCapabilities.binary` field describes the runtime protocol, not a Java provider API. The Java SDK cannot currently register a session filesystem provider or handle `sessionFs.readFileBytes` or `sessionFs.writeFileBytes`; do not advertise binary operations from Java. Java provider and binary support is deferred until the SDK exposes provider registration and reverse-RPC dispatch. Provider-only images currently require a provider-capable SDK.

## Prerequisites

To use the SDK, you'll need:

- Java 17 or later. **JDK 25 recommended**. The distributed jar is a multi-release jar (MR-JAR) and is compiled on JDK 25 with `maven.compiler.release` set to 17. This means, when run on JDK 25 and later, the SDK automatically uses virtual threads for its default internal executor.

Managed stdio and TCP connections materialize the platform classifier's
`copilot-runtime[.exe]` and adjacent `runtime.node` by default. An explicit
`cliPath` or `COPILOT_CLI_PATH` environment variable overrides the bundled
runtime.

## Installation

Stable releases are available from Maven Central. Preview versions use Maven `-SNAPSHOT` coordinates and the repository shown in [Snapshot builds](#snapshot-builds).

The SDK starts the Copilot runtime from a separate JAR for each platform, which `copilot-sdk-java` does not pull in.
Add `com.github:copilot-sdk-java-runtime` at the same version as the SDK, with the classifier for the platform the application runs on: `linux-x64`, `linux-arm64`, `linuxmusl-x64`, `linuxmusl-arm64`, `darwin-x64`, `darwin-arm64`, `win32-x64` or `win32-arm64`.
In Gradle that is `implementation 'com.github:copilot-sdk-java-runtime:<version>:<classifier>'`; the Maven form is shown under [Additional dependency](#additional-dependency).
Without it, `CopilotClient.start()` fails with a `FileNotFoundException` unless `cliPath` or `COPILOT_CLI_PATH` names a Copilot CLI to launch instead.

### Maven

```xml
<dependency>
    <groupId>com.github</groupId>
    <artifactId>copilot-sdk-java</artifactId>
    <version>1.0.28</version>
</dependency>
```

### Gradle

```groovy
implementation 'com.github:copilot-sdk-java:1.0.28'
```

### Snapshot builds

Java previews are published three times per weekday, at 10:00, 13:00, and 15:00 Pacific, through the matched SDK release pipeline. Previews with the same base SDK version update one rolling Maven coordinate: SDK `1.0.15-preview.1`, `1.0.15-preview.2`, and later `1.0.15` previews all publish Java `1.0.15-SNAPSHOT`. To use them, add the snapshot repository and depend on that version:

#### Maven

```xml
<repositories>
    <repository>
        <id>central-snapshots</id>
        <url>https://central.sonatype.com/repository/maven-snapshots/</url>
        <releases><enabled>false</enabled></releases>
        <snapshots><enabled>true</enabled></snapshots>
    </repository>
</repositories>

<dependency>
    <groupId>com.github</groupId>
    <artifactId>copilot-sdk-java</artifactId>
    <version>1.0.29-SNAPSHOT</version>
</dependency>
```

#### Gradle

```groovy
repositories {
    mavenCentral()
    maven {
        url = uri('https://central.sonatype.com/repository/maven-snapshots/')
        mavenContent { snapshotsOnly() }
    }
}
```

Then add the snapshot dependency:

```groovy
implementation 'com.github:copilot-sdk-java:1.0.29-SNAPSHOT'
```

## In-process mode (experimental)

The SDK supports running the Copilot runtime **in-process** as a native library instead of spawning a separate CLI process. This eliminates process management overhead and simplifies deployment. In-process mode is currently experimental and supported on **linux-x64** (glibc), **linux-arm64** (glibc), **linuxmusl-x64**, **linuxmusl-arm64**, **win32-x64**, **win32-arm64**, **darwin-x64**, and **darwin-arm64**.

Because in-process mode is experimental, see the [Using experimental APIs](#using-experimental-apis) section for how to opt in.

### Additional dependency

Add both the SDK and the platform-specific native runtime to your project:

```xml
<dependencies>
    <!-- Pure-Java SDK -->
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
    <!-- Use linux-arm64, linuxmusl-x64, linuxmusl-arm64, win32-x64, win32-arm64, darwin-x64, or darwin-arm64 on those target platforms -->
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

For the native standalone runtime, include
`"COPILOT_TELEMETRY_DISABLED", "true"` in the environment map passed to
`CopilotClientOptions.setEnvironment(...)` before startup to disable Copilot-owned
GitHub and Entra authentication telemetry process-wide, including sessionless events.
Authentication, `setOnGitHubTelemetry(...)` forwarding, and user-configured
OpenTelemetry export remain unchanged. This is separate from
`SessionConfig.setEnableSessionTelemetry(false)`, which disables only a session's
pipeline. The value must be exactly `"true"`; changing it requires a process
restart. `COPILOT_GITHUB_TELEMETRY_DISABLED` remains native-runtime/GitHub-only. In-process connections
reject per-client environment maps; set the variable in the host process
environment before starting the runtime instead.

## Quick Start

For experimental in-process AHP hosting, select a transport explicitly:
`client.startAhpHost(new AhpHostOptions().setAuthentication(AhpHostAuthentication.github()).setLocalServer(HostLocalServerOptions.builder().build()))`.
Admission is explicit in every client mode: select `github()`, `none()`, or
`custom("stable-owner", request -> CompletableFuture.completedFuture(new AhpAuthenticationResult("stable-owner", "2099-01-01T00:00:00Z")))`.
Custom validators receive the decrypted admission token and a cancellation
stage. Return a matching identity and required future RFC3339 `expiresAt`, or
reject with null/an exceptional future. Cancellation completes on timeout,
disconnect, or validation completion; late results cannot admit a client.
`none()` does not disable the independent WebSocket connection secret.
Mission Control requires `github()`. Create/resume callbacks receive trusted
`request.authentication()` separately from `request.config()`; custom tokens
never become GitHub or model-provider credentials.
The transport types are in `com.github.copilot.generated.rpc`. For GitHub Mission
Control, use `.setGithubEnvironment(HostGitHubEnvironmentOptions.builder().name("My host").computeId("compute-id").build())`
instead, or configure both transports. GitHub environment name and compute ID are
required; there is no implicit local listener. The host's `getUrl()`, `getToken()`,
and `getPid()` may return `null`; `getEnvironmentId()` returns the GitHub environment
ID when configured. Environment list/get/delete operations are available only
through the generated RPC API. Call `host.listSessions()` to read the complete
advertised catalog of live and dormant sessions.
Use `AhpHostOptions.setComputeId(...)` to reuse the durable catalog across local
and Mission Control hosting; it must agree with the GitHub environment's compute
ID when both are supplied. The runtime persists an omitted local-only identity.
Publication survives host shutdown; dormant application sessions require your resume callback.
See [runtime-supervised AHP hosting](../docs/runtime-supervised-host.md) for creation
and resume callbacks, resident-session publication, ownership, and shared-snapshot E2Es.

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

`client.forceStop()` retires tracked sessions locally without `session.detach`
RPCs. Retained session objects reject new work and subscriptions, unused handler
captures are released, and pending plain and structured response waits fail.
Already-admitted event handlers and response continuations are not joined.
Forced retirement also drops callbacks when a graceful `session.close()` is
already awaiting detach. Graceful close fails pending response waits without
joining their continuations; ordinary callbacks remain active until detach completes.
Plain response waits preserve terminal results and errors already received before
retirement, even when a blocked continuation delays forwarding them to the caller.
Graceful owned-process shutdown over stdio waits for the runtime transport and
launcher to exit. Over TCP, it closes the connection and terminates the owned
process after the shutdown acknowledgment, without waiting for peer EOF.
Both preserve intentionally detached services; failed or forced shutdown retains
process-tree cleanup.
For a custom TCP launcher that spawns the runtime as a child, graceful cleanup
terminates only the launcher. The launcher must supervise and reap its runtime
child; a shutdown acknowledgment alone does not prove that child exited.

When targeting MCP tools configured through `setMcpServers(...)`, remember the
runtime tool name is `<server-key>-<tool-name>`. For `setAvailableTools(...)`
and `setExcludedTools(...)`, prefer the source-qualified filter form
`mcp:<server-key>-<tool-name>`. For `CustomAgentConfig.setTools(...)` and
`DefaultAgentConfig.setExcludedTools(...)`, use `<server-key>-<tool-name>`
directly.

`CopilotClientOptions.setCwd(...)` sets the runtime process working directory, which otherwise inherits the current process working directory. `SessionConfig.setWorkingDirectory(...)` sets the session working directory, which otherwise defaults to the runtime process working directory.

### Subagent lifecycle hooks

Register `setOnSubagentStart` and `setOnSubagentStop` on `SessionHooks` to inspect
the parent session and subagent metadata around each child run:

```java
import com.github.copilot.rpc.PermissionHandler;
import com.github.copilot.rpc.SessionConfig;
import com.github.copilot.rpc.SessionHooks;
import com.github.copilot.rpc.SubagentStartHookOutput;
import com.github.copilot.rpc.SubagentStopHookOutput;
import java.util.concurrent.CompletableFuture;

var hooks = new SessionHooks()
    .setOnSubagentStart((input, invocation) ->
        CompletableFuture.completedFuture(
            new SubagentStartHookOutput("Check the relevant tests first.")))
    .setOnSubagentStop((input, invocation) ->
        CompletableFuture.completedFuture(
            new SubagentStopHookOutput(null, null, input.response())));
var config = new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL).setHooks(hooks);
```

`SubagentStartHookInput` includes the parent `sessionId`, `timestamp`, `cwd`,
`transcriptPath`, `agentName`, and optional display name and description. Its
`additionalContext` output is prepended to the child's first prompt.
`SubagentStopHookInput` also includes `agentType`, optional `agentId`,
`stopReason`, and the last assistant `response`. Return a stop output with
`decision` set to `"block"` and a nonempty `reason` to request another child
turn, or set `modifiedResponse` to replace the final response. A block takes
precedence over a rewrite.

`CopilotClientOptions.setExtensionLaunchProvider(...)` configures an experimental
connection-level resolver for extension launch profiles. The client installs the
reverse-RPC handler and registers the provider during startup before sessions can
be created.

### Installation confirmation (experimental)

`CopilotClientOptions.setInstallationConfirmationHandler(...)` configures the
connection-global `installations.confirm` receiver. The handler receives the
generated `InstallationConfirmationRequest` and an
`InstallationConfirmationContext`, then returns only an explicit
generated `InstallationDecision.CONFIRM`, `InstallationDecision.DECLINE` or
`InstallationDecision.CANCEL`. The SDK echoes the original challenge and review
fingerprint; it never infers approval and does not enable installation
capabilities or call a runtime registration RPC.

Match `operationId` and `policySessionId` against the original action on this
exact connection before presenting the complete review. Missing legacy session
metadata does not select a default session. Refuse unknown operations or
incomplete reviews.

Concurrent reviews are independent and do not block other client-global RPCs.
`context.getCancelled()` returns the single cancellation signal for the review.
It completes when the runtime's numeric `$/cancelRequest`, runtime-enforced
expiry, or loss of the original connection retires the review. Separately
spawned UI work should observe this signal and retire itself when it completes.
Dropping an outbound installation or OAuth future does not cancel that
operation.

### Network callbacks (experimental)

Use `CopilotClientOptions.setNetwork(NetworkConfig)` when the SDK host should
take over parts of the runtime's networking, for example when the runtime runs
in WSL or a container and should use VS Code's proxy settings and the host's
Kerberos tickets. Registration happens during `start()`, before sessions can be
created, and startup fails if it is rejected. Without this option, or for a
callback you leave unset, the runtime handles that part itself. Referencing
`NetworkConfig` requires opting in to
[experimental APIs](#using-experimental-apis).

- `setResolveProxy(...)` decides the proxies for every request the runtime would
  otherwise route by ambient proxy settings: proxy environment variables,
  `NO_PROXY` and operating-system proxy settings are not consulted unless it
  answers `null`. The callback receives the generated
  `NetworkResolveProxyRequest`, whose `url()` is the request target as a PAC
  script sees it: without credentials or fragment, and for `https` and `wss`
  also without path and query. Return a `CompletableFuture<String>` with a
  PAC-style list such as `PROXY proxy.example:8080; HTTPS proxy.example:443; DIRECT`
  (the format of Electron's `session.resolveProxy`); answer `DIRECT` for a
  direct connection, or `null` to let the runtime resolve the proxy itself, as
  if no callback were registered. Entry types the runtime doesn't support, such
  as SOCKS and QUIC, are skipped. If the callback
  fails, the request fails rather than connecting directly. Requests to loopback
  targets, and requests with an explicitly configured proxy, never consult it.
  The runtime caches answers, including `null`, for 5 seconds and waits at most
  10 seconds for the callback.
- `setAuthenticateNegotiate(...)` supplies the Kerberos token for a proxy's
  Negotiate challenge instead of the runtime's native credential lookup. The
  callback receives the generated `NetworkAuthenticateNegotiateRequest`, whose
  `spn()` is a service principal in the platform-independent
  `service/host[@REALM]` form; validate it before issuing credentials. Return a
  `CompletableFuture<String>` with a fresh base64 SPNEGO token without the
  `Negotiate ` prefix, or `null` when credentials are unavailable, which fails
  the request. The runtime waits at most 30 seconds for the callback (less when
  the request's own deadline is sooner). Tokens are not cached and must not be
  logged or persisted. The callback supplies the initial SPNEGO token only;
  exchanges requiring continuation tokens are not supported. Proxies chosen by
  `setResolveProxy(...)` are authenticated the same way.

The callbacks run on the client executor; if the executor rejects one, the
request fails. When the runtime stops waiting (it cancels the request, for
example after its timeout, or the connection closes), a queued invocation is
skipped, the returned future is cancelled, and any later result is ignored.

By default the callbacks serve only requests made for sessions this client
creates or resumes. With `setScope(NetworkScope.RUNTIME)` they also serve the
runtime's shared background traffic, such as catalog refreshes and telemetry;
only one connection at a time can hold that scope, so startup fails if another
client holds it. The registration ends when the connection closes.

```java
import com.github.copilot.generated.rpc.NetworkScope;
import com.github.copilot.rpc.CopilotClientOptions;
import com.github.copilot.rpc.NetworkConfig;

var options = new CopilotClientOptions().setNetwork(new NetworkConfig()
        .setScope(NetworkScope.RUNTIME)
        .setResolveProxy(request -> resolveProxyFor(request.url()))
        .setAuthenticateNegotiate(request -> acquireSpnegoToken(request.spn())));
```

`SessionConfig.setAutopilot(AutopilotOptions)` and the corresponding
`ResumeSessionConfig` setter configure experimental Autopilot policy.
`continuationEnabled` defaults to true and does not change session mode.
`maxContinues` limits continuations outside an active objective: a non-negative
integer, default five, with zero allowing none. Options updates and warm resumes
preserve omitted fields; an empty
policy changes nothing. Re-supply on cold resume. Pause state, prompt overrides,
and no-progress detection remain internal.

`SessionConfig.setAskUserVariant(AskUserVariant.ELICITATION)` selects the
structured form-based `ask_user` tool when an elicitation handler is also set.
The default is `AskUserVariant.LEGACY`. Re-supply the option and handler through
`ResumeSessionConfig` on a cold resume.

`ResumeSessionConfig.setContinuePendingWork(false)` interrupts work still in
flight when resuming (the default), while preserving completed tool results
already durably recorded by the runtime.
Pass `new ResumeSessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL).setContinuePendingWork(true)`
to `resumeSession` to keep waiting for pending tool calls and permission requests
instead. Leave the option unset to use the runtime default. Re-register any
external tools needed to handle continued work.

`SessionConfig.setInteractiveUser(true)` declares that a person types the
session's messages, as in an editor chat panel. This enables the runtime's
user-message analysis telemetry for user-authored messages. Automated messages
stay excluded. The default is off, and cloud sessions ignore it. Re-supply it
through `ResumeSessionConfig` on a cold resume.

`SessionConfig.setPermissionMode(PermissionMode.ALLOW_ALL)` applies a permission
mode before the session's first turn, and `ResumeSessionConfig` applies it
before a resume continues pending work. It uses the same checks as
`session.permissions.setMode`, and `CopilotSession.getPermissionModeResult()`
reports whether it was applied and the authoritative mode. Cloud sessions
reject it.

For rotating per-session GitHub credentials, use
`SessionConfig.setGitHubTokenProvider(...)` (or the equivalent
`ResumeSessionConfig` setter) instead of `setGitHubToken(...)`:

```java
var config = new SessionConfig()
    .setGitHubTokenProvider(args ->
        acquireForHost(args.host()).thenApply(token ->
            GitHubTokenProviderResult.token(token, 8 * 60 * 60)))
    .setOnPermissionRequest(PermissionHandler.APPROVE_ALL);
```

The remaining lifetime is required and must be positive when the callback
completes; production GitHub tokens typically last eight hours. A static token
and a provider are mutually exclusive.

Initial acquisition runs during session creation or resume. Cancellation,
provider errors, and invalid token responses reject that operation instead of
falling back to ambient authentication. Idle sessions refresh only before their
next credential-consuming operation; there is no background refresh timer.

### Skill providers (experimental)

`SessionConfig.setSkillProvider(...)` registers session-scoped skills that the
runtime can list and read on demand. The API is experimental; see
[Using experimental APIs](#using-experimental-apis) before compiling code that
references it.

```java
import com.github.copilot.SkillProvider;
import com.github.copilot.SkillProviderDescriptor;
import com.github.copilot.rpc.PermissionHandler;
import com.github.copilot.rpc.SessionConfig;
import java.util.List;
import java.util.concurrent.CompletableFuture;

SkillProvider provider = new SkillProvider() {
    @Override
    public CompletableFuture<List<SkillProviderDescriptor>> listSkills() {
        return CompletableFuture.completedFuture(List.of(
            new SkillProviderDescriptor("team-plan", "Team planning guidance", true, false, null)));
    }

    @Override
    public CompletableFuture<String> readSkill(String name) {
        if (!name.equals("team-plan")) {
            return CompletableFuture.completedFuture(null); // not found
        }
        return CompletableFuture.completedFuture("# Team plan\nWrite a concise monthly team plan.");
    }
};

var config = new SessionConfig()
    .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
    .setEnableSkills(true)
    .setSkillProvider(provider);
```

Provider callbacks may be invoked concurrently, so implementations should be
thread-safe. When the runtime cancels a call, for example after its 30-second
limit or when the session disconnects, the SDK calls `cancel(true)` on the
returned future; check `isCancelled()` or attach a completion callback to stop
early. The provider is not persisted with the session; pass it again via
`ResumeSessionConfig.setSkillProvider(...)` on every resume. Resuming without a
provider unbinds any provider the session had. In
`CopilotClientMode.EMPTY`, explicitly set `setEnableSkills(true)` if the
assistant should use skills, because empty mode disables skill loading by
default. Skill providers are local-session only and are rejected for cloud
sessions.

#### Supporting files (experimental)

To give a provider skill supporting files, such as scripts, references,
templates, and assets, set its descriptor's `directory` to the absolute path of
the local folder that holds them:

```java
new SkillProviderDescriptor("release-notes", "Writes release notes in the team's format.",
    null, null, null, "/srv/skills/release-notes")
```

When the skill is activated, the runtime lists the folder's files for the model.
See [Supporting files](../docs/features/skills.md#supporting-files-experimental) for details.

### Typed MCP installation and removal payloads (breaking change)

Three payloads in the experimental MCP installation and removal workflow are now sealed
interfaces with one record per variant, instead of `Object`, which brings Java into line
with the other SDKs. No other generated type changes.

| Field | Before | After |
| --- | --- | --- |
| `InstallationConfirmationRequest.review()` | `Object` | `InstallationReview` sealed interface (`InstallationReviewMcp` / `InstallationReviewSkill`, by `resource`); the MCP variant carries `McpInstallationReview` (`McpInstallationReviewInstall` / `McpInstallationReviewUninstall`, by `action`) and the Skill variant carries `SkillInstallationReview` |
| `McpInstallPlan.transportChoices()` | `List<Object>` | `List<McpPlanTransportChoice>` (`McpPlanTransportChoicePackage` / `McpPlanTransportChoiceRemote`, by `installMethod`) |
| `McpInstallationManagementResultOutcome.getOutcome()` / `setOutcome(...)` | `Object` | `McpInstallationManagementOutcome`, whose operation variant carries `McpInstallationOperationStatus` (by `phase`) |

## System Message Customization

Use `SessionConfig.setSystemMessage(...)` with a `SystemMessageConfig` in `SystemMessageMode.CUSTOMIZE` to override individual prompt sections. Section keys are defined by `SystemMessageSections`; use `SectionOverride.setAction(...)` for static overrides or `SectionOverride.setTransform(...)` for a transform callback.

`SystemMessageSections.LAST_INSTRUCTIONS` (`last_instructions`) includes configured subagent-model guidance when the `task` tool is available. Removing or replacing this section also removes that guidance; a transform callback receives the complete section, including the guidance, and its returned content is authoritative. Append, prepend, and preserve retain their usual section semantics. These overrides change prompt prose only, not configured subagent models, tool availability, or runtime dispatch policy. `SystemMessageSections.RUNTIME_INSTRUCTIONS` is a separate section: removing it does not remove `SystemMessageSections.LAST_INSTRUCTIONS`.

## Message source

Use `MessageSource.SYSTEM` for application-generated system context and
`MessageSource.agent(id)` for messages from an identified agent:

```java
import com.github.copilot.rpc.MessageOptions;
import com.github.copilot.rpc.MessageSource;

session.send(new MessageOptions()
    .setPrompt("The background build completed successfully.")
    .setSource(MessageSource.SYSTEM)).get();

session.sendAndWait(new MessageOptions()
    .setPrompt("The review found no blocking issues.")
    .setSource(MessageSource.agent("reviewer"))).get();
```

Leave `source` unset to omit it from the request and retain the runtime's default
user-input behavior, or set `MessageSource.USER` explicitly. Source is independent
of delivery mode (`enqueue` or `immediate`) and does not configure the session's
system prompt.

Agent sources serialize as `agent-<id>`. Pass the agent ID without adding a
prefix. The SDK preserves its case and whitespace and rejects null IDs.
`sendAndWait` accepts the same source values as `send`. Sub-agent events remain
visible to listeners but do not complete the wait or supply its reply.
Synchronous listeners registered before `sendAndWait` finish processing the root
`session.idle` event before its future completes. Event listeners run in
registration order.

## Structured output (experimental)

Annotate a result record or class using the same compile-time schema-generation
approach as `@CopilotTool`. No additional schema dependency is needed:

```java
@CopilotResponse
public record Inventory(int count, String color) {}

Inventory inventory = session.sendAndWait(
    "Call get_inventory, then report the widget count and color.",
    Inventory.class
).get();
```

On resume, transcript recovery defaults to true in all modes.
Use `ResumeSessionConfig.setAllowTranscriptRecovery(false)` to reject recovery.
A repaired session exposes
`getTranscriptRecovery()` (or null), with `plannedBackupPath`,
`invalidLineNumbers` (including discarded torn-tail lines), and
`sessionStartMoved`. A rejected resume retains the existing error message;
`JsonRpcException.getCode()` and `getData()` expose the server's code and
`invalidLineNumbers` / `sessionStartMoved` data.
Disabling recovery still permits adding a missing newline after an intact final
record; it rejects torn tails.

Enable annotation processing with `CopilotResponseProcessor` (automatically
discoverable alongside the SDK's existing processors), and opt in to experimental
APIs as described below. The processor reuses the custom-tool `SchemaGenerator`,
closing record/class objects for strict output. Its existing type-mapping
limitations apply: custom Jackson naming/converters and recursive types need an explicit schema.
Provider restrictions, including optional-field and dictionary restrictions,
still apply. Jackson deserialization is not full JSON Schema validation.

For an explicit schema, use `new MessageOptions().setPrompt(...).setResponseSchema(schema)`
with `send` or `sendAndWait`; schema-bearing `sendAndWait` returns the ordinary
message event. Typed overloads accept message options and a timeout, clone the
options, and reject explicit schemas and immediate delivery.

Schemas apply to one run, including tools, steering, and stop-hook corrections;
independent sends and subagents do not inherit them. Streaming stays text.
Structured waits return the last correlated root message without tool requests at
non-autopilot idle or the matching root `session.autopilot_stopped`.
All `sendAndWait` variants accept this authoritative Autopilot terminal without
waiting for or stopping attached shells. Its `requestId` must match the original
send ID; continuation messages do not change ownership. Missing, unrelated, or
subagent terminals cannot complete the wait. `session.idle` remains whole-session
quiescence; `assistant.idle` and `session.task_complete` alone are not completion.
Concurrent waits keep their own results; queued work can delay
idle. Aborts, session errors after the run starts, and missing final output fail.
Cancellation and timeout stop waiting without aborting the agent.

## Permission Handling

`PermissionHandler.APPROVE_ALL` approves requests when managed settings are disabled. When `enableManagedSettings` is true, it completes exceptionally. Custom handlers can inspect `request.getManagedApprovalRequired()` for human-facing confirmation logic.

When handling `PermissionRequestedEvent` directly, convert its generated event values with `PermissionRequest.fromJsonValue(...)` to access the typed metadata:

```java
if (Boolean.TRUE.equals(event.getData().resolvedByHook())) {
    return; // The permissionRequest hook already answered this request.
}

var rawRequest = PermissionRequest.fromJsonValue(event.getData().permissionRequest());
var promptValue = event.getData().promptRequest();
var promptRequest = promptValue == null ? null : PermissionRequest.fromJsonValue(promptValue);

var assistedApprovalClearsManagedRequirement = promptRequest != null
        && ("commands".equals(promptRequest.getKind()) || "read".equals(promptRequest.getKind())
                || "write".equals(promptRequest.getKind()) || "url".equals(promptRequest.getKind())
                || "workflow".equals(promptRequest.getKind()))
        && !Boolean.TRUE.equals(promptRequest.getManagedApprovalRequired());
var humanApprovalRequired = Boolean.TRUE.equals(rawRequest.getManagedApprovalRequired())
        && !assistedApprovalClearsManagedRequirement;
```

`PermissionHandler` receives the same derived prompt as `invocation.getPromptRequest()` when newer runtimes provide it. The raw request's `managedApprovalRequired` remains managed-policy provenance and enforces one-shot restrictions. Only derived `commands`, `read`, `write`, `url`, and `workflow` prompts can clear the human requirement after Assisted approval; for missing or other prompt kinds, keep the raw marker authoritative. Java represents the derived prompt as `PermissionRequest`, so prompt-only fields such as `assistedApproval` are available through `getExtensionData()`.

Custom handlers must check managed approval before applying kind-specific automatic decisions:

```java
import java.util.concurrent.CompletableFuture;

import com.github.copilot.rpc.PermissionHandler;
import com.github.copilot.rpc.PermissionRequestResult;

PermissionHandler handler = (request, invocation) -> {
    var prompt = invocation.getPromptRequest();
    var assistedApprovalClearsManagedRequirement = prompt != null
            && ("commands".equals(prompt.getKind()) || "read".equals(prompt.getKind())
                    || "write".equals(prompt.getKind()) || "url".equals(prompt.getKind())
                    || "workflow".equals(prompt.getKind()))
            && !Boolean.TRUE.equals(prompt.getManagedApprovalRequired());
    if (Boolean.TRUE.equals(request.getManagedApprovalRequired())
            && !assistedApprovalClearsManagedRequirement) {
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

The example depends only on `copilot-sdk-java`, not on the platform runtime JAR, so set `COPILOT_CLI_PATH` to a Copilot CLI before running it.

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

`@CopilotTool` and `@CopilotToolParam` are experimental.
Compiling a tool class also compiles the companion class that the annotation processor generates for it, and that class implements an experimental interface.
So the build needs `-Acopilot.experimental.allowed=true`; [Using experimental APIs](#using-experimental-apis) shows the Maven and Gradle settings.
Annotating your tool class with `@AllowCopilotExperimental` does not help here, because the generated companion is a separate top-level class that the annotation does not cover.
On JDK 23 and later, `javac` runs the SDK's annotation processors only when you ask for them, for example with `-proc:full`, and otherwise generates no companion class.

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
import com.github.copilot.tool.Param;

Param<Integer> limit = Param.of(Integer.class, "limit", "Max results", false, "10");
```

### Async handlers

Use `fromAsync` for asynchronous tool handlers:

<!-- docs-validate: hidden -->
```java
import java.util.concurrent.CompletableFuture;
import com.github.copilot.rpc.ToolDefinition;
import com.github.copilot.tool.Param;

public class AsyncToolHandlerExample {
    // The reader's own remote call.
    static String fetchRemote(String url) {
        return url;
    }

    static ToolDefinition fetchDataTool() {
        ToolDefinition fetchData = ToolDefinition.fromAsync(
            "fetch_data",
            "Fetches data from remote source",
            Param.of(String.class, "url", "Data source URL"),
            url -> CompletableFuture.supplyAsync(() -> fetchRemote(url))
        );
        return fetchData;
    }
}
```
<!-- /docs-validate: hidden -->
```java
import java.util.concurrent.CompletableFuture;
import com.github.copilot.rpc.ToolDefinition;
import com.github.copilot.tool.Param;

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
import com.github.copilot.rpc.ToolDefinition;
import com.github.copilot.tool.Param;

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

### String-schema `apply_patch` overrides

An explicit `apply_patch` override can declare `Map.of("type", "string")`.
The model sees a required `input` property, but the runtime restores the scalar
patch text before dispatch. Use `ToolInvocation.getArgumentsAs(String.class)`
to read it; `getArguments()` is for object-shaped arguments. This example
returns trimmed patch text; replace the handler body with your own patch
implementation:

```java
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import com.github.copilot.rpc.ToolDefinition;

ToolDefinition applyPatch = ToolDefinition.create("apply_patch", "Apply a patch", Map.of("type", "string"),
        invocation -> {
            String patch = invocation.getArgumentsAs(String.class);
            return CompletableFuture.completedFuture(patch.trim());
        }).overridesBuiltInTool(true);
```

Register this definition with `SessionConfig.setTools(...)`. String-schema
`apply_patch` overrides cannot contain JSON Schema references; use an object
schema if references are needed.

## Changing tools on a live session (experimental)

`CopilotSession.setTools(List<ToolDefinition>)` replaces the external tools
registered by this Java client without recreating the session:

```java
import java.util.List;
import com.github.copilot.rpc.ToolDefinition;
import com.github.copilot.tool.Param;

session.setTools(List.of(
    ToolDefinition.from("lookup_fruit", "Looks up fruit by code",
        Param.of(Integer.class, "code", "Fruit code"),
        code -> "dragonfruit")
)).get();
```

The list is a complete replacement for this client only. Built-in tools, MCP
tools, plugin tools, and tools registered by other connections are unchanged.
Pass an empty list to remove this client's tools. Handlers switch after the
runtime accepts the replacement; rejected replacements leave the previous
handlers in place. Tool calls already running finish on the handler that started
them.

For cross-SDK behavior and active-turn caveats, see
[changing tools](../docs/features/changing-tools.md).

## Auto routing tiers

Use `CapiSessionOptions.setAutoTier(...)` to select `AutoTier.EFFICIENCY`,
`AutoTier.BALANCE`, `AutoTier.INTELLIGENCE`, or `AutoTier.FAST`. This option is
meaningful only with model `auto` (Auto mode V2).
It requires a runtime version that supports `capi.autoTier`.
`AutoTier.FAST` is an integrator-only latency preset, not a first-party GitHub
Copilot product preference — the SDK does not decide Fast eligibility or apply
it implicitly.

```java
import com.github.copilot.rpc.AutoTier;
import com.github.copilot.rpc.CapiSessionOptions;
import com.github.copilot.rpc.PermissionHandler;
import com.github.copilot.rpc.SessionConfig;

var config = new SessionConfig()
    .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
    .setModel("auto")
    .setCapi(new CapiSessionOptions().setAutoTier(AutoTier.BALANCE));
```

The same options work with `ResumeSessionConfig.setCapi(...)` and can be combined
with `setEnableWebSocketResponses(false)`. The SDK omits an unset (`null`) tier:
the runtime chooses its default on create and preserves the persisted/current
tier on resume. An explicit tier overrides the persisted tier on cold resume. On
resident resume, a different tier requests a safe switch applied after the
resume succeeds; it cannot change a turn that is already in flight. The SDK does not choose a default or manage tier persistence.
See [Auto tier persistence](../docs/features/session-persistence.md#auto-tier-persistence)
for the lifecycle rules.

### Changing the Auto tier during a session

Change the Auto routing preference without changing the selected model. The runtime does not apply the preference immediately: it records the request and commits it only when a later user turn using the `auto` model successfully obtains a usable model from the provider, so a `pending` status confirms acceptance rather than effect. Only the most recent request survives.

Watch for the outcome through the `session.model_change` event on success or the ephemeral `session.auto_tier_switch_failed` event on failure. A failed activation leaves the incumbent effective tier unchanged. Read the authoritative committed, pending, and activating preferences at any time through the session's `model.getCurrent` RPC method.

```java
import com.github.copilot.generated.rpc.ModelSwitchAutoTierStatus;
import com.github.copilot.rpc.AutoTier;

var result = session.setAutoTier(AutoTier.INTELLIGENCE).get();
if (result.status() == ModelSwitchAutoTierStatus.PENDING) {
    // Accepted, but not yet in effect.
}

// Return to the provider's default Auto routing.
session.setAutoTier(null).get();
```

`setModel(SetModelOptions)` accepts the same preference through `SetModelOptions.setAutoTier(...)`, which stages the tier atomically with selecting `auto`. Call `setResetAutoTier(true)` instead to return to provider-default routing; the two options are mutually exclusive.

## Named construction and compatibility

Use the construction API that matches the object's role:

* **SDK-owned options** such as `SessionConfig`, `SetModelOptions`, and
  `com.github.copilot.rpc.ModelCapabilitiesOverride` keep their fluent `setX(...)`
  methods.
* **Generated object DTOs** use `Type.builder().property(value).build()`, including
  nested event payloads. Records retain component accessors such as `content()`;
  builders do not add JavaBeans getters or `toBuilder()`.
* **Extensible generated requests** keep their required-input constructors and
  fluent setters. For example, `SessionModelSwitchToRequest` exposes `providerId`,
  which the frozen `SessionModelSwitchToParams` record does not. Both expose
  `contextTier`. Use the existing request overload when `providerId` is needed,
  not another options or builder API.

```java
import com.github.copilot.AllowCopilotExperimental;
import com.github.copilot.CopilotSession;
import com.github.copilot.generated.AssistantMessageEvent;
import com.github.copilot.generated.rpc.ContextTier;
import com.github.copilot.generated.rpc.SessionModelSwitchToParams;
import com.github.copilot.generated.rpc.SessionModelSwitchToRequest;
import com.github.copilot.rpc.ModelCapabilitiesOverride;
import com.github.copilot.rpc.SetModelOptions;
import java.util.concurrent.CompletableFuture;

@AllowCopilotExperimental
class ConstructionExample {
    static CompletableFuture<Void> configureModel(CopilotSession session) {
        return session.setModel(new SetModelOptions()
            .setModel("gpt-5.4")
            .setModelCapabilities(new ModelCapabilitiesOverride()
                .setSupports(new ModelCapabilitiesOverride.Supports().setVision(false))));
    }

    static CompletableFuture<?> useRecord(CopilotSession session) {
        var params = SessionModelSwitchToParams.builder()
            .sessionId(session.getSessionId())
            .modelId("gpt-5.4")
            .reasoningEffort("low")
            .build();
        return session.getRpc().model.switchTo(params);
    }

    static CompletableFuture<?> useExtensibleRequest(CopilotSession session) {
        return session.getRpc().model.switchTo(new SessionModelSwitchToRequest("gpt-5.4")
            .setProviderId("my-provider")
            .setContextTier(ContextTier.fromValue("long_context")));
    }

    static AssistantMessageEvent makeEvent() {
        var event = new AssistantMessageEvent();
        event.setData(AssistantMessageEvent.AssistantMessageEventData.builder()
            .messageId("message-1")
            .content("Hello")
            .build());
        return event;
    }
}
```

Builders are additive: generated DTOs remain records or their existing mutable
variant classes. Mainline and published 1.0.28 constructor signatures are retained,
deprecated in favor of named construction, with `forRemoval = false`; no
constructor removal is scheduled. Retaining those signatures preserves selected
source and bytecode compatibility, but deprecation is not warning-free:
`-Xlint:deprecation -Werror` can fail a consumer build.

For additive optional **reference** properties, unchanged builder source and
already-compiled builder callers remain usable. Retained constructors preserve
released argument types and order, defaulting newly added optional
references to null with the existing omission behavior, including for middle
insertions. This guarantee requires unchanged existing property names, types,
and requiredness. It does not cover required additions, renames, removals, type
changes, conflicting erased constructor descriptors, exact record
reflection/deconstruction shape, or equality, hash-code, and string results
across different schemas.

`build()` rejects missing required properties with
`IllegalStateException("Missing required property: <wire-name>")`. Required
nullable properties need an explicit setter call, which can supply null.
Optional setters can be omitted; builders do not materialize schema defaults.
False, zero, and empty values count as supplied. Supply actual session IDs even
though session-bound RPC wrappers remain authoritative. Retained constructors
and Jackson reads do not pass through builder validation. A nullable setter does
not promise a JSON null; existing omission behavior and explicit-null reset APIs
such as `setAutoTier(null)` remain unchanged.

Builders are mutable and reusable; each build creates a fresh DTO without changing
earlier field assignments. Collections retain shallow aliasing and mutability,
not defensive copies or deep immutability. Raw `JsonNode` union wrappers, open
maps, opaque JSON, enums, sealed interfaces, abstract union bases, event
envelopes, and transport/framework types retain their existing construction APIs.
Generated packages remain public in the existing SDK module.

Javadoc `@since` describes the introduction of the API it documents: an older
record type's tag does not date its newly added builder. Constructor deprecation
metadata likewise describes the deprecation release, not the record's original
introduction.

## Reasoning-summary presentation (experimental)

Read the current model's reasoning-summary policy through `session.getRpc().model.getReasoningSummaries(...)`. Load the model catalog with `session.getRpc().model.list().get()` first, or accept a result with `ready() == false`.

```java
import com.github.copilot.AllowCopilotExperimental;
import com.github.copilot.CopilotSession;
import com.github.copilot.generated.rpc.SessionModelGetReasoningSummariesParams;

@AllowCopilotExperimental
class ReasoningSummaryExample {
    static void readPolicy(CopilotSession session) throws Exception {
        var presentation = SessionModelGetReasoningSummariesParams.builder()
            .sessionId(session.getSessionId())
            .summariesVisible(false)
            .build();
        var policy = session.getRpc().model.getReasoningSummaries(presentation).get();
        System.out.println(policy.visibility());
    }
}
```

Omit `persistedShowReasoning` when the persisted visibility setting is unset, or
set it to that setting's value. `summariesVisible` reports whether the host
currently shows summaries (unset defaults to false). Supply the actual session
ID to the builder; the session wrapper still overrides it with its bound ID.
The deprecated two-argument presentation-state and three-argument constructors
remain available. `visibility()` can be `null` while experiment assignment is
pending, and `clearRedundantSetting()` tells the host when its persisted setting
matches the model default.

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

## JSON-RPC error handling

Server error responses surface as `com.github.copilot.JsonRpcException`, a
`RuntimeException` with `getCode()`, `getMessage()`, and `getData()`. The data is a
Jackson `JsonNode`: objects, arrays, strings, numbers, and booleans retain their
JSON types, including empty values, zero, and false. Numeric fidelity follows
Jackson's existing parser. Omitted data returns Java `null`; explicit JSON `null`
returns a `NullNode` (`data.isNull()` is true).

Future wrapping is unchanged. With `get()`, inspect the cause of
`ExecutionException`:

```java
import com.github.copilot.JsonRpcException;
import java.util.concurrent.ExecutionException;

try {
    client.ping("hello").get();
} catch (ExecutionException ex) {
    if (ex.getCause() instanceof JsonRpcException rpcError) {
        System.err.println("RPC " + rpcError.getCode() + ": " + rpcError.getMessage());
        var data = rpcError.getData();
        if (data != null && !data.isNull()) {
            // Inspect data according to the server's error contract.
        }
    } else {
        throw ex; // Transport and local failures are not JSON-RPC error responses.
    }
}
```

The enclosing method must handle or declare both `InterruptedException` and the
rethrown `ExecutionException`.
With `join()`, the wrapper is `CompletionException` instead:

```java
import java.util.concurrent.CompletionException;

try {
    client.ping("hello").join();
} catch (CompletionException ex) {
    if (ex.getCause() instanceof JsonRpcException rpcError) {
        System.err.println("RPC " + rpcError.getCode() + ": " + rpcError.getMessage());
        var data = rpcError.getData();
        if (data == null) {
            // The data member was omitted.
        } else if (data.isNull()) {
            // The server explicitly supplied JSON null.
        } else {
            // Inspect data according to the server's error contract.
        }
    } else {
        throw ex;
    }
}
```

Error data is not appended to `getMessage()` or `toString()`. Avoid logging it
indiscriminately: server-provided data may contain sensitive information.
`getData()` returns the shared Jackson node, not an immutable snapshot. Use
`deepCopy()` before modifying a container node, particularly when multiple
observers share the failed future.

## Using experimental APIs

Some SDK APIs are marked as experimental with `@CopilotExperimental`. These APIs may change or be removed in future versions without notice.

An otherwise stable session event can contain experimental properties. Generated
event payload records preserve each property's schema stability marker on its
record component and generated accessor; the enclosing event and its other
properties remain stable.

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

In practice, these gaps rarely matter for experimental types: any meaningful use of an experimental SDK type almost always appears in a field declaration, method signature, or type hierarchy — all of which are caught. A purely inline expression with no declaration footprint (e.g., `session.getRpc().model.getReasoningSummaries(params).join()`) slips through, and so does any use of an experimental method, described below.

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

The processor checks the types that declarations name, not the members your code uses. `@CopilotExperimental` on an individual method of an otherwise stable type is therefore not enforced: calling that method, storing its result in a field of a stable type, or overriding it compiles without an opt-in. Likewise, calling a static method of an experimental type compiles as long as no declaration names that type. `@AllowCopilotExperimental` mirrors the same declaration-level boundary: annotating a class opts in that class and its enclosed declarations, while annotating a method or constructor opts in just that executable signature.

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

# Build and test with JDK 25
mvn test-compile jar:jar
mvn verify -Dskip.test.harness=true

# Set your paths for JDK 17
# Run the JDK 25 built jar with JDK 17 JVM for tests. Do not re-compile the jar.
mvn jacoco:prepare-agent@wire-up-coverage-instrumentation antrun:run@print-test-jdk-banner surefire:test failsafe:integration-test failsafe:verify jacoco:report@build-coverage-report-from-tests -Denforcer.skip=true
```

#### Formatting and linting

From the repository root, run `just format-java` to apply formatting and `just lint-java` to check formatting and Javadoc. These recipes are also included in `just format` and `just lint`.

Without `just`, run the equivalent Maven commands from `java/`:

```bash
# Apply formatting
mvn -pl sdk spotless:apply

# Check formatting and Javadoc
mvn -pl sdk spotless:check checkstyle:check
```

CI enforces both checks. Spotless runs explicitly in CI; `mvn verify` alone does not check formatting.

#### Development Setup for native embedding

Run native-runtime Maven commands from the `java` directory. Native packaging requires Node.js in addition to JDK 25 and Maven. In a standalone SDK checkout, `copilot-native/scripts/fetch-native.mjs` retrieves the pinned runtime package from the corresponding GitHub release. When the SDK is nested in the Copilot runtime source tree, it instead stages the same-checkout artifacts from `dist-cli`; run `pnpm run build:cli` from the runtime root first.

On a native Linux glibc host, Maven activates `native-linux-x64` or `native-linux-arm64` for the matching architecture when `copilot.native.libc=glibc` is set. On a Linux musl host, Maven activates `native-linuxmusl-x64` or `native-linuxmusl-arm64` for the matching architecture when `copilot.native.libc=musl` is set. On Windows x64, Windows ARM64, Intel macOS, and Apple Silicon macOS, Maven activates `native-win32-x64`, `native-win32-arm64`, `native-darwin-x64`, or `native-darwin-arm64` automatically. The matching profile validates the host, runs the native script tests, stages the platform package during `generate-resources`, packages the classifier JAR during `package`, and verifies its native contents.

Before opting in, validate that Node.js reports glibc for the build host:

```bash
node copilot-native/scripts/validate-native-host.mjs linux-x64
mvn -pl copilot-native clean verify -Dcopilot.native.libc=glibc
```

The `inprocess` test profile performs the same validation and native packaging automatically, so the full in-process test command remains:

```bash
mvn -Pinprocess clean verify
```

On Windows x64 or ARM64 PowerShell, initialize Java and run the same profile:

```powershell
mvn -Pinprocess clean verify
```

The same command validates in-process mode on macOS; use the classifier for the host architecture:

```bash
node copilot-native/scripts/validate-native-host.mjs darwin-x64 # Use darwin-arm64 on Apple Silicon
mvn -Pinprocess clean verify
```

The same command validates in-process mode on Linux ARM64:

```bash
node copilot-native/scripts/validate-native-host.mjs linux-arm64
mvn -Pinprocess clean verify -Dcopilot.native.libc=glibc
```

The same command validates in-process mode on Linux musl x64 or ARM64:

```bash
node copilot-native/scripts/validate-native-host.mjs linuxmusl-x64 # Use linuxmusl-arm64 on ARM64
mvn -Pinprocess clean verify -Dcopilot.native.libc=musl
```

On unsupported hosts, do not set `copilot.native.libc`. A normal build produces only the OS-neutral primary, sources, and Javadoc JARs; it does not run native script tests, download or stage native files, or produce a platform classifier JAR.

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

Each classifier JAR includes `runtime.node`, `platform.properties`, and `copilot-runtime` (or `copilot-runtime.exe`) under its `native/<classifier>` directory. It does not contain the legacy `copilot` SEA. The placeholder JAR remains OS-neutral and contains no native binaries. Unsupported hosts retain the placeholder-only behavior.

### Versioning and releases

The Java SDK uses [Maven CI-friendly versions](https://maven.apache.org/maven-ci-friendly.html). Every module declares `<version>${revision}</version>`, and the single source of truth is the `<revision>` property in `java/pom.xml`. The committed value stays a `-SNAPSHOT` for local development. Published artifacts contain a concrete version rather than the unresolved `${revision}` property.

Stable, prerelease, and public unstable releases include all six SDKs. SDK and runtime versions are numbered independently; each Java release contains the matching runtime artifacts in its native classifier JARs. Java prereleases append `-SNAPSHOT` to the SDK version; stable and public unstable versions remain unchanged.

Release artifacts and source references are public:

- Stable and public unstable Java packages are available from Maven Central; previews use Central Portal snapshots.
- The `java/v<SDK-version>` and `v<SDK-version>` tags identify the corresponding public SDK source snapshot in [`github/copilot-sdk`](https://github.com/github/copilot-sdk).
- Stable/prerelease versions also have a combined `v<SDK-version>` [GitHub release](https://github.com/github/copilot-sdk/releases) and versioned Java documentation. Prerelease documentation does not replace the latest documentation.
- Unstable releases create source tags without advancing SDK `main`, creating an SDK GitHub release announcement, or deploying Java documentation. Their runtime assets are available in the separate `runtime-<runtime-version>` release.

To validate a build with an explicit version locally, without changing the checked-in POM or publishing:

```bash
# Build and verify with an explicit version, without touching the POM
mvn clean verify -Drevision=1.2.3

# Inspect the generated flattened POMs for the literal version (no ${revision})
cat sdk/.flattened-pom.xml copilot-native/.flattened-pom.xml
```

These commands do not upload artifacts. Do not use `deploy` for local validation: the Central publishing plugin is configured with `autoPublish=true`.

`flatten-maven-plugin` (ossrh mode) resolves `${revision}` into the installed and published POMs. Documentation version references are updated through a normal reviewed pull request (see `scripts/update-documentation-versions.sh`), not as a side effect of publishing.

## License

MIT — see [LICENSE](../LICENSE) for details.
