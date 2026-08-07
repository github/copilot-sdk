# Fix remaining InProcess test parity failures

## Context

Branch: `edburns/review-copilot-pr-2272` (local worktree at `copilot-sdk-01`)
Push target: `git push upstream HEAD:copilot/edburns1917-java-embed-rust-cli-runtime-post-agent`

The `-Pinprocess` Maven profile sets `COPILOT_SDK_DEFAULT_CONNECTION=inprocess`, which forces all E2E tests to use the InProcess FFI transport instead of subprocess. Most tests now pass. 24 tests still fail in two categories.

## Category 1: Tests that set `cwd` or `cliArgs` on options

These tests go through `ctx.createClient(options)` → `E2ETestContext.applyContextOptions()`. The InProcess branch absorbs `environment` into `InProcessEnvGuard` and nulls it, but does NOT do the same for `cwd` or `cliArgs`. The `CopilotClient` constructor then calls `validateEnvironmentOptions()` which rejects non-null `cwd`/`cliArgs` for InProcess.

**Fix:** In `E2ETestContext.applyContextOptions()`, when InProcess mode is detected, also null out `cwd` and `cliArgs` before constructing the client. For `cwd`, it's meaningless in InProcess (host process cwd is already set). For `cliArgs`, they're subprocess-specific flags.

Location: `java/sdk/src/test/java/com/github/copilot/E2ETestContext.java` lines 354-376

Current InProcess branch in `applyContextOptions`:
```java
if (isInProcessMode(options)) {
    InProcessEnvGuard guard = new InProcessEnvGuard(buildInProcessEnvironment(options));
    inProcessEnvGuards.add(guard);
    try {
        options.setEnvironment(null);
        return new CopilotClient(options, guard::close);
    } catch (RuntimeException e) {
        guard.close();
        throw e;
    }
}
```

Needs to also null `cwd` and `cliArgs`:
```java
options.setEnvironment(null);
options.setCwd(null);
options.setCliArgs(null);
```

Affected tests: `PerSessionAuthTest` (sets cwd+environment), possibly others.

## Category 2: StreamingFidelityTest hang

`StreamingFidelityTest.testShouldEmitStreamingDeltasWithReasoningEffortConfigured` hangs indefinitely in InProcess mode. The main thread is blocked on `CompletableFuture.get()` at line 258. The JSON-RPC reader thread is reading from `QueueInputStream` (the InProcess FFI receive stream) but never receives the expected response.

This is a functional issue, not a validation issue. The replay proxy is running (CapiProxy thread is active), but the InProcess transport isn't completing the streaming interaction.

Diagnosis approach:
1. Check if the test's replay snapshot exists and is correct for streaming
2. Check if `host_start` succeeds for this test (serverHandle != 0)
3. jstack showed the reader thread blocked in `QueueInputStream.read()` — no data arriving via the FFI callback
4. Possible causes: the replay proxy response format doesn't match what the InProcess runtime expects for streaming, or the connection isn't routing correctly through the replay proxy

## Key architectural facts

- `runtime.node` is loaded via JNA. `copilot` CLI binary is spawned as child by `host_start` via `argv[0]`.
- Both are now bundled in the classifier JAR at `native/<classifier>/runtime.node` and `native/<classifier>/copilot`.
- `NativeRuntimeLoader.resolve()` extracts both to `~/.copilot/runtime-cache/<version>/<classifier>/`.
- `NativeRuntimeLoader.resolveEntrypoint()` finds `copilot` alongside `runtime.node`.
- `CopilotClient.resolveInProcessEntrypoint()` simply calls `NativeRuntimeLoader.resolveEntrypoint().toString()`.
- `InProcessEnvGuard` uses JNA `libc.setenv()` to mutate the native process env (not visible to `System.getenv()`).
- The replay proxy (CapiProxy) runs as a Node.js subprocess serving YAML snapshot responses.

## CopilotClientOptions.setEnvironment(null) quirk

`setEnvironment(null)` does NOT set the field to null — it calls `this.environment.clear()`, leaving an empty HashMap. `getEnvironment()` then returns a non-null empty map. The validation now checks `!isEmpty()` too (already fixed).

Similarly, check if `setCwd(null)` / `setCliArgs(null)` have similar behavior. If `setCwd(null)` doesn't actually null the field, the validation might still fire.

## Validation in CopilotClient constructor

```java
private static void validateEnvironmentOptions(CopilotClientOptions options, RuntimeConnection connection) {
    if (!(connection instanceof InProcessRuntimeConnection)) return;
    rejectInProcessOption("Environment", options.getEnvironment() != null && !options.getEnvironment().isEmpty(), ...);
    rejectInProcessOption("Telemetry", options.getTelemetry() != null, ...);
    rejectInProcessOption("Cwd", options.getCwd() != null, ...);
    rejectInProcessOption("CliArgs", options.getCliArgs() != null && options.getCliArgs().length > 0, ...);
}
```

## resolveDefaultConnection precedence (already fixed)

When `COPILOT_SDK_DEFAULT_CONNECTION=inprocess` but `cliUrl`/`cliPath`/`port` are explicitly set, the explicit options win and subprocess transport is used. Tests like `McpAuthInterestRegistrationTest` that create `new CopilotClient(options.setCliUrl(...))` directly now correctly bypass InProcess.

## Full list of 24 failing test methods

```
ByokBearerTokenProviderE2ETest (3 methods)
CopilotRequestCancelErrorE2ETest (2)
CopilotRequestHandlerE2ETest (2)
CopilotRequestSessionIdE2ETest (1)
GitHubTelemetryTest (2)
McpAuthInterestRegistrationTest (3)
ModeHandlersTest (2)
PerSessionAuthTest (3)
ProviderEndpointE2ETest (2)
RpcServerE2ETest (1 - testShouldAddSecretFilterValues — NOW PASSES)
SessionConfigE2ETest (2)
StreamingFidelityTest (1 - hangs)
SubagentHooksE2ETest (1)
```

## Commands

```bash
# Run all tests with InProcess
cd java && mvn clean verify -Pinprocess

# Run specific failing tests
COPILOT_SDK_DEFAULT_CONNECTION=inprocess mvn test -pl sdk -Dtest="PerSessionAuthTest,StreamingFidelityTest" -DfailIfNoTests=false

# Format before commit
mvn spotless:apply

# Push
git push upstream HEAD:copilot/edburns1917-java-embed-rust-cli-runtime-post-agent
```

## Java env bootstrap (required before any mvn/java command)
```bash
export JAVA_HOME="/usr/lib/jvm/msopenjdk-25-amd64"
export M2_HOME="${HOME}/Downloads/apache-maven-3.9.8"
export PATH="${M2_HOME}/bin:${JAVA_HOME}/bin:${PATH}"
```
