## Overview

Create the `RuntimeConnection` sealed class hierarchy, add `setConnection()` to `CopilotClientOptions`, and implement the InProcess code path in `CopilotClient` that uses `FfiRuntimeHost` instead of `CliServerManager`. **Do NOT create a `Transport` enum or `setTransport()` method** — that approach was explicitly rejected in favor of the `RuntimeConnection` type hierarchy.

**This is task 4.6 of 9 in the implementation plan.** Tasks are assigned, completed, and merged serially in this listed order. Tasks 4.1–4.5 are complete on the base branch before this task begins.

**Branch:** `edburns/1917-java-embed-rust-cli-runtime-dd-3039145` on `upstream`

## Plan and supporting resources

On the `edburns/1917-java-embed-rust-cli-runtime-dd-3039145` branch, the directory `1917-java-embed-rust-cli-runtime-remove-before-merge` contains the plan (`1917-embed-cli-runtime-ignorance-reduction-plan.md`) and supporting resources (spikes, prototypes, diagrams).

**Read the entire plan before working.**

## Relevant plan sections to carefully re-read

- **Section 3.5.1 — How is InProcess transport selected** — Resolution: RECOMMENDATION SUPERSEDED. `Transport` enum rejected. Use `RuntimeConnection` sealed class hierarchy with factory methods mirroring .NET 1:1. Four concrete subtypes: `StdioRuntimeConnection`, `TcpRuntimeConnection`, `UriRuntimeConnection`, `InProcessRuntimeConnection`. Backward compatibility via nullable `connection` field (null → legacy fields). `IllegalArgumentException` when both `connection` and legacy fields are set.
- **Section 3.5.2 — What replaces CliServerManager for InProcess** — Resolution: `FfiRuntimeHost` (from task 4.5). `CopilotClient.startCoreBody()` dispatches on `connection instanceof InProcessRuntimeConnection`.
- **Section 3.14 — `@CopilotExperimental` annotation** — Resolution: Annotate InProcess transport with `@CopilotExperimental`.
- **Section 4.6 — Transport integration with CopilotClient** (the primary task description).
- **TDD discipline for all implementation steps** — write tests first, implement until green, refactor, gate before proceeding.

## Critical implementation requirements (from plan ✅✅ markers)

1. **`COPILOT_SDK_DEFAULT_CONNECTION` env var resolution:** `CopilotClient` must implement `resolveDefaultConnection()` when no `connection` is set. See .NET `dotnet/src/Client.cs` — search for `ResolveDefaultConnection`; Rust `rust/src/lib.rs` — search for `fn resolve_default_transport` and constant `DEFAULT_CONNECTION_ENV_VAR`. When the env var is `"inprocess"`, automatically select `InProcessRuntimeConnection`.

2. **`ValidateEnvironmentOptions` — reject incompatible options for InProcess:** `environment`, `telemetry`, `workingDirectory`, `extraArgs` must be rejected with `IllegalArgumentException` when InProcess is selected. Without this, users set options that silently do nothing in-process. See .NET `dotnet/src/Client.cs` — search for `ValidateEnvironmentOptions`; Rust `rust/src/lib.rs` — search for `fn validate_inprocess_options`.

3. **Backward-compatibility bridge:** When `connection` is null, existing `useStdio`/`cliUrl`/`cliPath` logic runs unchanged. Test this bridge explicitly.

## Resolved decisions that constrain this task

- **Package for `RuntimeConnection` hierarchy:** `com.github.copilot.rpc` (alongside `CopilotClientOptions`).
- **Sealed class design:**
  ```java
  public abstract sealed class RuntimeConnection
      permits StdioRuntimeConnection, TcpRuntimeConnection,
              UriRuntimeConnection, InProcessRuntimeConnection {
      RuntimeConnection() {} // package-private
      public static StdioRuntimeConnection forStdio() { ... }
      public static StdioRuntimeConnection forStdio(String path) { ... }
      public static TcpRuntimeConnection forTcp() { ... }
      public static UriRuntimeConnection forUri(String url) { ... }
      public static InProcessRuntimeConnection forInProcess() { ... }
  }
  ```
- **Four concrete subtypes:**
  | Java subclass                | Transport                       | Config fields                             |
  | ---------------------------- | ------------------------------- | ----------------------------------------- |
  | `StdioRuntimeConnection`     | stdin/stdout pipe to subprocess | `path`, `args`                            |
  | `TcpRuntimeConnection`       | TCP socket to subprocess        | `path`, `port`, `connectionToken`, `args` |
  | `UriRuntimeConnection`       | TCP to external server          | `url` (required), `connectionToken`       |
  | `InProcessRuntimeConnection` | FFI via JNA C ABI               | _(none — uses bundled native library)_    |

- **`CopilotClientOptions.connection`:** Nullable, default `null`. When non-null, takes precedence over legacy fields. If both `connection` and legacy fields are set → `IllegalArgumentException`.
- **`CopilotClient.startCoreBody()` dispatch:**
  ```java
  if (connection instanceof InProcessRuntimeConnection) {
      ffiHost = new FfiRuntimeHost(...);
      ffiHost.start();
      rpc = JsonRpcClient.fromStreams(ffiHost.getReceiveStream(), ffiHost.getSendStream());
  } else if (optionsHost != null) {
      rpc = serverManager.connectToServer(null, optionsHost, optionsPort);
  } else {
      // existing subprocess path — unchanged
  }
  ```
- **`Connection` record update:** Add `FfiRuntimeHost` field so `stop()`/`forceStop()` can call `ffiHost.close()`.
- **`@CopilotExperimental`:** Annotate `InProcessRuntimeConnection` and `RuntimeConnection.forInProcess()`.

## Deliverables

### Files to create

1. **`java/sdk/src/main/java/com/github/copilot/rpc/RuntimeConnection.java`** — Sealed class with factory methods.
2. **`java/sdk/src/main/java/com/github/copilot/rpc/StdioRuntimeConnection.java`**
3. **`java/sdk/src/main/java/com/github/copilot/rpc/TcpRuntimeConnection.java`**
4. **`java/sdk/src/main/java/com/github/copilot/rpc/UriRuntimeConnection.java`**
5. **`java/sdk/src/main/java/com/github/copilot/rpc/InProcessRuntimeConnection.java`** — annotated `@CopilotExperimental`.

### Files to modify

6. **`java/sdk/src/main/java/com/github/copilot/rpc/CopilotClientOptions.java`** — Add `connection` field (type `RuntimeConnection`, nullable, default `null`), `setConnection()`/`getConnection()`.
7. **`java/sdk/src/main/java/com/github/copilot/CopilotClient.java`** — InProcess connection path via `RuntimeConnection` dispatch. Add `resolveDefaultConnection()` and `validateEnvironmentOptions()`. Update `Connection` record with `FfiRuntimeHost` field.

### Test files to create

8. **`java/sdk/src/test/java/com/github/copilot/CopilotClientTransportTest.java`** — Unit tests:
   - `RuntimeConnection.forInProcess()` routes through `FfiRuntimeHost`.
   - `COPILOT_SDK_DEFAULT_CONNECTION=inprocess` env var selects InProcess.
   - Backward-compatibility bridge: legacy `useStdio`/`cliUrl`/`cliPath` fields → correct `RuntimeConnection` inference.
   - `IllegalArgumentException` when both `connection` and legacy fields are set.
   - `validateEnvironmentOptions()` rejects `environment`, `telemetry`, `workingDirectory`, `extraArgs` for InProcess.
   - CLI transport remains unchanged when `connection` is null.

## Gating tests and criteria

1. **Unit tests pass:** All tests in `CopilotClientTransportTest` pass.
2. **InProcess routing:** `new CopilotClientOptions().setConnection(RuntimeConnection.forInProcess())` routes through FFI host.
3. **Env var resolution:** `COPILOT_SDK_DEFAULT_CONNECTION=inprocess` works.
4. **Backward compat:** Existing CLI transport paths work identically.
5. **Option validation:** Incompatible options rejected for InProcess.
6. **All prior tests pass:** `mvn verify` from `java/` passes.
7. **Spotless compliance:** `mvn spotless:check` passes.

## Out of scope

- `copilot-native` module or native binary downloading (task 4.7).
- E2E testing with real `runtime.node` (task 4.8).
- CI workflow changes (task 4.9).
