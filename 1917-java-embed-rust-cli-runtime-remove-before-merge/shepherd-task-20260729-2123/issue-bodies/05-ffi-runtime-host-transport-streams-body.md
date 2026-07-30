## Overview

Create the `FfiRuntimeHost` class that manages the full FFI lifecycle: `host_start` → `connection_open` → duplex stream bridging via `QueueInputStream`/`FfiOutputStream` → `connection_close` → `host_shutdown`. Also create `QueueInputStream`, `FfiOutputStream`, and the MR-JAR `ReaderThreadFactory`.

**This is task 4.5 of 9 in the implementation plan.** Tasks are assigned, completed, and merged serially in this listed order. Tasks 4.1–4.4 are complete on the base branch before this task begins.

**Branch:** `edburns/1917-java-embed-rust-cli-runtime-dd-3039145` on `upstream`

## Plan and supporting resources

On the `edburns/1917-java-embed-rust-cli-runtime-dd-3039145` branch, the directory `1917-java-embed-rust-cli-runtime-remove-before-merge` contains the plan (`1917-embed-cli-runtime-ignorance-reduction-plan.md`) and supporting resources (spikes, prototypes, diagrams).

**Read the entire plan before working.**

## Relevant plan sections to carefully re-read

- **Section 3.4 — JNA callback threading and lifecycle** — Resolution: `QueueInputStream` proven approach, `PipedInputStream` REJECTED. `ReaderThreadFactory` MR-JAR swap point (platform thread on JDK 17, virtual thread on JDK 25). Read the spike at `1917-java-embed-rust-cli-runtime-remove-before-merge/spike-3-4-jna-callback-and-threading/java-program-that-invokes-rust-dll-mr-jar-17-25/` (✅ approved approach).
- **Section 3.5.2 — What replaces CliServerManager for InProcess** — Resolution: New `FfiRuntimeHost` class, parallel to `CliServerManager`, not an extension. Lifecycle: load library → `host_start` → `connection_open` → duplex streams → `connection_close` → `host_shutdown`. Package `com.github.copilot.ffi`.
- **Section 3.5.3 — How does JsonRpcClient connect to FFI streams** — Resolution: `QueueInputStream` for read side. `FfiOutputStream` wrapping `connection_write` for write side. Add `JsonRpcClient.fromStreams(InputStream, OutputStream)` factory method.
- **Section 3.9 — C ABI parameter semantics** — Resolution: Wire format is LSP `Content-Length` header framing. `argv_json` construction rules. `env_json` key inventory (3 keys). Shutdown sequence: set closing flag → `connection_close` → drain active callbacks → `host_shutdown` → release callback reference.
- **Section 3.10 — Error handling and diagnostics** — Resolution: Use `IllegalStateException` (not a dedicated exception type). Callback error containment: wrap callback body in try-catch for all `Throwable`. Register `Callback.UncaughtExceptionHandler`. `FfiRuntimeHost` implements `AutoCloseable`; `close()` must never throw. Dispose order: set `disposed` flag → `connection_close` (swallowed) → drain callbacks → `host_shutdown` (swallowed) → close `QueueInputStream` → release callback reference.
- **Section 4.5 — FFI runtime host and transport streams** (the primary task description).
- **TDD discipline for all implementation steps** — write tests first using the test native library from spike-3-4.

## Resolved decisions that constrain this task

- **`QueueInputStream`** — `BlockingQueue<byte[]>`-backed `InputStream`. Shared by both JDK 17 and JDK 25 paths. Lives in base source tree.
- **`ReaderThreadFactory`** — MR-JAR swap point. Baseline at `src/main/java/.../ffi/ReaderThreadFactory.java` (platform thread). Overlay at `src/main/java25/.../ffi/ReaderThreadFactory.java` (virtual thread via `Thread.ofVirtual()`). Same pattern as existing `InternalExecutorProvider`.
- **`FfiOutputStream`** — Wraps `connection_write`. `write(byte[], int, int)` delegates to JNA. The native side copies the buffer synchronously.
- **`JsonRpcClient.fromStreams()`** — One-line addition: `return new JsonRpcClient(in, out, null, null)`.
- **Callback `closing` flag early-exit** — The `on_outbound` callback must check a `closing` flag and return immediately without enqueuing data. Without this, the shutdown drain may never converge.
- **Operation lock for concurrent write/close safety** — `FfiOutputStream.write()` can race with `FfiRuntimeHost.close()`. A lock (e.g., `ReentrantLock`) must serialize write and close operations.
- **`Connection` record needs `FfiRuntimeHost` field** — The current `CopilotClient.Connection` record has `(JsonRpcClient rpc, Process process, ServerRpc serverRpc)`. Add an `ffiHost` field so `stop()`/`forceStop()` can call `ffiHost.close()`. Alternatively, this can be deferred to task 4.6 if `FfiRuntimeHost` lifecycle is managed outside `Connection`.
- **Callback error containment** — Wrap callback body in try-catch for all `Throwable`, log at `WARNING`, return normally. Register `Callback.UncaughtExceptionHandler` via `Native.setCallbackExceptionHandler()`.
- **`FfiRuntimeHost` implements `AutoCloseable`** — `close()` must never throw. Best-effort teardown of all native resources.
- **Logging** — Use `java.util.logging` with logger named for the FFI class. Log successful start at `FINE`, shutdown failures at `FINE` (swallowed), callback exceptions at `WARNING`.
- **`argv_json` construction** — `[entrypoint, "--embedded-host", "--no-auto-update", ...optional_args]`. Optional args: `--log-level`, `--auth-token-env`, `--no-auto-login`, `--session-idle-timeout`, `--remote`.
- **`env_json` construction** — 3 keys only: `COPILOT_SDK_AUTH_TOKEN`, `COPILOT_HOME`, `COPILOT_DISABLE_KEYTAR`.
- **`host_start` blocking constraint** — Must NOT be called on async/reactive executor thread. Use a dedicated thread (Rust uses `spawn_blocking`, .NET uses `Task.Run`).

## Deliverables

### Files to create

1. **`java/sdk/src/main/java/com/github/copilot/ffi/QueueInputStream.java`** — `BlockingQueue<byte[]>`-backed `InputStream`.
2. **`java/sdk/src/main/java/com/github/copilot/ffi/FfiOutputStream.java`** — `OutputStream` wrapping `connection_write` via JNA.
3. **`java/sdk/src/main/java/com/github/copilot/ffi/ReaderThreadFactory.java`** — Baseline (JDK 17): creates a platform daemon thread for `QueueInputStream` consumption.
4. **`java/sdk/src/main/java25/com/github/copilot/ffi/ReaderThreadFactory.java`** — JDK 25 overlay: creates a virtual thread via `Thread.ofVirtual()`.
5. **`java/sdk/src/main/java/com/github/copilot/ffi/FfiRuntimeHost.java`** — Full lifecycle manager. Implements `AutoCloseable`. Key methods:
   - `start(String entrypointPath, CopilotClientOptions options)` — builds `argv_json`/`env_json`, calls `host_start` (on blocking thread), then `connection_open`.
   - `getReceiveStream()` — returns the `QueueInputStream`.
   - `getSendStream()` — returns the `FfiOutputStream`.
   - `close()` — graceful shutdown: closing flag → `connection_close` → drain callbacks → `host_shutdown` → close streams → release callback.

### Files to modify

6. **`java/sdk/src/main/java/com/github/copilot/JsonRpcClient.java`** — Add `public static JsonRpcClient fromStreams(InputStream in, OutputStream out)` factory method.

### Test files to create

7. **`java/sdk/src/test/java/com/github/copilot/ffi/QueueInputStreamTest.java`** — Tests for stream semantics, blocking reads, poison pill / close behavior.
8. **`java/sdk/src/test/java/com/github/copilot/ffi/FfiRuntimeHostTest.java`** — Full lifecycle tests with the spike-3-4 test native library:
   - Start → open → write/read → close lifecycle.
   - Callback data flows through `QueueInputStream`.
   - Write data reaches `connection_write`.
   - Shutdown drains active callbacks.
   - Concurrent write/close safety.
   - Callback exception containment (exception in callback does not propagate to native).
   - `close()` never throws even on failure.
   - Thread-safety tests: multiple threads writing/reading simultaneously, shutdown during active callback.

## Gating tests and criteria

1. **Unit tests pass:** All tests in `QueueInputStreamTest` and `FfiRuntimeHostTest` pass.
2. **Full lifecycle:** Start → open → duplex data flow → close works with the test native library.
3. **Callback data delivery:** Data from native callback arrives via `QueueInputStream`.
4. **Shutdown safety:** Active callbacks drain before `host_shutdown`. `close()` never throws.
5. **Concurrent safety:** Write/close race is protected by operation lock.
6. **`JsonRpcClient.fromStreams()`:** New factory method exists and works.
7. **All prior tests pass:** `mvn verify` from `java/` passes.
8. **Spotless compliance:** `mvn spotless:check` passes.

## Out of scope

- `CopilotClient` integration or `RuntimeConnection` types (task 4.6).
- `copilot-native` module or native binary downloading (task 4.7).
- E2E testing with real `runtime.node` (task 4.8).
