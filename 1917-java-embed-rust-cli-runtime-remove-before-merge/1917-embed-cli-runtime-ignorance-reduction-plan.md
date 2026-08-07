# Implementation plan: Embed Rust-based Copilot CLI runtime in the Java SDK (issue #1917)

Human DRI: Ed Burns
ADR: `java/docs/adr/adr-007-native-bundling-strategy.md`
Epic: https://github.com/github/copilot-sdk/issues/1917
Reference PRs:

- https://github.com/github/copilot-sdk/pull/1901 — .NET in-process FFI transport (`FfiRuntimeHost.cs`)
- https://github.com/github/copilot-sdk/pull/1915 — Rust SDK in-process FFI transport (`ffi.rs`)

Working directory: `copilot-sdk/1917-java-embed-rust-cli-runtime-remove-before-merge/`

---

✅✅✅YOU MUST READ THE ENTIRE ADR 007 before proceeding. `java/docs/adr/adr-007-native-bundling-strategy.md` ✅✅✅

## Goal

Embed the Copilot runtime (`runtime.node` cdylib) directly into the Java SDK so that consumers no longer need an externally installed Copilot CLI. The SDK will:

1. Ship per-platform classifier JARs containing the `runtime.node` binary for each of the 8 platform targets (Option 2).
2. Support uber-jar assembly via `maven-assembly-plugin` that merges all (or a subset of) platform JARs into a single distributable artifact (Option 1 compatibility).
3. Detect the current platform at runtime, extract the matching native binary, and load it via JNA to call the 5 `extern "C"` entry points of the runtime's C ABI front door.
4. Bridge bidirectional JSON-RPC transport over the FFI boundary (Java → native downcalls, native → Java upcall callbacks).

### C ABI entry points to bind (from .NET PR #1901 and Rust PR #1915)

| Entry point                        | Signature (C)                                                                                                                                                                                                                                                              | Purpose                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
| ---------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `copilot_runtime_host_start`       | `(const uint8_t* argv_json, size_t argv_json_len, const uint8_t* env_json, size_t env_json_len) → uint32_t`                                                                                                                                                                | Start the runtime host. `argv_json` is a JSON array: `["/full/path/to/copilot","--embedded-host","--no-auto-update"]` for a binary entrypoint, or `["node","/full/path/to/index.js","--embedded-host","--no-auto-update"]` for a `.js` dev entrypoint; `--no-auto-update` is always required (pins the worker to the bundled cdylib version, preventing ABI skew). `env_json` is an optional JSON object of environment overrides (null/0 if empty). Returns server handle (0 = failure). **This call blocks for up to ~30 s while the worker boots and connects back; it must not be called on an async/reactive executor thread** (Rust uses `spawn_blocking`, .NET uses `Task.Run`). |
| `copilot_runtime_host_shutdown`    | `(uint32_t server_id) → bool`                                                                                                                                                                                                                                              | Shut down the runtime host identified by `server_id`.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
| `copilot_runtime_connection_open`  | `(uint32_t server_id, void(*on_outbound)(void* user_data, const uint8_t* data, size_t len), void* user_data, const uint8_t* ext_source, size_t ext_source_len, const uint8_t* ext_name, size_t ext_name_len, const uint8_t* conn_token, size_t conn_token_len) → uint32_t` | Open a bidirectional connection; registers `on_outbound` callback for runtime→Java data delivery. `ext_source`, `ext_name`, `conn_token` are nullable metadata buffers — **all three are passed as null/0 in every current SDK implementation** (Rust, .NET, Go, Python); their semantics are under investigation in Q3.9. Returns connection handle (0 = failure).                                                                                                                                                                                                                                                                                                                     |
| `copilot_runtime_connection_write` | `(uint32_t connection_id, const uint8_t* data, size_t len) → bool`                                                                                                                                                                                                         | Write a JSON-RPC frame from Java into the runtime. Native side copies the buffer synchronously before returning.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
| `copilot_runtime_connection_close` | `(uint32_t connection_id) → bool`                                                                                                                                                                                                                                          | Close a connection.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |

The outbound callback signature: `void on_outbound(void* user_data, const uint8_t* data, size_t len)` — invoked by native code (potentially on native threads) to deliver JSON-RPC responses and notifications back to Java.

> **Constraints applying to all five functions:**
>
> - **One library per process.** The cdylib may only be loaded once per process; loading a second instance (different path or version) is unsupported. All four existing SDK implementations (Rust, .NET, Go, Python) enforce this with a process-wide guard. The Java implementation must do the same.
> - **`host_start` must run on a blocking thread.** See table row above.

### Technology choices (decided in ADR-007)

| Concern            | Decision                                                               |
| ------------------ | ---------------------------------------------------------------------- |
| Binding technology | JNA (not Panama FFM) — supports Java 17 baseline, zero consumer config |
| Distribution       | Per-platform classifier JARs (DJL-style) + uber-jar composition        |
| Platform detection | `os.name` + `os.arch` + ELF PT_INTERP for musl detection               |
| Cache location     | `~/.copilot/runtime-cache/<version>/<classifier>/runtime.node`         |

---

## Completed phases

### Phase 1 ✅ — Define the problem and architectural decision

- Epic #1917 created.
- ADR-007 written and reviewed. Evaluates monolithic JAR (Option 1), per-platform classifier JARs (Option 2), and download-on-demand (Option 3).
- Decision: Option 2 + Option 1 via `maven-assembly-plugin`. JNA chosen over Panama FFM.
- Size analysis completed: 48–65 MB uncompressed per platform, ~19–26 MB compressed.
- Platform matrix documented: 8 targets (6 common + 2 musl).
- Panama vs. JNA rationale documented (baseline, consumer friction, performance irrelevance, upcall complexity, GraalVM compatibility).

### Phase 2 ✅ — Reference implementation study

- .NET PR #1901 analyzed: `FfiRuntimeHost.cs` (674 lines), dual interop backends (LibraryImport for net8.0+, delegate-based for netstandard2.0), `InProcessRuntimeConnection` type, Channel-backed duplex streams.
- Rust PR #1915 analyzed: `ffi.rs` (633 lines), `Transport::InProcess`, `CallbackState` with `AtomicUsize` for active callback tracking, `on_outbound` extern "C" callback, `FfiShared` with explicit `Send`/`Sync`.
- Key patterns identified: server handle lifecycle, callback-to-async-stream bridging, LSP framing over FFI, `COPILOT_SDK_DEFAULT_CONNECTION=inprocess` env var for transport selection.

---

## Phase 3 — Ignorance reduction: questions to answer before writing code

This phase eliminates unknowns. Each item is a question or spike. Resolve these **before** writing production code.

### 3.1 — ✅ Maven module structure for per-platform classifier JARs

**Question:** How should the Maven project be structured to produce the coordination artifact plus 8 classifier JARs?

ADR-007 specifies publishing `copilot-sdk-java-runtime:VERSION:<classifier>` artifacts alongside the existing `copilot-sdk-java` coordination artifact. Options:

| Option | Structure                                                                                                                                  | Trade-off                                                                                                                                                                                                                  |
| ------ | ------------------------------------------------------------------------------------------------------------------------------------------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| A      | Single `pom.xml` with Maven Assembly Plugin producing classifier JARs as attached artifacts                                                | Simpler build, but classifier JARs are secondary artifacts of the main module. Maven Central treats them as the same artifact — consumers declare `<classifier>linux-x64</classifier>` on the same `copilot-sdk-java` GAV. |
| B      | Multi-module reactor: parent `pom.xml` → `copilot-sdk-java` (existing) + `copilot-sdk-java-runtime` (new module producing classifier JARs) | Cleaner separation, DJL-style. The runtime module has its own GAV. But adds build complexity and the monorepo's `java/` directory currently has a single `pom.xml`.                                                        |
| C      | Single module, classifiers produced by a custom Maven plugin or build-helper-maven-plugin to attach additional artifacts                   | Middle ground. The classifier JARs are attached artifacts of a new `copilot-sdk-java-runtime` artifact built by its own `pom.xml` adjacent to the main SDK pom.                                                            |

**Spike needed:** Look at how DJL's `pytorch-native` module produces classifier JARs. Verify whether `maven-assembly-plugin` or `build-helper-maven-plugin` is the right tool for attaching pre-built native binaries as classifier artifacts.

**Recommendation:** Option B — a new `copilot-sdk-java-runtime` module with its own `pom.xml` that produces 8 classifier JARs. The main `copilot-sdk-java` artifact declares an optional dependency on the runtime module. This matches the DJL pattern and keeps the existing build untouched.

**Resolution:** Option B — hybrid multi-module reactor, refined by the DJL `pytorch-native` pattern (one module produces all classifier JARs, not one module per platform).

Reactor structure:

```
java/
├── pom.xml                          (parent, packaging=pom, new GAV: com.github:copilot-sdk-java-parent)
├── sdk/
│   └── pom.xml                      (existing SDK, KEEPS GAV: com.github:copilot-sdk-java)
├── copilot-native/
│   └── pom.xml                      (new GAV: com.github:copilot-sdk-java-runtime)
├── copilot-native-all/
│   └── pom.xml                      (optional monolithic: com.github:copilot-sdk-java-runtime-all)
```

Key design decisions:

- The existing `copilot-sdk-java` GAV is preserved — no breaking change for consumers.
- The parent POM (`copilot-sdk-java-parent`) is `packaging=pom` and internal-only.
- The `copilot-native` module uses multiple `maven-jar-plugin` executions (one per platform) to produce 8 classifier JARs as attached artifacts under a single GAV (`copilot-sdk-java-runtime`). Plus a placeholder primary JAR (like DJL's `placeholder=true` pattern) to satisfy Maven Central validation.
- The `copilot-native-all` module uses `maven-assembly-plugin` with `jar-with-dependencies` to merge all 8 classifier JARs into a monolithic JAR, satisfying the ADR's "Option 1 + Option 2" decision outcome.
- `central-publishing-maven-plugin` publishes all classifier JARs atomically under the single `copilot-sdk-java-runtime` GAV — one staging repo, one GPG key, one atomic publish.
- No dependency from `copilot-sdk-java` to `copilot-sdk-java-runtime` — consumer declares both manually. This matches the DJL precedent (`pytorch-engine` does not depend on `pytorch-native-cpu`). The runtime SDK code handles the absence gracefully: throws `UnsupportedOperationException` if `Transport.IN_PROCESS` was explicitly requested but no native binary is found, or silently falls back to subprocess transport if `Transport.DEFAULT` is in effect.
- A Gradle Module Metadata (`.module`) file is generated and published alongside the POM, declaring 8 variants with `org.gradle.native.operatingSystem` and `org.gradle.native.architecture` attributes. This enables Gradle consumers to resolve the correct classifier JAR via variant-aware resolution without a `ComponentMetadataRule`. Musl variants use a custom `com.github.copilot.libc` attribute. A convenience Gradle plugin is deferred until demand warrants it.

### 3.2 — ✅ How do native binaries enter the build?

**Question:** Where do the `runtime.node` binaries come from during the Maven build, and how are they placed into the classifier JARs?

The .NET PR uses MSBuild targets to copy `runtime.node` from `runtimes/<rid>/native/`. The Rust PR uses a `build.rs` script that downloads/extracts from npm package tarballs. For Java, options:

| Option | Mechanism                                                                                                        | Trade-off                                                                                                                            |
| ------ | ---------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------ |
| A      | Maven downloads pre-built tarballs from GitHub Releases during `generate-resources` phase                        | Requires network access at build time; must handle version pinning and integrity verification.                                       |
| B      | A CI workflow pre-stages the binaries into a known directory before `mvn` runs; Maven just copies them into JARs | Simpler POM; CI does the heavy lifting. Matches how the publish pipeline already works.                                              |
| C      | npm-based download (similar to the Rust SDK's approach) via `exec-maven-plugin` calling a Node.js script         | Leverages existing `test/harness` Node.js infrastructure in the monorepo. But adds a Node.js build dependency for the main artifact. |

**Spike needed:** Examine the `copilot-agent-runtime` publish pipeline (`publish-cli.yml`) to understand what artifacts are produced and how other SDKs consume them.

**Recommendation:** Option B for CI/publishing (the workflow stages binaries, Maven packages them). For local development, provide a script that fetches the binaries, but the main `mvn clean verify` should work without native binaries present (InProcess transport is optional).

**Resolution:** Option C variant — `npm pack` per-platform via `exec-maven-plugin`, with SHA-512 integrity verification.

The `package.json`-as-dependency-manifest approach was ruled out by experiment: `npm install` returns `EBADPLATFORM` for cross-platform packages, and `npm install --force` disables all npm safety checks. `npm pack` downloads the tarball without any platform check and does not require `--force`.

Long-term target shape: the `copilot-native` module's `generate-resources` phase runs `npm pack @github/copilot-<platform>@${project.version}` for each supported platform. This produces `.tgz` tarballs, which are then extracted with `tar` to stage **both** the `runtime.node` shared library and the `copilot` CLI executable at `target/native-staging/<classifier>/native/<classifier>/`. The version comes from `${project.version}` — the SDK and npm package versions are identical, so no separate version property is needed.

**Necessary-and-sufficient runtime artifact invariant:** The classifier JAR must contain both `native/<classifier>/runtime.node` (the cdylib loaded via JNA) **and** `native/<classifier>/copilot` (the CLI executable passed as `argv[0]` to `copilot_runtime_host_start`). The Rust `embedded_host.rs` spawns the CLI as a child process to service TypeScript method bodies not yet ported to Rust. Without the CLI executable, `host_start` fails — the classifier JAR is not self-sufficient. Both artifacts ship together in the same `@github/copilot-<platform>` npm package; both must be extracted and bundled. This matches the .NET SDK, which bundles the CLI binary and cdylib together under `runtimes/<rid>/native/`. When the TypeScript migration completes and `embedded_host.rs` no longer spawns a child process, the CLI executable can be removed from the classifier JAR.

Temporary invariant (`linux-x64` only for now): perform this only for `linux-x64` on Ubuntu 24.04 in this phase; all other platform packaging is deferred to a later phase.

Integrity verification: a build step reads the `integrity` field (SHA-512) from the monorepo's `nodejs/package-lock.json` for each `@github/copilot-<platform>` package and verifies the downloaded `.tgz` against it, mirroring Rust's `resolve_version_and_integrity` → `cached_download` → verify pattern in `build/in_process.rs`.

Node.js is required to build the `copilot-native` module but **not** the main `copilot-sdk-java` artifact. Node.js is already required for Java E2E tests (replay proxy), so this introduces no new build dependency. The `copilot-native` module can be skipped entirely (`mvn -pl sdk`) for developers working only on the SDK's Java code.

For CI/publishing, the workflow may optionally pre-stage binaries to skip the `npm pack` step, but the same module supports both paths.

### 3.3 — ✅ JNA binding interface design

**Question:** What does the internal abstraction layer look like that isolates the JNA-specific code from the transport logic?

ADR-007 mandates an internal binding interface so a future FFM implementation can be swapped in. The .NET PR uses two `#if` interop backends behind the same `FfiRuntimeHost` class. For Java, we need:

```java
// Internal interface — not public API
interface NativeBinding {
    int hostStart(String entrypoint, String args);
    boolean hostShutdown(int serverHandle);
    int connectionOpen(int serverHandle, OutboundCallback callback, Pointer userData);
    boolean connectionWrite(int connectionHandle, byte[] data);
    boolean connectionClose(int connectionHandle);
}

@FunctionalInterface
interface OutboundCallback extends Callback {
    void invoke(Pointer userData, Pointer data, int length);
}
```

**Open questions:**

1. Should `NativeBinding` be a Java `interface` or an `abstract class`? An interface is cleaner for future FFM, but an abstract class could hold shared validation logic.
2. Should the binding be discovered via `ServiceLoader` (for multi-release JAR FFM override) or via direct instantiation in the transport class?
3. What package should this live in? `com.github.copilot.ffi` (new) or `com.github.copilot` (alongside `CliServerManager`)?

**Recommendation:** Use a Java `interface` in a new `com.github.copilot.ffi` package. Direct instantiation for now; `ServiceLoader` only if/when the FFM implementation ships as a multi-release JAR.

**Resolution:**

1. **Interface with default methods.** `NativeBinding` is a Java `interface`, not an abstract class. An interface is the natural seam for swapping implementations (JNA today, FFM later) and avoids committing to a class hierarchy. Any shared validation logic (e.g., checking handle != 0) can live in `default` methods on the interface or in the transport class that calls it, rather than in an abstract base class. This mirrors DJL's `EngineProvider` which is also a plain interface.

2. **Direct instantiation.** The transport class (e.g., `FfiTransport`) instantiates the JNA-backed `NativeBinding` implementation directly — no `ServiceLoader` indirection. Rationale: `ServiceLoader` solves a _user-selectable_ swappability problem (DJL uses it because a consumer chooses between PyTorch, TensorFlow, etc. by changing classpath dependencies). Our binding swap is _JDK-version-determined_, not user-chosen, which is exactly what multi-release JARs already handle. When a future FFM implementation ships (ADR-007 defers this), it goes in `META-INF/versions/25/` and the multi-release JAR mechanism itself selects the right class at load time — the same pattern the SDK already uses for `InternalExecutorProvider`. No `ServiceLoader` needed.

3. **Package: `com.github.copilot.ffi`.** New package, separate from the public API surface in `com.github.copilot`. Contains `NativeBinding`, `OutboundCallback`, the JNA implementation class, and the platform-detection/library-extraction logic. All classes are package-private or `@InternalApi`; consumers never reference them directly.

### 3.4 — ✅ JNA callback threading and lifecycle

**Question:** How should the native outbound callback (Rust → Java) be handled in JNA, particularly regarding thread safety and callback lifetime?

**Important constraint:** The entire JNA/callback/stream-bridging machinery described in this section is **conditionally instantiated** — it only exists when the user selects the InProcess transport (see 3.5). When the subprocess transport is selected (the default), none of this code runs. The existing subprocess path via `CliServerManager` remains completely unchanged.

The Rust FFI implementation (`ffi.rs` in PR #1915) uses a `CallbackState` with `AtomicUsize` tracking active callbacks, and waits for all active callbacks to drain before freeing the state. The .NET implementation uses a `GCHandle`-pinned delegate.

In JNA:

- `Callback` instances must remain reachable (not GC'd) for the duration of native use. If GC'd, the function pointer becomes dangling → JVM crash.
- JNA attaches the native thread to the JVM automatically when the callback is invoked.
- The callback is invoked on the native thread, not the Java thread that initiated the call.

**Open questions:**

1. How do we pipe callback data into the Java async world? Options:
   - `java.util.concurrent.LinkedBlockingQueue<byte[]>` — simple, but blocks a thread reading from it.
   - `CompletableFuture`-based chaining — matches SDK's existing async model.
   - `java.util.concurrent.Flow.Publisher` (reactive streams) — more complex but supports backpressure.
   - `java.io.PipedInputStream`/`PipedOutputStream` — maps to the existing `JsonRpcClient` which reads from an `InputStream`.

2. How do we ensure the JNA `Callback` instance is not GC'd while native code holds the function pointer? The .NET solution (`GCHandle`) has no direct analog; we need to hold a strong reference.

3. Should we track active callbacks (like Rust's `AtomicUsize`) to safely drain before shutdown?

**Spike needed:** Write a minimal JNA program that loads a test `.so`, registers a callback, and verifies callback invocation from a native thread. Confirm JNA's thread attachment behavior.

**Recommendation:** Use `PipedInputStream`/`PipedOutputStream` to bridge the callback into the existing `JsonRpcClient` input stream model. Hold the `Callback` instance as a field in the transport class (prevents GC). Track active callbacks with `AtomicInteger` and drain on close, mirroring the Rust pattern.

**Resolution:**

The spike at `1917-java-embed-rust-cli-runtime-remove-before-merge/spike-3-4-jna-callback-and-threading/` contains three proven artifacts an implementer must study before writing production code:

**Spike structure:**

- `rust-dll/` — A Rust `cdylib` crate exporting 5 `extern "C"` functions that simulate the real `runtime.node` C ABI (`host_start`, `host_shutdown`, `connection_open`, `connection_write`, `connection_close`). The `connection_open` function spawns a **new native thread** that invokes the callback multiple times, reproducing the real runtime's threading behavior. All functions are heavily instrumented with `println!` logging showing thread IDs at entry/exit.

- `java-program-that-invokes-rust-dll-jdk17/` — The initial JNA-only spike (JDK 17 baseline). Demonstrates the working `QueueInputStream` approach and documents the `PipedInputStream` failure. ❌❌❌Do not adopt this approach. This is just for illustration.❌❌❌

- `java-program-that-invokes-rust-dll-mr-jar-17-25/` — The multi-release JAR spike. A single `java -jar` that automatically selects a platform thread reader (JDK 17) or virtual thread reader (JDK 25) via the MR-JAR mechanism, matching the existing `InternalExecutorProvider` pattern. Both JDK versions use JNA for the native binding (FFM is deferred per ADR-007). Verified on JDK 17.0.18 and JDK 25.0.2. ✅✅✅This is the approach we will use.✅✅✅

**Answers to the open questions:**

1. **How to pipe callback data into Java:** Use `QueueInputStream` — a `BlockingQueue<byte[]>`-backed `InputStream`. **`PipedInputStream`/`PipedOutputStream` is REJECTED.** JNA creates a new short-lived Java thread for each callback invocation (observed as Thread-0, Thread-1, Thread-2... with different thread IDs). `PipedInputStream` tracks `writeSide` (the last thread that wrote) and checks `writeSide.isAlive()`. When a JNA callback thread terminates after returning, subsequent reads fail with `IOException: Write end dead`. This was discovered and reproduced in the spike. `QueueInputStream` has no thread-affinity checks and works correctly from any thread. On JDK 25, the reader thread consuming from `QueueInputStream` is a virtual thread (via `ReaderThreadFactory` MR-JAR overlay using `Thread.ofVirtual()`), which unmounts from its carrier while blocked on `queue.take()`, freeing the OS thread. On JDK 17, it is a platform thread.

2. **Callback GC protection:** Hold the JNA `Callback` instance as a strong-reference field in the transport class (e.g., `FfiRuntimeHost.callbackRef`). If this reference is GC'd, the native function pointer becomes dangling and the JVM will crash. There is no Java equivalent of .NET's `GCHandle`; a strong field reference is the correct pattern.

3. **Active callback tracking:** Use `AtomicInteger` to track the count of currently active callbacks, mirroring Rust's `AtomicUsize` in `CallbackState`. Drain (wait for count to reach zero) before calling `connection_close` / `host_shutdown` to ensure no callback is in-flight when the native resources are freed.

**Key implementation details for the production `com.github.copilot.ffi` package:**

- `QueueInputStream` — shared by both JDK 17 and JDK 25 paths. Lives in the base source tree.
- `ReaderThreadFactory` — MR-JAR swap point. Baseline at `src/main/java/.../ffi/ReaderThreadFactory.java` (platform thread), overlay at `src/main/java25/.../ffi/ReaderThreadFactory.java` (virtual thread). Same pattern as `InternalExecutorProvider`.
- `NativeBindingProvider` (or `JnaNativeBinding`) — JNA binding class. **Not** a MR-JAR swap point; JNA is used on all JDK versions. FFM is deferred per ADR-007.
- The `OutboundCallback` lambda must use `Pointer.getByteArray(0, len)` to copy the native buffer — the pointer is only valid for the duration of the callback invocation.

### 3.5 — ✅ Transport integration with `CopilotClient`

**Question:** How does the InProcess transport fit into the existing `CopilotClient` architecture?

**Key design principle:** The existing subprocess transport path via `CliServerManager` remains the **default and is completely unchanged**. The InProcess transport is strictly opt-in. `CopilotClient` must support both paths coexisting in the same codebase, with transport selection determining which path is instantiated at construction time. `FfiRuntimeHost` is a **parallel** class to `CliServerManager`, not a replacement — mirroring the .NET PR's approach where `if (_connection is InProcessRuntimeConnection)` takes the FFI path, else the existing subprocess/TCP path runs exactly as before.

Currently, `CopilotClient` uses `CliServerManager` to spawn a subprocess and connects via TCP JSON-RPC. The .NET PR adds `InProcessRuntimeConnection` as a new connection type alongside `StdioRuntimeConnection` and `TcpRuntimeConnection`. The Rust PR adds `Transport::InProcess` and `Transport::Default`.

For Java, we need to decide:

1. **How is InProcess transport selected?**
   - New option on `CopilotClientOptions` (e.g., `.setTransport(Transport.IN_PROCESS)`)?
   - Environment variable `COPILOT_SDK_DEFAULT_CONNECTION=inprocess` (matching Rust/Node)?
   - Automatic: try InProcess if native binary is on classpath, fall back to CLI subprocess?

2. **What replaces `CliServerManager` for InProcess?**
   - A new `FfiRuntimeHost` class (parallel to .NET's) that manages `host_start` → `connection_open` → duplex streams → `connection_close` → `host_shutdown`?
   - Or extend `CliServerManager` with an InProcess code path?

3. **How does the `JsonRpcClient` connect to the FFI streams?**
   - Currently `JsonRpcClient` reads from an `InputStream` and writes to an `OutputStream`. The FFI transport must provide compatible streams backed by the native callback (read) and `connection_write` (write).

```java
// Proposed addition to CopilotClientOptions
public enum Transport {
    /** Spawn CLI as subprocess, connect via TCP (current default). */
    CLI,
    /** Load runtime.node in-process via FFI. */
    IN_PROCESS,
    /** Use IN_PROCESS if native binary available, else fall back to CLI. */
    DEFAULT
}

public CopilotClientOptions setTransport(Transport transport) { ... }
```

**Recommendation:** Add a `Transport` enum and `setTransport()` on `CopilotClientOptions`. Create a new `FfiRuntimeHost` class (not extend `CliServerManager`). Provide `InputStream`/`OutputStream` wrappers over the FFI callback and `connection_write`.

**Resolution (3.5.1 — How is InProcess transport selected?):**

**RECOMMENDATION SUPERSEDED.** The `Transport` enum approach is rejected. Instead, adopt the .NET `RuntimeConnection` type hierarchy pattern via a new `setConnection(RuntimeConnection)` field on `CopilotClientOptions`.

**Rationale:** The existing Java options API (`setUseStdio(boolean)`, `setCliUrl(String)`, `setCliPath(String)`) is already messy — two interacting flags that implicitly select from three transport modes. Adding another boolean (`setUseInProcess`) would make it worse. An enum (`Transport.CLI`, `Transport.IN_PROCESS`) doesn't carry per-transport config (path, port, connection token) without the existing fields. The .NET SDK solved this cleanly with a sealed `RuntimeConnection` class hierarchy where each subclass carries only its own config, and `CopilotClientOptions.Connection` selects the transport.

**Design:** Add a sealed `RuntimeConnection` class with factory methods, mirroring .NET 1:1:

```java
public abstract sealed class RuntimeConnection
    permits StdioRuntimeConnection, TcpRuntimeConnection,
            UriRuntimeConnection, InProcessRuntimeConnection {

    RuntimeConnection() {} // package-private — only factory methods create instances

    public static StdioRuntimeConnection forStdio() { return new StdioRuntimeConnection(); }
    public static StdioRuntimeConnection forStdio(String path) { return new StdioRuntimeConnection().setPath(path); }
    public static TcpRuntimeConnection forTcp() { return new TcpRuntimeConnection(); }
    public static UriRuntimeConnection forUri(String url) { return new UriRuntimeConnection(url); }
    public static InProcessRuntimeConnection forInProcess() { return new InProcessRuntimeConnection(); }
}
```

Four concrete sealed subtypes:

| Java subclass                | .NET equivalent              | Transport                       | Config fields                             |
| ---------------------------- | ---------------------------- | ------------------------------- | ----------------------------------------- |
| `StdioRuntimeConnection`     | `StdioRuntimeConnection`     | stdin/stdout pipe to subprocess | `path`, `args`                            |
| `TcpRuntimeConnection`       | `TcpRuntimeConnection`       | TCP socket to subprocess        | `path`, `port`, `connectionToken`, `args` |
| `UriRuntimeConnection`       | `UriRuntimeConnection`       | TCP to external server          | `url` (required), `connectionToken`       |
| `InProcessRuntimeConnection` | `InProcessRuntimeConnection` | FFI via JNA C ABI               | _(none — uses bundled native library)_    |

**Usage for all transport choices:**

```java
// 1. Stdio subprocess (same as today's default useStdio=true)
new CopilotClientOptions().setConnection(RuntimeConnection.forStdio("/usr/local/bin/copilot"));

// 2. TCP subprocess (same as today's setUseStdio(false))
new CopilotClientOptions().setConnection(RuntimeConnection.forTcp().setPath("/usr/local/bin/copilot"));

// 3. External server (same as today's setCliUrl())
new CopilotClientOptions().setConnection(RuntimeConnection.forUri("localhost:3000"));

// 4. In-process FFI (NEW)
new CopilotClientOptions().setConnection(RuntimeConnection.forInProcess());

// 5. Backward compat — no connection set, infers from legacy fields
new CopilotClientOptions().setCliPath("/usr/local/bin/copilot"); // works exactly as today
```

**Backward compatibility:** The `connection` field on `CopilotClientOptions` is nullable (default `null`). When null, existing `useStdio`/`cliUrl`/`cliPath` logic runs unchanged. When non-null, `connection` takes precedence. If both `connection` and legacy fields are set, `CopilotClient` throws `IllegalArgumentException` at construction time.

**Package:** `com.github.copilot.rpc` (alongside `CopilotClientOptions`).

**Resolution (3.5.2 — What replaces `CliServerManager` for InProcess?):**

New `FfiRuntimeHost` class, parallel to `CliServerManager` — not an extension of it. This mirrors the .NET SDK's `FfiRuntimeHost.cs` exactly.

**Rationale:** `CliServerManager` is entirely about subprocess lifecycle (`ProcessBuilder`, command-line construction, `waitForPortAnnouncement`, stderr pumping, `Process` cleanup). `FfiRuntimeHost` is entirely about FFI lifecycle (`host_start` → `connection_open` → duplex streams via `QueueInputStream`/`connection_write` → `connection_close` → `host_shutdown`). Zero overlap in mechanics. Combining them would violate SRP, make the name misleading ("CliServerManager" doesn't manage a server when running in-process), and increase change risk to the stable subprocess path.

**Lifecycle managed by `FfiRuntimeHost`:**

1. Load native library (from classpath-extracted cache path)
2. `copilot_runtime_host_start(argv_json, env_json)` → `serverId`
3. `copilot_runtime_connection_open(serverId, callback, ...)` → `connectionId` + `QueueInputStream` fed by callback
4. Expose `getReceiveStream()` (the `QueueInputStream`) and `getSendStream()` (wraps `connection_write`)
5. `copilot_runtime_connection_close(connectionId)` on shutdown
6. `copilot_runtime_host_shutdown(serverId)` on shutdown

**Shared arg/env building:** Both `CliServerManager` and `FfiRuntimeHost` need to build argument arrays and environment maps from `CopilotClientOptions` (auth tokens, telemetry config, `--embedded-host`, `--no-auto-update`, etc.). If the duplication becomes non-trivial, extract a shared static helper (e.g., `RuntimeArgs.buildArgv(options)` / `RuntimeArgs.buildEnv(options)`). Defer this extraction until implementation reveals the actual overlap.

**Package:** `com.github.copilot.ffi` (alongside `NativeBindingProvider` and `QueueInputStream`).

**`CopilotClient.startCoreBody()` dispatch:**

```java
if (connection instanceof InProcessRuntimeConnection) {
    ffiHost = new FfiRuntimeHost(...);
    ffiHost.start();
    rpc = JsonRpcClient.fromStreams(ffiHost.getReceiveStream(), ffiHost.getSendStream());
} else if (optionsHost != null) {
    rpc = serverManager.connectToServer(null, optionsHost, optionsPort);
} else {
    // existing subprocess path — unchanged
    ProcessInfo processInfo = serverManager.startCliServer();
    ...
}
```

**Resolution (3.5.3 — How does `JsonRpcClient` connect to the FFI streams?):**

No spike needed — the 3.4 spike already proved the hard part, and the remaining piece is trivial.

`JsonRpcClient` accepts a plain `InputStream` + `OutputStream` in its private constructor (see `JsonRpcClient.java` lines 55–57). It currently has two factory methods: `fromProcess(Process)` (stdio) and `fromSocket(Socket)` (TCP). Add a third:

```java
public static JsonRpcClient fromStreams(InputStream in, OutputStream out) {
    return new JsonRpcClient(in, out, null, null);
}
```

The two stream implementations:

**Read side (`InputStream` ← native callback):** This is the `QueueInputStream` proven in the 3.4 spike (`spike-3-4-jna-callback-and-threading/java-program-that-invokes-rust-dll-mr-jar-17-25/`). The JNA `on_outbound` callback pushes `byte[]` chunks into a `BlockingQueue<byte[]>`; `QueueInputStream.read()` drains them. Verified on both JDK 17.0.18 and JDK 25.0.2. The 3.4 spike also proved that `PipedInputStream` does NOT work here (JNA creates a new short-lived thread per callback invocation, and `PipedInputStream` checks `writeSide.isAlive()` → "Write end dead").

**Write side (`OutputStream` → `connection_write`):** A trivial `FfiOutputStream` that delegates `write()` to the JNA `copilot_runtime_connection_write(connectionId, data, len)` binding. The native side copies the buffer synchronously before returning (documented in the C ABI table above), so no lifecycle concern. Implementation:

```java
class FfiOutputStream extends OutputStream {
    private final CopilotRuntimeLibrary lib;
    private final int connectionId;

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        byte[] slice = (off == 0 && len == b.length) ? b : Arrays.copyOfRange(b, off, off + len);
        if (!lib.copilot_runtime_connection_write(connectionId, slice, new com.sun.jna.NativeLong(len))) {
            throw new IOException("copilot_runtime_connection_write failed");
        }
    }

    @Override
    public void write(int b) throws IOException {
        write(new byte[]{(byte) b}, 0, 1);
    }
}
```

**Cleanup:** `JsonRpcClient.close()` already handles null `socket` and null `process` — when constructed via `fromStreams()`, both are null, so cleanup just closes the streams. The native lifecycle (`connection_close`, `host_shutdown`) is owned by `FfiRuntimeHost`, not by `JsonRpcClient`.

**Package:** `FfiOutputStream` in `com.github.copilot.ffi` (alongside `QueueInputStream` and `NativeBindingProvider`). `JsonRpcClient.fromStreams()` is a one-line addition to the existing `com.github.copilot.JsonRpcClient`.

### 3.6 — Platform detection implementation

**Question:** What is the exact implementation of platform detection, particularly the ELF PT_INTERP parsing for musl vs. glibc on Linux?

ADR-007 specifies reading the first 2 KB of `/proc/self/exe` and parsing the ELF PT_INTERP segment. This is the same approach as the `detect-libc` npm package.

**Open questions:**

1. Can we read `/proc/self/exe` from Java? (`/proc/self/exe` is a symlink to the JVM binary — on glibc Linux it will contain the glibc dynamic linker path, on Alpine/musl it will contain the musl path.)
2. Should the detector be in a standalone utility class (reusable) or inline in the loader?
3. Edge case: What about container environments where `/proc` is mounted but the JVM binary is from a different libc than the container's userspace? (This shouldn't happen in practice — the JVM must match the libc.)

**Spike needed:** Write a Java snippet that parses ELF PT_INTERP from `/proc/self/exe` on a glibc Linux system and on Alpine. Verify the dynamic linker paths match expectations (`/lib64/ld-linux-x86-64.so.2` vs. `/lib/ld-musl-x86_64.so.1`).

**Recommendation:** Standalone `PlatformDetector` class in `com.github.copilot.ffi` with methods `detectOs()`, `detectArch()`, `detectLinuxLibc()`, `detectClassifier()`. Pure Java, no dependencies. Unit-testable with mocked system properties and test ELF binaries.

**Resolution:**

Read these three spike apps before implementing production code:

- `1917-java-embed-rust-cli-runtime-remove-before-merge/spike-3-6-platform-detection-darwin-arm64/`
- `1917-java-embed-rust-cli-runtime-remove-before-merge/spike-3-6-platform-detection-linux-x64/`
- `1917-java-embed-rust-cli-runtime-remove-before-merge/spike-3-6-platform-detection-win32-x64/`

All three spikes converge on the same pure-Java detector shape:

1. `detectOs()` maps `os.name` to `darwin | linux | win32`.
2. `detectArch()` maps `os.arch` aliases (`amd64`/`x86_64`/`x64` and `aarch64`/`arm64`) to `x64 | arm64`.
3. `detectLinuxLibc()` runs only on Linux and reads `/proc/self/exe`, parses ELF `PT_INTERP` from the first 2 KB, then classifies:
   - contains `/ld-musl-` → `MUSL`
   - contains `/ld-linux-` → `GLIBC`
   - parse/read failure → `UNKNOWN`
4. `detectClassifier()` returns:
   - non-Linux: `<os>-<arch>`
   - Linux + MUSL: `linuxmusl-<arch>`
   - Linux + GLIBC/UNKNOWN: `linux-<arch>`

High-level per-spike notes:

- **darwin-arm64 spike:** exercises the generic detector and logs `os`, `arch`, `linuxLibc`, `classifier`; Linux-only ELF parsing is present but skipped on Darwin.
- **linux-x64 spike:** exercises full Linux path, parses and logs `PT_INTERP`, and explicitly validates expected glibc/musl linker patterns (`/ld-linux-x86-64.so.2` and `/ld-musl-x86_64.so.1`).
- **win32-x64 spike:** exercises non-Linux classification path, verifies `win32-x64`, and includes an explicit allow-list check for all 8 ADR-007 classifiers.

The three spikes were run on their respective hardware and confirm the platform-selection approach is deterministic.

How to extrapolate to triples without a dedicated spike:

- `linux-arm64` (`aarch64-unknown-linux-gnu`): same Linux logic as the linux-x64 spike; with `arch=arm64` and `PT_INTERP` containing `/ld-linux-`, classifier becomes `linux-arm64`.
- `linuxmusl-x64` (`x86_64-unknown-linux-musl`): already covered by the linux spike’s MUSL branch; when `PT_INTERP` contains `/ld-musl-`, classifier is `linuxmusl-x64`.
- `linuxmusl-arm64` (`aarch64-unknown-linux-musl`): same MUSL detection; with `arch=arm64`, classifier becomes `linuxmusl-arm64`.
- `darwin-x64` (`x86_64-apple-darwin`): same Darwin logic as darwin-arm64 spike; with `arch=x64`, classifier becomes `darwin-x64`.
- `win32-arm64` (`aarch64-pc-windows-msvc`): same Windows logic as win32-x64 spike; with `arch=arm64`, classifier becomes `win32-arm64`.

Implementation guidance for production `com.github.copilot.ffi.PlatformDetector`:

- Keep detector as a standalone utility class (not inline in loader), with `detectOs()`, `detectArch()`, `detectLinuxLibc()`, `detectClassifier()`.
- Keep ELF parsing logic private and pure Java (no subprocesses, no external dependencies).
- Keep classifier derivation table-driven and include an allow-list for the 8 supported ADR-007 classifiers so unsupported tuples fail fast.

### 3.7 — Native binary extraction and caching

**Question:** What is the exact extraction and caching strategy for the `runtime.node` binary?

ADR-007 proposes extracting from classpath to `~/.copilot/runtime-cache/<version>/<classifier>/runtime.node`. Open questions:

1. **Version source:** Where does the version come from? `getClass().getPackage().getImplementationVersion()` relies on the JAR manifest. Is this set by the build? What about running from an IDE (un-jarred classes)?
2. **Atomicity:** If two JVM processes start simultaneously and both try to extract, how do we prevent corruption? Options: temp file + atomic rename, file locking, check-then-extract with size/checksum verification.
3. **Cache invalidation:** Should we verify integrity (e.g., file size or hash) on each startup, or trust the version-keyed path?
4. **Permissions:** On Unix, the extracted binary needs `chmod +x`. The ADR's `cached.toFile().setExecutable(true)` works — but note `runtime.node` is a shared library, not an executable. Shared libraries loaded via `dlopen` (which JNA uses internally) do **not** need execute permission on most Linux systems. Verify.
5. **Cleanup:** Should old versions in the cache be cleaned up? The .NET and Rust SDKs don't do this.

**Recommendation:** Use temp file + atomic rename for extraction. Trust the version-keyed path after a cheap regular/non-empty check. Don't clean up old versions. Do not set executable permission on the shared library. Use the primary artifact version from the top-level POM, injected into a `.properties` resource, for version identification.

**Resolution:**

Extract the classpath resource `native/<classifier>/runtime.node` to
`~/.copilot/runtime-cache/<version>/<classifier>/runtime.node` on first use.

1. **Version source: the primary artifact version from the top-level POM.** Maven resource filtering writes `${project.version}` to a properties resource in the SDK artifact. `NativeRuntimeLoader` reads that resource; it does not use `Package.getImplementationVersion()`. This works for a packaged JAR and for IDE execution after Maven resource processing, because the filtered resource is also present under `target/classes`. A missing or blank version resource is a build/configuration error and must produce a clear exception rather than sharing an `unknown` cache directory.

2. **Atomicity: unique sibling temp file plus atomic publish; no file lock.** The extraction sequence is:
   1. Return an existing cache entry if it is a regular, non-empty file.
   2. Create the cache directory and a unique temp file in that same directory with `CREATE_NEW`.
   3. Copy the classpath resource to the temp file, reject an empty result, flush it, and call `FileChannel.force(true)` before publication.
   4. Publish with `Files.move(temp, cached, ATOMIC_MOVE)`. The sibling temp file guarantees the move stays on one filesystem. Concurrent publishers contain identical version/classifier bytes, so either winner is valid. If another process publishes first and the move reports that the target exists, accept the winner after the same regular/non-empty check. If the filesystem does not support atomic moves, fail with a clear extraction error rather than expose a partially published native library.
   5. Delete the caller's temp file in a `finally` block when publication does not consume it.

The considered mechanisms have these tradeoffs:

| Mechanism                                            | Pros                                                                                                                                                                          | Cons                                                                                                                                                                                                                                                                                  | Decision                                                                                                                               |
| ---------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------- |
| Unique temp file + atomic rename                     | Readers never observe a partially written final file; process crashes leave only an unreferenced temp file; no process-wide coordination; the common path is simple and fast. | Requires a sibling temp file and atomic-move support; concurrent processes can duplicate extraction work; abandoned temp files are possible after a hard crash; atomic replacement behavior differs when a target already exists, so the loser must explicitly accept a valid winner. | **Use this as the publication mechanism.**                                                                                             |
| File locking                                         | Serializes writers and avoids duplicate extraction work; allows validation and repair to happen under one coordinator.                                                        | Locks are advisory; semantics differ across platforms and filesystems; overlapping locks in one JVM need special handling; lock files and exceptional cleanup add failure modes; a lock does not itself prevent a partial final write or prove integrity.                             | **❌❌❌Do not use.❌❌❌** The small amount of duplicate first-run I/O is preferable to permanent lock-management complexity.         |
| Check, then extract, with size/checksum verification | A size check catches empty/truncated files; a cryptographic hash detects arbitrary corruption and can validate the winner of a race.                                          | Check-then-act alone is racy and is not a publication mechanism; size is not an integrity proof; hashing a 48-65 MB library on every startup adds I/O; a trusted expected hash must be shipped; local same-user modification remains subject to a check/load TOCTOU race.             | **Use only the cheap regular/non-empty sanity check.** Atomic publication prevents partial first writes; do not hash on every startup. |

3. **Cache invalidation: version key plus cheap sanity check, not a startup hash.** Released artifact versions are immutable, so `<version>/<classifier>` is the invalidation boundary. On each load, require a regular, non-empty file. A missing, empty, or non-regular entry is treated as a cache miss and republished atomically. Do not compute a full-file hash on each startup.
   - **.NET:** It does not perform Java-style runtime extraction at application startup. MSBuild downloads and extracts the version/platform npm tarball under `$(IntermediateOutputPath)copilot-cli/<version>/<platform>`, then copies `runtime.node` to the build output. An existing CLI binary is treated as the cache hit; there is no runtime size/hash validation, and `FfiRuntimeHost` loads the output library by absolute path.
   - **Rust:** The build script SHA-512-verifies every downloaded or cached npm archive against npm integrity metadata. For the embedded in-process runtime library itself, runtime installation accepts an existing regular file when its length is greater than zero; otherwise it extracts non-empty trusted embedded bytes to a unique temp file and renames it into place. It does not hash the installed runtime library on every startup. Rust's CLI executable path is deliberately stricter (verified publication plus a size/header marker), but that is not the policy currently used for the shared runtime library.

4. **Permissions: do not set the execute bit on `runtime.node`.** The `spike-3-6-platform-detection-linux-x64` spike now includes a direct JNA permission probe. In an Ubuntu 22.04/glibc container with OpenJDK 17, it compiled a shared object, set its mode to `0644`, loaded it by absolute path through JNA 5.16.0, invoked an exported function, and exited successfully:

   ```text
   FILE_MODE=644
   INFO: PASS: JNA loaded and invoked a shared library with permissions [OWNER_WRITE, OTHERS_READ, GROUP_READ, OWNER_READ]
   JAVA_EXIT_CODE=0
   ```

   Linux `dlopen` needs permission to read/map the shared object; it does not require a filesystem execute bit as `execve` does. A `noexec` mount can still reject executable mappings, and adding the file execute bit does not fix that mount policy. The Rust build packages the runtime library with mode `0644`, although its current runtime extraction helper also serves the CLI executable and incidentally changes the extracted copy to `0755`. .NET does not chmod the library before `NativeLibrary.Load`. Therefore Java must preserve normal extracted-file permissions and must not call `setExecutable(true)`.

5. **Cleanup: none.** Do not delete old cache versions automatically. Versioned entries are retained until the user or an external cache-management policy removes them.

### 3.8 — JNA dependency management

**Question:** How should JNA be added as a dependency, and what version constraints apply?

The Java SDK currently has no JNA dependency. Adding it introduces:

1. **Version selection:** JNA 5.x is current. The latest is 5.16.0 (as of 2025). It supports Java 8+. The SDK targets Java 17.
2. **Transitive impact:** JNA brings `jna-platform` optionally. We likely only need `jna` (core), not `jna-platform`.
3. **Scope:** Should JNA be a required dependency or optional? If the SDK works without native binaries (subprocess transport), JNA is only needed for InProcess transport. Making it `<optional>true</optional>` means consumers using only CLI transport don't pull it in.
4. **GraalVM native-image:** JNA has established `native-image.properties` in its JAR. Verify this works for the callback pattern we need.

**Recommendation:** Add JNA as an `<optional>true</optional>` dependency. Only required when using InProcess transport. Use `jna` (not `jna-platform`). Version 5.16.0 or later.

**Resolution:**

Use JNA core 5.19.1 as an optional compile dependency of the SDK module:

```xml
<dependency>
    <groupId>net.java.dev.jna</groupId>
    <artifactId>jna</artifactId>
    <version>5.19.1</version>
    <optional>true</optional>
</dependency>
```

Actionable dependency decisions:

1. **Pin 5.19.1; do not use a Maven version range or the earlier `5.16.0 or later` recommendation.** Version 5.19.1 is the version exercised by `spike-3-8-graal-research`. Keep the version in a Maven property so upgrades are deliberate. A JNA upgrade must rerun the callback spike rather than relying only on compilation or ordinary downcalls.
2. **Depend on `net.java.dev.jna:jna` only.** The required APIs (`Native`, `Library`, `Callback`, and `Pointer`) are in core JNA. Do not add `jna-platform`; the spike does not use it and the runtime ABI needs none of its platform wrappers.
3. **Keep JNA optional because only InProcess transport needs it.** Maven optionality prevents subprocess-only consumers from receiving JNA transitively. Consequently, a consumer that explicitly selects InProcess transport must place JNA 5.19.1 on its runtime classpath in addition to the appropriate `copilot-sdk-java-runtime` classifier artifact. If InProcess is explicitly selected without JNA, fail with a clear dependency/setup error. The default subprocess transport must not initialize or load JNA.
4. **Do not claim GraalVM Native Image support for the JNA-backed InProcess transport.** The spike proves that ordinary JNA downcalls work in the tested native executable, but the callback upcall required by `connection_open` does not. Application-specific proxy, reflection, and JNI reachability metadata allows Native Image to load the JNA interface and create the callback function pointer, but invoking it fails before Java callback code executes. More metadata is therefore not a demonstrated remedy.
5. **Treat JVM support and Native Image support as separate compatibility claims.** On the regular JVM, the spike passed one synchronous callback and five callbacks from a Rust-created native thread, including `QueueInputStream` delivery and cleanup. That validates the callback design for the tested JVM stack; it does not validate a Native Image executable.

Evidence and implementation details are in `1917-java-embed-rust-cli-runtime-remove-before-merge/spike-3-8-graal-research/`, especially `java-program-that-invokes-rust-dll-jdk17/README.md` and its `reachability-metadata.json`.

**Explicit scope of the GraalVM result:** The experiment ran only on Windows x64 using Oracle GraalVM 25.0.4+7.1, JNA 5.19.1 core, Native Build Tools Maven plugin 0.11.3, Maven 3.9.14, Visual Studio Build Tools 2022 17.14, and Windows SDK 10.0.26100.0. The 21 MB native executable built successfully, loaded `jnidispatch.dll` and the Rust DLL, and completed ordinary native calls. A same-thread callback then failed with `java.lang.Error: Invalid memory access`; the Rust-thread callback separately crashed in `JNIJavaCallTrampolineHolder.varargsJavaCallTrampoline`. Because the same-thread control also failed, the observed blocker is JNA callback upcalls in this configuration, not attachment of Rust-created threads.

The spike did **not** test Linux, macOS, Windows arm64, any Linux libc/architecture combination, other GraalVM distributions or versions, other JNA versions, or Native Build Tools 1.1.6 (that plugin failed during Maven extension initialization under Maven 3.9.14 before Native Image compilation). Do not extrapolate the failure to every Native Image platform, but do not enable or advertise JNA-backed Native Image support on any platform without a passing callback test for that exact OS, architecture, GraalVM, and JNA combination. Until such a matrix passes, Native Image users must use subprocess transport rather than InProcess transport.

❌❌❌❌As a result of this spike, we will not pursue GraalVM native image support at all for this feature. The responsible human has decided that if someone wants native performance, they will choose Rust.❌❌❌

### 3.9 — C ABI parameter semantics

**Question:** What are the exact semantics of every parameter across all five C ABI functions?

The C ABI table at the top of this plan names each parameter but does not explain what values to pass or what invariants the runtime enforces. An implementer reading the table alone cannot write production code.

#### `copilot_runtime_host_start(argv_json, argv_json_len, env_json, env_json_len)`

1. **`argv_json`** — The plan table shows the example `["copilot","--embedded-host"]`. What is the full set of valid arguments? Is `--embedded-host` required, optional, or inferred? What other flags does the runtime accept or require in embedded mode?
2. **`env_json`** — The plan says this is an optional JSON object of environment overrides. What are the valid keys? At minimum: what key carries the GitHub auth token, what keys carry proxy URLs, what key controls log level, and are there any other keys the runtime reads? A complete key inventory is required — not just "study the .NET and Rust SDKs."
3. **Nullability** — Can either buffer be passed as a null pointer with length 0? Is a zero-length `argv_json` treated as "use defaults" or as an error?
4. **Return value** — When `host_start` returns 0 (failure), is there a companion error-retrieval function, or is the only diagnostic stderr output? (Relates to 3.10 but the answer determines how much error context the Java caller can surface.)

#### `copilot_runtime_connection_open(server_id, on_outbound, user_data, ext_source, ext_source_len, ext_name, ext_name_len, conn_token, conn_token_len)`

5. **`ext_source`** — What is this semantically? An extension/plugin identifier? A source URI? The table says it is a nullable metadata buffer; the spike fixture omits it entirely. When is it required vs. safe to pass null?
6. **`ext_name`** — What is the relationship to `ext_source`? Is this a human-readable label for the same extension? Does the runtime use it for logging, routing, or access control?
7. **`conn_token`** — Is this a per-connection authentication token distinct from the global auth token passed via `env_json` at `host_start`? If so, when would per-connection tokens differ from the global token? What format — opaque bytes, JWT, something else?
8. **`user_data`** — The Spike 3.4 fixture passes `Pointer.NULL` and the callback captures Java state via constructor fields rather than via `user_data`. Confirm whether `user_data = null` is safe with the real runtime, and document that the Java implementation should always pass null, relying on Java closure capture instead of the C void-pointer cookie mechanism.
9. **Multiple concurrent connections** — The handle-per-connection ABI design implies multiple connections per server handle are possible. Confirm whether the runtime supports N concurrent open connections on one server handle, or whether the expected usage is one connection at a time (as .NET and Rust both do in practice).

#### Wire format of `connection_write` and `on_outbound`

10. **Frame format** — The table says `connection_write` writes "a JSON-RPC frame." What exactly is a frame? Length-prefixed (4-byte big-endian)? LSP `Content-Length` header? Newline-delimited? The `on_outbound` callback delivers frames in the same format. The Spike 3.4 `QueueInputStream` bridge uses a 4-byte length prefix as a local convention, but the real runtime may use something different. This must be confirmed against the actual implementation.
11. **Buffer lifetime for `connection_write`** — Does the runtime copy the buffer before returning, or does it read the buffer asynchronously? The .NET PR comments that the native side copies synchronously. Confirm this — it determines whether the Java caller must keep the byte array alive after the call returns.

**Spike needed (`spike-3-9-deep-entrypoint-questions`):** Read `copilot_runtime_host_start` and `copilot_runtime_connection_open` in `github/copilot-agent-runtime` `src/runtime/src/interop/cabi.rs`. Read how the .NET SDK (`FfiRuntimeHost.cs`) and Rust SDK (`ffi.rs`) construct every parameter. Produce a **complete call-by-call reference** — for each parameter of each function, state the value the Java implementation must pass, the format, and the nullability rule. Explicitly confirm or deny items 1–11 above.

**Resolution:**

Read the full evidence and analysis in `1917-java-embed-rust-cli-runtime-remove-before-merge/spike-3-9-c-abi-parameter-semantics/` before implementing. The spike reviewed all five production SDK implementations (Rust `ffi.rs`, .NET `FfiRuntimeHost.cs`, Node.js `ffiRuntimeHost.ts`, Go `ffihost.go`, Python `_ffi_runtime_host.py`) and their client-side parameter construction code.

**Actionable parameter specification for `copilot_runtime_host_start`:**

| Parameter       | Format                      | Value the Java implementation must pass                                                                                                                                          |
| --------------- | --------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `argv_json`     | UTF-8 JSON array of strings | `[entrypoint, "--embedded-host", "--no-auto-update", ...optional_args]`. Prefix with `"node"` if entrypoint ends in `.js`.                                                       |
| `argv_json_len` | `size_t`                    | Byte length of the JSON text above.                                                                                                                                              |
| `env_json`      | UTF-8 JSON object or null   | `{"COPILOT_SDK_AUTH_TOKEN":"<token>", "COPILOT_HOME":"<path>", "COPILOT_DISABLE_KEYTAR":"1"}` — include only keys that apply; pass **null with len=0** when no overrides needed. |
| `env_json_len`  | `size_t`                    | Byte length of env JSON, or 0 when null.                                                                                                                                         |

Optional arguments appended to `argv_json` after the two required flags:

| Flag                                      | Condition                                                           |
| ----------------------------------------- | ------------------------------------------------------------------- |
| `--log-level <level>`                     | `options.logLevel` is set                                           |
| `--auth-token-env COPILOT_SDK_AUTH_TOKEN` | `options.githubToken` is provided                                   |
| `--no-auto-login`                         | `useLoggedInUser` is false (default when `githubToken` is provided) |
| `--session-idle-timeout <seconds>`        | `options.sessionIdleTimeoutSeconds > 0`                             |
| `--remote`                                | `options.enableRemoteSessions` is true                              |

Complete `env_json` key inventory (these are the **only** three keys used across all five SDKs):

| Key                      | Value                            | Condition                         |
| ------------------------ | -------------------------------- | --------------------------------- |
| `COPILOT_SDK_AUTH_TOKEN` | The GitHub token string          | `options.githubToken` is provided |
| `COPILOT_HOME`           | Copilot base/home directory path | `options.baseDirectory` is set    |
| `COPILOT_DISABLE_KEYTAR` | `"1"`                            | `options.mode == "empty"`         |

**Actionable parameter specification for `copilot_runtime_connection_open`:**

| Parameter                       | Value the Java implementation must pass                                                                                            |
| ------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------- |
| `server_id`                     | The non-zero handle from `host_start`                                                                                              |
| `on_outbound`                   | JNA `Callback` function pointer (held as strong field reference)                                                                   |
| `user_data`                     | **`Pointer.NULL`** — safe; runtime passes it back unmodified; Java uses closure/field capture instead of the C void-pointer cookie |
| `ext_source` / `ext_source_len` | **`null, 0`** — reserved/future; all 5 SDKs pass null                                                                              |
| `ext_name` / `ext_name_len`     | **`null, 0`** — reserved/future; all 5 SDKs pass null                                                                              |
| `conn_token` / `conn_token_len` | **`null, 0`** — reserved/future; all 5 SDKs pass null                                                                              |

**Three key invariants:**

1. **`argv_json` must never be null.** It always contains at least `[entrypoint, "--embedded-host", "--no-auto-update"]`. **`--no-auto-update` is mandatory** — it pins the worker to the bundled cdylib version, preventing ABI skew between the loaded library and the runtime worker. Omitting it allows the runtime to drift to a newer `~/.copilot/pkg` version whose ABI may be incompatible with the loaded cdylib.
2. **`env_json` can be null** (with `env_json_len = 0`) when no environment overrides are needed.
3. **All three metadata buffers (`ext_source`, `ext_name`, `conn_token`) are always null/0.** No current SDK uses them; they are reserved extension points.

**Wire format and buffer lifetime:**

- **Frame format:** LSP `Content-Length: <n>\r\n\r\n<payload>` — identical to the stdio transport. NOT binary length-prefixed. The existing Java `JsonRpcClient` handles this framing unchanged; no special encoding/decoding is needed at the FFI boundary.
- **Buffer lifetime:** `connection_write` copies the buffer synchronously before returning. The Java byte array does not need to survive past the JNA call.
- **Callback buffer lifetime:** The `on_outbound` callback's `data` pointer is only valid for the duration of the callback invocation. The callback must copy bytes out (via `Pointer.getByteArray(0, len)`) before returning.

**Additional confirmed behaviors:**

- **No error retrieval function:** The C ABI has no `copilot_runtime_last_error` export. Failure is indicated solely by return value (0 for handles, false for booleans). The Java implementation must format its own diagnostic messages.
- **One connection per server:** All 5 SDKs open exactly one connection per server handle. The Java implementation should follow the same pattern.
- **Shutdown sequence:** Set closing flag → `connection_close(connectionId)` → drain active callbacks (wait for `AtomicInteger` to reach 0) → `host_shutdown(serverId)` → release callback reference.

**Answers to the 11 original questions (summary):**

1. Full argv set — documented in table above.
2. Complete env key inventory — exactly 3 keys, documented above.
3. Nullability — argv never null; env can be null.
4. Error retrieval — none; return value only.
5. `ext_source` — reserved/future; pass null.
6. `ext_name` — reserved/future; pass null.
7. `conn_token` — reserved/future; pass null. Unrelated to global auth.
8. `user_data = null` — confirmed safe by 3 SDKs that pass null in production.
9. Multiple connections — architecturally possible but unused; one per server.
10. Frame format — LSP `Content-Length:` header framing.
11. Buffer lifetime — native copies synchronously; no retention needed.

### 3.10 — Error handling and diagnostics

**Question:** How should FFI-level errors be surfaced to the Java SDK user?

The C ABI functions return `uint32_t` handles or `bool` success flags. When they fail:

1. Is there an error message channel? (e.g., a `copilot_runtime_last_error` function, or is error info logged to stderr?)
2. Should FFI failures be wrapped in a new exception type (e.g., `FfiTransportException`) or use existing SDK exception types?
3. How should the SDK handle a native crash/abort (e.g., Rust panic that unwinds through FFI)? JNA's protected mode can catch `SIGSEGV` on some platforms, but this is best-effort.
4. How should the SDK log FFI-level diagnostics (library loading, callback events)?

**Recommendation:** Wrap FFI failures in a new `FfiTransportException extends RuntimeException`. Use `java.util.logging` consistent with the rest of the SDK. Document that a native abort (Rust panic) terminates the JVM — this is the cost of in-process hosting, mitigated by the fact that the runtime is extensively tested.

**Resolution:**

The error handling strategy mirrors .NET's approach (no dedicated exception type, descriptive diagnostic strings, best-effort teardown) with two Java-specific improvements: defensive callback wrapping and JNA's `Callback.UncaughtExceptionHandler` as a secondary safety net.

**Answers to the four questions:**

**1. Is there an error message channel?**

No. The C ABI has no `copilot_runtime_last_error` export — confirmed by examining all five SDK implementations and the ABI surface. `Native.getLastError()` (which retrieves OS-level `errno`/`GetLastError`) is irrelevant because the Rust runtime does not set OS error codes; it returns 0 on failure. All five SDKs construct their own diagnostic strings from the library path and entrypoint path. Java must do the same. There is nothing additional to retrieve.

**2. Should FFI failures use a new exception type or existing SDK types?**

**RECOMMENDATION SUPERSEDED.** No dedicated `FfiTransportException`. Use `IllegalStateException` — the standard Java analog of .NET's `InvalidOperationException`, which is what .NET uses for every FFI failure. .NET has no dedicated FFI exception type either, and the existing Java SDK already uses `IllegalStateException` for "operation cannot proceed" scenarios (e.g., `CopilotSession`: "Session is not connected — RPC client is unavailable").

Specific error messages match the .NET pattern verbatim for consistency across SDKs:

| Failure                          | Exception               | Message                                                                                                                                                  |
| -------------------------------- | ----------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Library not found                | `IllegalStateException` | `"FFI runtime library not found. Looked for '{path1}' and '{path2}'."`                                                                                   |
| Library load failure             | `IllegalStateException` | `"Failed to load FFI runtime library '{path}'."`                                                                                                         |
| Missing export                   | `IllegalStateException` | `"FFI runtime library is missing the '{export}' export."`                                                                                                |
| `host_start` returns 0           | `IllegalStateException` | `"copilot_runtime_host_start failed (library '{libPath}', entrypoint '{entrypoint}')."`                                                                  |
| `connection_open` returns 0      | `IllegalStateException` | `"copilot_runtime_connection_open failed."`                                                                                                              |
| `connection_write` returns false | `IOException`           | `"Failed to write a frame to the in-process runtime connection."`                                                                                        |
| Write on closed connection       | `IOException`           | `"The in-process runtime connection is closed."`                                                                                                         |
| Duplicate library load           | `IllegalStateException` | `"An in-process FFI runtime library is already loaded from '{path1}'; loading a different library from '{path2}' in the same process is not supported."` |

**3. How should the SDK handle a native crash/abort?**

Nothing special. A Rust panic that unwinds through the FFI boundary terminates the process — this is the cost of in-process hosting. .NET does nothing special (no SEH guards, no `AccessViolationException` catching). JNA's `Native.setProtected(true)` can catch `SIGSEGV` on some platforms, but the JNA documentation warns it is unreliable, interferes with the JVM's own signal handling, should only be used for testing/debugging, and "should not be considered reliable or robust." The Java implementation must NOT enable protected mode. The mitigation is that the Copilot runtime is extensively tested and the C ABI is designed with `catch_unwind` at the FFI boundary (Rust prevents unwinding across `extern "C"` functions by default since Rust 1.71).

**4. How should the SDK log FFI-level diagnostics?**

Use `java.util.logging` — the logging framework already used throughout the Java SDK (`CliServerManager`, `CopilotClient`, `JsonRpcClient`, etc.). Use a logger named for the FFI class (e.g., `Logger.getLogger(FfiRuntimeHost.class.getName())`).

Logging points (matching .NET's `FfiRuntimeHost` logging):

| Event                                     | Level                   | Content                                |
| ----------------------------------------- | ----------------------- | -------------------------------------- |
| Successful start                          | `FINE` (= .NET `Debug`) | Library path, server ID, connection ID |
| `connection_close` failure during dispose | `FINE`                  | Exception message (swallowed)          |
| `host_shutdown` failure during dispose    | `FINE`                  | Exception message (swallowed)          |
| Callback exception (caught in try-catch)  | `WARNING`               | Full exception with stack trace        |

**Additional Java-specific decisions:**

**Callback error containment (better than .NET, matching Go/Python):**

.NET's outbound callback does NOT wrap in try-catch — if `FeedInbound` throws, the exception propagates into native code. Go and Python are more defensive: Go uses `recover()` with the comment "Nothing may panic across the FFI boundary"; Python catches all exceptions and logs them.

Java must follow the Go/Python pattern, not .NET's, for two reasons:

1. **Primary defense: wrap the callback body in try-catch.** The `on_outbound` callback implementation must catch all `Throwable` (including `Error`), log via `java.util.logging` at `WARNING` level, and return normally. This prevents any Java exception from reaching the native caller.

2. **Secondary defense: register a `Callback.UncaughtExceptionHandler`.** JNA's `Callback` contract states: "A callback should generally never throw an exception [...] Any exceptions thrown will be passed to the default callback exception handler." The default handler prints to stderr. The Java implementation should register a custom handler via `Native.setCallbackExceptionHandler()` that logs via `java.util.logging` instead, as a belt-and-suspenders defense for any exception that slips past the primary try-catch.

**Dispose/close error handling (matching .NET, leveraging `AutoCloseable`):**

`FfiRuntimeHost` implements `AutoCloseable`. The `close()` method:

1. Sets a `disposed` flag.
2. Calls `connection_close(connectionId)` — wrapped in try-catch, failure logged at `FINE` and swallowed.
3. Drains active callbacks (wait for `AtomicInteger` count to reach 0).
4. Calls `host_shutdown(serverId)` — wrapped in try-catch, failure logged at `FINE` and swallowed.
5. Closes the `QueueInputStream` receive buffer.
6. Releases the JNA `Callback` reference (sets to null).

`close()` must always complete — it must never throw. This matches .NET's `Dispose()` pattern and supports the Java SDK's existing `AutoCloseable` usage (try-with-resources).

### 3.11 — E2E testing with InProcess transport

**Question:** How should E2E tests exercise the InProcess transport?

The existing Java E2E tests use `E2ETestContext` which starts a replay proxy (Node.js-based `CapiProxy`). The .NET PR adds `Should_Start_And_Connect_Over_InProcess_Ffi`. The Rust PR adds `inprocess.rs` E2E test. Notably, the Rust PR runs the **entire** existing E2E suite with `COPILOT_SDK_DEFAULT_CONNECTION=inprocess` set, exercising the full test matrix over the in-process transport — not just a single smoke test.

For Java:

1. Can E2E tests use the InProcess transport against the replay proxy? The replay proxy is a network endpoint — InProcess transport bypasses network entirely. These are different transport paths.
2. Should InProcess E2E tests use a **real** `runtime.node` binary? This would require the binary to be available in CI.
3. How do we mock/stub the native library for unit testing the JNA binding layer without a real `runtime.node`?
4. Should InProcess E2E tests reuse existing YAML snapshots, or do they need separate snapshots?
5. **Should the entire existing E2E test suite be run with each valid transport (subprocess and InProcess)?** The Rust PR does this — the same E2E tests run in a separate CI job with `COPILOT_SDK_DEFAULT_CONNECTION=inprocess`, providing confidence that both transport paths produce identical behavior. The researcher should determine whether the Java E2E suite can be structured the same way (e.g., a separate Maven profile or CI matrix entry that sets the transport to InProcess and re-runs the full suite).

**Spike needed:** Determine whether the replay proxy can be adapted to work with InProcess transport, or if InProcess tests must use the real runtime binary. Determine whether the full E2E suite can run under both transports, or if certain tests are inherently transport-specific.

**Recommendation:** InProcess E2E tests use the real `runtime.node` binary (not the replay proxy). They run only in CI environments where the binary is available, gated by a Maven profile or system property. Existing YAML snapshots are orthogonal (they're for the replay proxy). Unit tests for the binding layer use a test `.so`/`.dylib` with a minimal C ABI surface. The full E2E suite should be run under both subprocess and InProcess transports in CI, mirroring the Rust PR's approach.

**Resolution:**

Read the full evidence in `1917-java-embed-rust-cli-runtime-remove-before-merge/spike-3-11-replay-proxy-and-in-process/`. The spike ran the complete InProcess flow on win32-x64 (JDK 25.0.2, JNA 5.19.1, `runtime.node` 1.0.73) and produced a successful ping–pong round trip in 1.1 s. All five answers are now definitive.

**Answer 1: Can E2E tests use the InProcess transport against the replay proxy?**

**YES.** The replay proxy intercepts HTTP calls (to `COPILOT_API_URL`). The in-process runtime library is loaded into the test process via JNA and reads `COPILOT_API_URL` from the **native process environment block** — not from Java's `System.getenv()` snapshot or any per-client dictionary. To redirect traffic to the proxy, the Java E2E harness must write `COPILOT_API_URL=<proxyUrl>` into the live environment block **before** `copilot_runtime_host_start` is called.

Java has no stdlib API for this. The solution is a new `InProcessEnvGuard` class (see `spike-3-11/java-inprocess-e2e-win32-x64/`) that calls `SetEnvironmentVariableW` (Windows) or `setenv()` (Linux/macOS) via JNA to mutate the process environment, and restores saved values on `close()`. This is the Java analog of:

- Rust: `InProcessEnvGuard` in `rust/tests/e2e/support.rs` (lines 603–677)
- .NET: `InProcessEnvIsolation.Apply()` in `dotnet/test/Harness/InProcessEnvIsolation.cs`

**Critical constraint: E2E concurrency must be 1 when running in-process.** The guard mutates process-global state. Concurrent tests would race on env writes. Rust enforces `concurrency = 1` via semaphore when `COPILOT_SDK_DEFAULT_CONNECTION=inprocess`; Java must do the same (e.g., via `surefire.forkCount=1` or a JUnit 5 `@ResourceLock`).

**Answer 2: Should InProcess E2E tests use a real `runtime.node` binary?**

**YES** (DRI decision). The binary is the same one packaged by the `copilot-native` Maven module (from `npm pack @github/copilot-win32-x64@<version>`). CI makes it available wherever the `copilot-native` module has run. The spike confirms this binary works correctly with JNA.

**ABI version sensitivity:** The `runtime.node` in `@github/copilot-win32-x64@1.0.69-0` (the version currently installed in `nodejs/node_modules`) is missing `copilot_runtime_host_start`. Version `1.0.73` (pinned in `nodejs/package-lock.json`) has both the old `host_start`/`host_shutdown` API and the newer `server_create`/`server_remove` API. The `copilot-native` module's `npm pack` downloads from `package-lock.json`, ensuring `1.0.73` (or newer matching the lock) is used in production — the same version that the spike verifies works.

**Answer 3: How do we mock/stub the native library for unit testing the JNA binding layer?**

**We don't** (DRI decision). Only E2E tests (running with the real binary) exercise the JNA binding layer. Unit tests for the `com.github.copilot.ffi` package (step 4.3) use the minimal Rust test DLL from spike-3-4 for component-level testing of the callback/stream machinery. There is no middle tier of "mock runtime.node".

**Answer 4: Should InProcess E2E tests reuse existing YAML snapshots?**

**YES.** From the replay proxy's perspective, HTTP traffic is identical whether the runtime was launched as a subprocess or loaded in-process — only the transport inside the JVM changes. The Rust `inprocess.rs` smoke test reuses the same `should_start_ping_and_stop_stdio_client` YAML snapshot used by the stdio smoke test. The full Java E2E suite re-runs against all existing YAML snapshots under the InProcess transport.

Some tests need skip-guards for in-process-incompatible behavior (e.g., per-client environment variables are ignored when the runtime is shared in-process — see [issue #1934](https://github.com/github/copilot-sdk/issues/1934)). The Java equivalent of Rust's `skip_inprocess(reason)` function is a JUnit 5 `@DisabledIf` condition or a custom `@SkipInProcess` annotation.

**Answer 5: Should the entire E2E suite run under both transports?**

**YES**, mirroring the Rust PR's pattern exactly:

- **CI job A** (existing): subprocess transport (stdio/TCP) — existing `java-sdk-tests.yml` job, no changes.
- **CI job B** (new): InProcess transport — same test suite, new Maven profile (`-Pinprocess`):

```xml
<profile>
  <id>inprocess</id>
  <properties>
    <COPILOT_SDK_DEFAULT_CONNECTION>inprocess</COPILOT_SDK_DEFAULT_CONNECTION>
  </properties>
  <build>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-failsafe-plugin</artifactId>
        <configuration>
          <!-- Mandatory: env guard is process-global; concurrent tests would race -->
          <forkCount>1</forkCount>
          <parallel>none</parallel>
          <environmentVariables>
            <COPILOT_SDK_DEFAULT_CONNECTION>inprocess</COPILOT_SDK_DEFAULT_CONNECTION>
          </environmentVariables>
        </configuration>
      </plugin>
    </plugins>
  </build>
</profile>
```

CI job B requires `runtime.node` to be on the classpath (from the `copilot-native` module built by job A's prerequisite). The matrix runs both jobs, providing confidence that subprocess and InProcess transports produce identical behavior for all non-skip-guarded tests.

**Java-specific implementation requirements (for step 4.7 — E2E tests):**

1. **`InProcessEnvGuard`** in `com.github.copilot.ffi` (or `com.github.copilot.test.harness` for the test module): calls `SetEnvironmentVariableW` / `setenv()` via JNA. See `spike-3-11` for the proven implementation.

2. **`E2ETestContext.createClient()`** dispatch: when `connection instanceof InProcessRuntimeConnection` (or `COPILOT_SDK_DEFAULT_CONNECTION=inprocess`), apply `InProcessEnvGuard` before starting the client, and call `InProcessEnvGuard.close()` in the test's `@AfterEach` / try-with-resources.

3. **Concurrency guard**: enforce single-threaded test execution when running in-process. The `InProcessEnvGuard` must not be active for two tests simultaneously.

4. **`@SkipInProcess` annotation**: a JUnit 5 condition annotation that skips tests that set per-client environment variables or rely on behavior that the in-process transport cannot support (see issue #1934).

### 3.12 — CI/CD workflow changes

**Question:** What GitHub Actions workflow changes are needed to build and test the InProcess transport?

The .NET PR modifies `dotnet-sdk-tests.yml` to add 6 lines for InProcess test configuration. The Rust PR adds 87 lines to `rust-sdk-tests.yml` with Linux/macOS CI jobs.

For Java:

1. Does the existing `java-sdk-tests.yml` workflow need modification, or does a separate workflow handle InProcess tests?
2. How are the native binaries provisioned in CI? Downloaded from a release? Built from source?
3. Which CI runner platforms need InProcess test coverage? (historically discussed as linux-x64 and darwin-arm64 minimum)
4. Should InProcess tests be gated behind a `runtime.node` availability check to avoid failing when the binary isn't present?

**Recommendation:** Modify the existing `java-sdk-tests.yml` to add an InProcess test job on linux-x64 (`ubuntu-latest`) for now. InProcess tests run as a separate Maven profile. Additional runner platforms are deferred under the temporary linux-x64-only implementation invariant.

**Resolution:**

**Sub-question 1 — Does `java-sdk-tests.yml` need modification, or a separate workflow?**

Answered by 3.11 Resolution. Modify the existing `java-sdk-tests.yml` to add a new `java-sdk-inprocess` job — a separate job, not a matrix entry (that is the .NET pattern). NOT a separate workflow file. The existing `java-sdk` job is completely unchanged. The new job activates the `-Pinprocess` Maven profile. This mirrors the Rust pattern exactly: `test` and `test-inprocess` are separate jobs in the same `rust-sdk-tests.yml`.

**Sub-question 2 — How are the native binaries provisioned in CI?**

Answered by 3.2 Resolution. Via the `copilot-native` Maven module's `generate-resources` phase running `npm pack @github/copilot-<platform>@${project.version}` with SHA-512 integrity verification against `nodejs/package-lock.json`. NOT downloaded from GitHub Releases. NOT built from Rust source. Under the temporary linux-x64-only implementation invariant, this means `npm pack @github/copilot-linux-x64@${project.version}` only in this phase. The InProcess CI job must build (or have a prerequisite step that builds) the `copilot-native` module to produce the linux-x64 classifier JAR on the classpath before tests execute.

**Sub-question 3 — Which CI runner platforms?**

Answered by the temporary implementation invariant and 3.12 Recommendation. Current phase scope is `ubuntu-latest` (linux-x64) only. `macos-latest` (darwin-arm64), Windows, and all other OS/arch combinations are deferred for later phases.

**Sub-question 4 — Should InProcess tests be gated behind a `runtime.node` availability check?**

Answered by 3.11 and 3.5 Resolutions. No explicit availability check in code. The gating mechanism is the `-Pinprocess` Maven profile, activated only in CI job B. The CI job ensures the `copilot-native` module build step (which runs `npm pack`) has completed before tests execute. If `InProcessRuntimeConnection` is explicitly selected but no native binary is found on the classpath, the SDK throws `IllegalStateException` with a diagnostic message (from 3.5 Resolution). No runtime sentinel check or feature flag is needed.

### 3.13 — Classpath-first or path-first native resolution?

**Question:** In what order should the SDK look for the `runtime.node` binary?

Options for resolution order:

1. `COPILOT_CLI_PATH` environment variable → explicit path to the runtime binary
2. Classpath resource (`native/<classifier>/runtime.node`) → from classifier JAR
3. Bundled CLI location (existing `CliServerManager` path) → the current subprocess path, but load the `.so`/`.dylib`/`.dll` sibling

The .NET PR resolves the entrypoint from `COPILOT_CLI_PATH` and falls back to the bundled CLI location. The Rust PR discovers or extracts the platform library alongside the embedded CLI.

**Recommendation:** Resolution order: `COPILOT_CLI_PATH` (explicit) → classpath resource (classifier JAR) → alongside bundled CLI. This matches the .NET pattern and gives operators an override.

**Resolution:**

Resolution order: `COPILOT_CLI_PATH` (explicit) → classpath resource (classifier JAR) → alongside bundled CLI. This matches the .NET pattern and gives operators an override.

### 3.14 — `@CopilotExperimental` annotation on InProcess API

**Question:** Should the InProcess transport API be annotated with `@CopilotExperimental`?

The existing SDK marks experimental features with `@CopilotExperimental` (compile-time check via `CopilotExperimentalProcessor`). The .NET PR's InProcess transport appears to be non-experimental (it's opt-in via connection type). The Rust PR's `Transport::InProcess` is additive.

**Recommendation:** Yes, annotate with `@CopilotExperimental` initially. The InProcess transport depends on the Rust runtime's C ABI stability and the ongoing TypeScript migration. Remove the annotation when the C ABI and runtime are declared stable.

**Resolution:**

Annotate with `@CopilotExperimental` initially. The InProcess transport depends on the Rust runtime's C ABI stability and the ongoing TypeScript migration. Remove the annotation when the C ABI and runtime are declared stable.

---

### 3.15 ✅ Additional human generated questions while reviewing the first draft of this plan, committed in 292a9036aa

1. Is the set of C ABI entry points listed in the table at "C ABI entry points to bind" sufficient? I thought ypou said there were "12 `extern "C"` entry points? That table only has 5.

**Resolution:** Answered out of band. Changes made accordingly. No further action necessary.

2. Don't I need instructions for installing the rust toolchain in my dev environment? In order to do the bundling, won't I need to build the rust binaries? Or are they available in some artifact repository of some kind? I could add the Copilot CLI codebase to this VS Code workspace if that helps. This overlaps with question 3.2:

   > The .NET PR uses MSBuild targets to copy `runtime.node` from `runtimes/<rid>/native/`. The Rust PR uses a `build.rs` script that downloads/extracts from npm package tarballs.

   Where is this `runtimes` direcory? Is it committed to `git`? I doubt that. Is it in `~/.copilot`?

**Resolution:** Answered out of band. Changes made accordingly. No further action necessary.

4. I heard the engineers working on other Copilot SDK languages talk about their language bindings being able to communicate in-proc or out of proc. This leads me to think they have some kind of configurable switch. If the other languages do this, then Java should probably also do it. And if so, this impacts the answer to questions 3.4 and 3.5, no?

**Resolution:** Answered out of band. Changes made accordingly. No further action necessary.

5. For the Copilot SDK language bindings that have already made the transition to embedding the Copilot CLI runtime, did they completely abandon the old practice of allowing the use of the system-installed Copilot CLI runtime? Or is this configurable? I expect they abandoned it. This is related to questions 3.8, 3.13 and 3.14. I thought we didn't need a COPILOT_CLI_PATH any more with this approach. I thought that was the entire point of embedding the CLI.

**Resolution:** Answered by answer to previous question.

6. What, if any, is the TDD-style guidance given to the agents during the implementation phases? I don't see this in the plan. We need to make sure there is very good test coverage.

**Resolution:** Answered out of band. Changes made accordingly. No further action necessary.

## Phase 4 — Implementation (the build order)

After Phase 3 questions are resolved, implement in this order. Each step should be a separately testable commit.

> **Execution order is locked to Choice A (restructure-first).**
>
> Phase 4 uses a flattened sequence where the former 4.6a runs first, then implementation proceeds in the new numbering below. The former-step mapping is included in each heading.

> **DRI decision — hard scope invariant for all native implementation work in Phase 4.**
>
> Because this implementation includes native code and is split across Copilot Coding Agent and local Copilot CLI work, all Phase 4 native implementation work is limited to **Ubuntu 24.04 on linux-x64 only**.
>
> Any platform-specific implementation work for the following OS/arch pairs is **out of scope for this phase** and must not be done now:
>
> - `linux-arm64`
> - `linuxmusl-x64`
> - `linuxmusl-arm64`
> - `darwin-x64`
> - `darwin-arm64`
> - `win32-x64`
> - `win32-arm64`
>
> If any step below appears to imply implementation for those platforms, this invariant overrides that text. Those platforms are deferred to a later phase/plan.

### TDD discipline for all implementation steps

Every implementation step in this phase **must** follow this test-driven workflow:

1. **Write tests first.** Before writing or modifying production code for a step, write the unit tests (and integration tests where specified) that define the expected behavior. Tests should initially fail (red).

The test native library from `spike-3-4-jna-callback-and-threading/rust-dll/` is the test fixture for steps 4.4 and 4.5. Build it once with `cargo build --release` for the current OS and architecture and place the output at a known path before writing Java tests.

2. **Implement until green.** Write the minimum production code to make all tests pass.
3. **Refactor.** Clean up the implementation while keeping tests green. Run `mvn spotless:apply` to ensure formatting compliance.
4. **Gate before proceeding.** All tests from the current step **and all prior steps** must pass (`mvn verify`) before moving to the next step. Do not proceed with a step if any prior step's tests are broken.
5. **Coverage expectations per step:**

- Every public method must have at least one test exercising the success path and one test exercising the primary failure/edge-case path.
- Error handling paths (e.g., missing native binary, failed `host_start`, callback on closed connection) must have explicit tests — do not assume "it would throw."
- Platform-specific behavior in this phase is limited to Ubuntu `linux-x64` only. Do not add implementation-specific tests for other OS/arch pairs in this phase.
- Thread-safety-sensitive code (callback handling, stream bridging, shutdown draining) must have concurrency tests — e.g., multiple threads writing/reading simultaneously, shutdown during active callback.

6. **Test isolation.** Each step's tests must be runnable independently of whether a real `runtime.node` binary is present. Unit tests must use mocks, test doubles, or minimal test native libraries — never depend on the real runtime binary. Only E2E integration tests (step 4.8) require the real binary.
7. **No skipping tests.** Do not annotate tests with `@Disabled` or `@Ignore` to work around failures. If a test cannot pass, fix the production code or fix the test.

### 4.1 — Parent POM restructure

**What:** Convert the single-module `java/pom.xml` into a multi-module reactor. Move the existing SDK code into a `sdk/` subdirectory while preserving its GAV (`com.github:copilot-sdk-java`).

**Files to create:**

- `java/pom.xml` — new parent POM (`com.github:copilot-sdk-java-parent`, `packaging=pom`). Declares `<modules>` for `sdk`, `copilot-native`, and `copilot-native-all`. Centralizes shared properties, plugin versions, and `copilot.sdk.root` path.

**Files to move:**

- Existing `java/pom.xml` → `java/sdk/pom.xml` (with `<parent>` added pointing to `copilot-sdk-java-parent`; existing GAV `com.github:copilot-sdk-java` preserved)
- Existing `java/src/` → `java/sdk/src/`
- Existing `java/config/` → `java/sdk/config/` (or kept at `java/config/` and referenced via `${project.parent.basedir}/config/`)

**Files to update:**

- `justfile` — update `java/` paths to `java/sdk/` where needed
- `.github/workflows/java-sdk-tests.yml` — update working directory references
- `.github/workflows/` — any other workflows referencing `java/pom.xml`

**Gating criteria:** `mvn clean verify` from `java/` runs the full reactor. `mvn -pl sdk clean verify` builds and tests the SDK exactly as before. All existing tests pass. CI workflows work with the new directory structure.

### 4.2 — Platform detection utility

**What:** `PlatformDetector` class that determines `os`, `arch`, `libc` and produces the classifier string.

**Files to create:**

- `java/sdk/src/main/java/com/github/copilot/ffi/PlatformDetector.java`

**Tests:** Unit tests with mocked system properties, test ELF binary fragments for PT_INTERP parsing.

- `java/sdk/src/test/java/com/github/copilot/ffi/PlatformDetectorTest.java`

**Gating criteria:** Correct classifier output for Ubuntu `linux-x64` on `ubuntu-latest`. Multi-platform and musl-specific classifier gating is deferred to a later phase.

### 4.3 — Native binary extraction and caching

**What:** `NativeRuntimeLoader` class that locates `runtime.node` on the classpath, extracts to cache, and returns the filesystem path.

**Files to create:**

- `java/sdk/src/main/java/com/github/copilot/ffi/NativeRuntimeLoader.java`

**Tests:** Unit tests with classpath resources, temp directory extraction, atomic rename behavior.

- `java/sdk/src/test/java/com/github/copilot/ffi/NativeRuntimeLoaderTest.java`

**Gating criteria:**

- Extracts binary to `~/.copilot/runtime-cache/<version>/<classifier>/runtime.node`. Handles concurrent extraction safely.

- When _multiple_ platform JARs are on the classpath (uber-jar scenario), it sorts candidates and picks the best match. The plan's `NativeRuntimeLoader` should handle this case — in the `copilot-native-all` uber-JAR, all 8 `native/<classifier>/runtime.node` resources exist on the classpath simultaneously. The loader must filter by the detected classifier, not just grab the first `runtime.node` it finds. ❌❌❌We are not doing the uber-jar approach now, but we want to do it in the future, so we must be ready for it.❌❌❌

### 4.4 — JNA binding interface and implementation

**What:** `NativeBinding` interface, `JnaNativeBinding` implementation, JNA `Callback` for outbound data.

**Files to create:**

- `java/sdk/src/main/java/com/github/copilot/ffi/NativeBinding.java`
- `java/sdk/src/main/java/com/github/copilot/ffi/JnaNativeBinding.java`
- `java/sdk/src/main/java/com/github/copilot/ffi/OutboundCallback.java`

**Tests:** Unit tests using a test native library with minimal C ABI (or mock/spy on JNA calls).

- `java/sdk/src/test/java/com/github/copilot/ffi/JnaNativeBindingTest.java`

**Gating criteria:**

- Can load a native library, call functions, receive callbacks. Error cases throw `IllegalStateException` (see 3.10 resolution — no dedicated `FfiTransportException`).

- **Library-never-unloads pattern** — the loaded native handle must be held in a `static` field and never released. JNA caches by library name, but the plan should make this explicit since native worker threads outlive any `FfiRuntimeHost` instance. See Rust `OnceLock<Mutex<HashMap<PathBuf, &'static Library>>>` + `Box::leak()` Missing this risks a crash if a second `FfiRuntimeHost` is created after the first is closed.

### 4.5 — FFI runtime host and transport streams

**What:** `FfiRuntimeHost` class that manages the full lifecycle: `host_start` → `connection_open` → duplex stream bridging → `connection_close` → `host_shutdown`. Provides `InputStream`/`OutputStream` compatible with `JsonRpcClient`.

**Files to create:**

- `java/sdk/src/main/java/com/github/copilot/ffi/FfiRuntimeHost.java`

**Tests:**

- `java/sdk/src/test/java/com/github/copilot/ffi/FfiRuntimeHostTest.java`

**Gating criteria:**

- Full lifecycle works with a test native library. Callback data flows through `InputStream`. Write data reaches `connection_write`. Shutdown drains active callbacks.

- **Callback `closing` flag early-exit** — the `on_outbound` callback must check a `closing` flag and return immediately without enqueuing data. Without this, the shutdown drain may never converge. Both .NET and Rust set this flag before `connection_close`. Failing to do this can caus a hang on shutdown.

- **Operation lock for concurrent write/close safety** — `FfiOutputStream.write()` can race with `FfiRuntimeHost.close()`. See how the Rust SDK uses a `parking_lot::Mutex` (`operation_lock`). See the Rust SDK `FfiShared`. Failing to do this can cause a data race during shutdown.

- **`Connection` record needs `FfiRuntimeHost` field** — the current `CopilotClient.Connection` record has `(JsonRpcClient rpc, Process process, ServerRpc serverRpc)`. InProcess has no `Process`. Without an `ffiHost` field, `stop()` and `forceStop()` can't call `ffiHost.close()`. .NET's `Connection` record includes `FfiRuntimeHost? ffiHost`. Failure to do this can cause a leak of native resources on shutdown.

### 4.6 — Transport integration with `CopilotClient`

**What:** `RuntimeConnection` sealed class hierarchy (see 3.5.1 resolution), `setConnection()` on `CopilotClientOptions`, InProcess code path in `CopilotClient` that uses `FfiRuntimeHost` instead of `CliServerManager`. **Do NOT create a `Transport` enum or `setTransport()` method — that approach was explicitly rejected in the 3.5.1 resolution in favor of the `RuntimeConnection` type hierarchy.**

✅✅Remember to handle **`COPILOT_SDK_DEFAULT_CONNECTION` env var resolution in `CopilotClient` constructor**. `CopilotClient` must implement `resolveDefaultConnection()` when no `connection` is set. See .NET `dotnet/src/Client.cs` — search for `ResolveDefaultConnection` (private static method) and its caller `_options.Connection ?? ResolveDefaultConnection(_options)`; Rust `rust/src/lib.rs` — search for `fn resolve_default_transport` and constant `DEFAULT_CONNECTION_ENV_VAR`.

✅✅Remember: **`ValidateEnvironmentOptions` — reject incompatible options for InProcess** — `environment`, `telemetry`, `workingDirectory`, `extraArgs` must be rejected when InProcess is selected. Without this, users set options that silently do nothing in-process. See .NET `dotnet/src/Client.cs` — search for `ValidateEnvironmentOptions` (private static method, called right after `ResolveDefaultConnection`); Rust `rust/src/lib.rs` — search for `fn validate_inprocess_options`.

**Files to modify:**

- `java/sdk/src/main/java/com/github/copilot/rpc/CopilotClientOptions.java` — add `connection` field (type `RuntimeConnection`, nullable, default `null`)
- `java/sdk/src/main/java/com/github/copilot/CopilotClient.java` — InProcess connection path via `RuntimeConnection` dispatch

**Files to create:**

- `java/sdk/src/main/java/com/github/copilot/rpc/RuntimeConnection.java` — sealed class with factory methods (see 3.5.1 resolution)
- `java/sdk/src/main/java/com/github/copilot/rpc/StdioRuntimeConnection.java`
- `java/sdk/src/main/java/com/github/copilot/rpc/TcpRuntimeConnection.java`
- `java/sdk/src/main/java/com/github/copilot/rpc/UriRuntimeConnection.java`
- `java/sdk/src/main/java/com/github/copilot/rpc/InProcessRuntimeConnection.java`

**Tests:** Unit test that InProcess transport selection uses `FfiRuntimeHost`.

✅✅✅Test the backward-compatibility bridge (legacy fields → `RuntimeConnection` inference) and the `IllegalArgumentException` when both `connection` and legacy fields are set.✅✅✅

- `java/sdk/src/test/java/com/github/copilot/CopilotClientTransportTest.java`

**Gating criteria:** `new CopilotClientOptions().setConnection(RuntimeConnection.forInProcess())` routes through FFI host. `COPILOT_SDK_DEFAULT_CONNECTION=inprocess` env var works. CLI transport unchanged.

### 4.7 — Native binary download and classifier JAR module

**What:** New `copilot-native/` module (`com.github:copilot-sdk-java-runtime`) that, in this phase, downloads `runtime.node` for `linux-x64` only via `npm pack` and packages a `linux-x64` classifier JAR.

**Files to create:**

- `java/copilot-native/pom.xml` — module POM with:
  - `exec-maven-plugin` execution in `generate-resources` phase for `linux-x64` only: `npm pack @github/copilot-linux-x64@${project.version}`, followed by `tar` extraction to `target/native-staging/linux-x64/native/linux-x64/runtime.node`
  - A build step that reads `integrity` (SHA-512) from `${copilot.sdk.root}/nodejs/package-lock.json` and verifies the downloaded `.tgz`
  - Default `maven-jar-plugin` execution producing a placeholder primary JAR (contains only `native/lib/copilot-runtime.properties` with `placeholder=true`)
  - One additional `maven-jar-plugin` execution with `<classifier>linux-x64</classifier>`, packaging from `target/native-staging/linux-x64/`
  - Optional: keep `build-helper-maven-plugin` wiring prepared for future Gradle Module Metadata (`.module`) expansion
- `java/copilot-native/src/main/resources/native/lib/copilot-runtime.properties` — placeholder properties (`placeholder=true`, `version=${project.version}`)
- `java/copilot-native/gmm-template.json` — optional deferred artifact; if present in this phase, limit to `linux-x64` only

**Resource path convention per classifier JAR:**

```
native/<classifier>/runtime.node
native/<classifier>/platform.properties
```

Where `platform.properties` contains:

```properties
classifier=linux-x64
version=${project.version}
```

**Gating criteria:** `mvn package -pl copilot-native` produces the `linux-x64` classifier JAR with `native/linux-x64/runtime.node`. The placeholder primary JAR contains no native binaries. SHA-512 verification passes for the `linux-x64` tarball. Other classifiers are deferred.

> **Deferred optional work:** Monolithic uber-JAR assembly remains out of scope for this phase. Do not implement multi-platform uber-JAR assembly while the Phase 4 invariant is `linux-x64`-only.

### 4.8 — E2E integration test

**What:** Failsafe IT that exercises InProcess transport with a real `runtime.node` binary.

**Files to create:**

- `java/sdk/src/test/java/com/github/copilot/e2e/InProcessTransportIT.java`

**Snapshot files:** Reuse existing snapshots or create new ones as needed.

**Gating criteria:** Client connects, creates session, sends message, receives response — all via InProcess FFI transport. Runs in CI where `runtime.node` is available.

### 4.9 — CI workflow updates

**What:** Modify `java-sdk-tests.yml` to add InProcess test jobs.

**Files to modify:**

- `.github/workflows/java-sdk-tests.yml`

**Gating criteria:** CI runs InProcess E2E tests on Ubuntu `linux-x64` only. No implementation-specific CI work for other OS/arch pairs is included in this phase.

---

## Phase 5 — Documentation

- Update `java/README.md` with InProcess transport usage example.
- Update ADR-007 status from DRAFT to ACCEPTED.
- Document `COPILOT_SDK_DEFAULT_CONNECTION` env var.
- Add troubleshooting section for native library loading issues.

---

## Cross-cutting concerns

| Concern                   | Notes                                                                                                                                                           |
| ------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Java 17 baseline**      | JNA works on Java 17. No Panama FFM. No `--enable-native-access` needed.                                                                                        |
| **GraalVM native-image**  | Out of scope for this feature in this plan iteration; do not pursue native-image support for the JNA-backed InProcess transport.                                |
| **Windows path handling** | Deferred. Do not do Windows-specific implementation work in this phase; current scope is Ubuntu linux-x64 only.                                                 |
| **Thread safety**         | `FfiRuntimeHost` must be thread-safe. Callback invocations come from native threads.                                                                            |
| **Memory management**     | JNA `Callback` instances must not be GC'd while native holds the function pointer. `Pointer`/`Memory` objects must be freed correctly.                          |
| **Graceful degradation**  | If `runtime.node` is not on the classpath and no CLI path is configured, the SDK should produce a clear error message, not a `ClassNotFoundException` from JNA. |
| **Spotless/Checkstyle**   | All new code must pass `mvn spotless:check` and Checkstyle. Javadoc required on public APIs.                                                                    |
