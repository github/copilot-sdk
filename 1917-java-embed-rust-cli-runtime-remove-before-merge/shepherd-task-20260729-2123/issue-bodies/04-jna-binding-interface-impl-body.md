## Overview

Create the `NativeBinding` interface, `JnaNativeBinding` JNA-backed implementation, and `OutboundCallback` JNA callback for the 5 C ABI entry points of the Copilot runtime.

**This is task 4.4 of 9 in the implementation plan.** Tasks are assigned, completed, and merged serially in this listed order. Tasks 4.1–4.3 are complete on the base branch before this task begins.

**Branch:** `edburns/1917-java-embed-rust-cli-runtime-dd-3039145` on `upstream`

## Plan and supporting resources

On the `edburns/1917-java-embed-rust-cli-runtime-dd-3039145` branch, the directory `1917-java-embed-rust-cli-runtime-remove-before-merge` contains the plan (`1917-embed-cli-runtime-ignorance-reduction-plan.md`) and supporting resources (spikes, prototypes, diagrams).

**Read the entire plan before working.**

## Relevant plan sections to carefully re-read

- **Section 3.3 — JNA binding interface design** — Resolution: `NativeBinding` is a Java `interface` with default methods, not an abstract class. Direct instantiation (no `ServiceLoader`). Package `com.github.copilot.ffi`.
- **Section 3.4 — JNA callback threading and lifecycle** — Resolution: Use `QueueInputStream` (not `PipedInputStream` — rejected). Hold `Callback` as strong-reference field. `AtomicInteger` for active callback tracking. Read the spike at `1917-java-embed-rust-cli-runtime-remove-before-merge/spike-3-4-jna-callback-and-threading/`, specifically the `java-program-that-invokes-rust-dll-mr-jar-17-25/` directory (✅ the approved approach).
- **Section 3.8 — JNA dependency management** — Resolution: JNA core 5.19.1, `<optional>true</optional>`. Pin version, do not use range. No `jna-platform`. No GraalVM native-image support.
- **Section 3.9 — C ABI parameter semantics** — Resolution: Complete parameter specification for all 5 entry points. Wire format is LSP `Content-Length` header framing. Buffer copied synchronously by native side.
- **C ABI entry points to bind** (table at top of plan) — the 5 `extern "C"` functions: `copilot_runtime_host_start`, `copilot_runtime_host_shutdown`, `copilot_runtime_connection_open`, `copilot_runtime_connection_write`, `copilot_runtime_connection_close`.
- **Section 4.4 — JNA binding interface and implementation** (the primary task description).
- **TDD discipline for all implementation steps** — write tests first using the test native library from `spike-3-4-jna-callback-and-threading/rust-dll/`.

## Resolved decisions that constrain this task

- **JNA version:** 5.19.1, `<optional>true</optional>` in the SDK POM. Only `net.java.dev.jna:jna` (no `jna-platform`).
- **Interface, not abstract class:** `NativeBinding` is a Java `interface` for future FFM swappability.
- **Direct instantiation:** The transport class instantiates `JnaNativeBinding` directly. No `ServiceLoader`.
- **Library-never-unloads pattern:** The loaded native handle must be held in a `static` field and never released. JNA caches by library name, but this must be explicit since native worker threads outlive any `FfiRuntimeHost` instance. See Rust `OnceLock<Mutex<HashMap<PathBuf, &'static Library>>>` + `Box::leak()`. Missing this risks a crash if a second `FfiRuntimeHost` is created after the first is closed.
- **Duplicate library guard:** If a library is already loaded from path A and a request comes for path B, throw `IllegalStateException` with diagnostic message (from Section 3.10 Resolution).
- **Callback GC protection:** Hold JNA `Callback` as a strong-reference field. If GC'd → dangling function pointer → JVM crash.
- **Active callback tracking:** `AtomicInteger` count of active callbacks, mirroring Rust's `AtomicUsize`.
- **Callback buffer copying:** Use `Pointer.getByteArray(0, len)` — pointer only valid during callback invocation.
- **No GraalVM native-image support** for InProcess transport (Section 3.8 Resolution, DRI decision).

## Deliverables

### Files to create

1. **`java/sdk/src/main/java/com/github/copilot/ffi/NativeBinding.java`** — Interface with methods:
   - `int hostStart(byte[] argvJson, int argvJsonLen, byte[] envJson, int envJsonLen)`
   - `boolean hostShutdown(int serverId)`
   - `int connectionOpen(int serverId, OutboundCallback callback, Pointer userData, byte[] extSource, int extSourceLen, byte[] extName, int extNameLen, byte[] connToken, int connTokenLen)`
   - `boolean connectionWrite(int connectionId, byte[] data, int dataLen)`
   - `boolean connectionClose(int connectionId)`

2. **`java/sdk/src/main/java/com/github/copilot/ffi/JnaNativeBinding.java`** — JNA implementation:
   - Defines a JNA `Library` interface mapping the 5 `extern "C"` exports.
   - Loads the native library by absolute path (from `NativeRuntimeLoader`).
   - Static field holding the loaded library (never unloaded).
   - Process-wide guard preventing loading a different library path.
   - Delegates method calls to the JNA library interface.

3. **`java/sdk/src/main/java/com/github/copilot/ffi/OutboundCallback.java`** — JNA `Callback` interface:
   - `void invoke(Pointer userData, Pointer data, int len)` (or `NativeLong` for `size_t`)
   - Invoked by native code on native threads.

4. **`java/sdk/src/test/java/com/github/copilot/ffi/JnaNativeBindingTest.java`** — Unit tests:
   - Load a test native library (from spike-3-4 `rust-dll/`), call functions, receive callbacks.
   - Error cases throw `IllegalStateException`.
   - Duplicate library load from different path throws `IllegalStateException` with diagnostic message.
   - Callback invocation increments/decrements active callback count.
   - At least one success-path test and one failure/edge-case test per public method.

### JNA dependency addition

Add to `java/sdk/pom.xml`:

```xml
<dependency>
    <groupId>net.java.dev.jna</groupId>
    <artifactId>jna</artifactId>
    <version>5.19.1</version>
    <optional>true</optional>
</dependency>
```

Keep the version in a Maven property for deliberate upgrades.

## Gating tests and criteria

1. **Unit tests pass:** All tests in `JnaNativeBindingTest` pass using the spike-3-4 test native library.
2. **Library load and call:** Can load a native library by path, call exported functions, receive callbacks on native threads.
3. **Duplicate load guard:** Loading from path A then path B throws `IllegalStateException`.
4. **Callback tracking:** `AtomicInteger` correctly tracks active callback count.
5. **All prior tests pass:** `mvn verify` from `java/` passes.
6. **Spotless compliance:** `mvn spotless:check` passes.

## Out of scope

- `FfiRuntimeHost` lifecycle management (task 4.5).
- `QueueInputStream`, `FfiOutputStream`, or stream bridging (task 4.5).
- `CopilotClient` integration (task 4.6).
- Testing with real `runtime.node` binary (task 4.8).
