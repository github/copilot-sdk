# Implementation plan: Embed Rust-based Copilot CLI runtime in the Java SDK (issue #1917)

Human DRI: Ed Burns
ADR: `java/docs/adr/adr-007-native-bundling-strategy.md`
Epic: https://github.com/github/copilot-sdk/issues/1917
Reference PRs:

- https://github.com/github/copilot-sdk/pull/1901 — .NET in-process FFI transport (`FfiRuntimeHost.cs`)
- https://github.com/github/copilot-sdk/pull/1915 — Rust SDK in-process FFI transport (`ffi.rs`)

Working directory: `copilot-sdk/1917-java-embed-rust-cli-runtime-remove-before-merge/`

---

## Goal

Embed the Copilot runtime (`runtime.node` cdylib) directly into the Java SDK so that consumers no longer need an externally installed Copilot CLI. The SDK will:

1. Ship per-platform classifier JARs containing the `runtime.node` binary for each of the 8 platform targets (Option 2).
2. Support uber-jar assembly via `maven-assembly-plugin` that merges all (or a subset of) platform JARs into a single distributable artifact (Option 1 compatibility).
3. Detect the current platform at runtime, extract the matching native binary, and load it via JNA to call the 5 `extern "C"` entry points of the runtime's C ABI front door.
4. Bridge bidirectional JSON-RPC transport over the FFI boundary (Java → native downcalls, native → Java upcall callbacks).

### C ABI entry points to bind (from .NET PR #1901 and Rust PR #1915)

| Entry point                        | Signature (C)                                                                                                                                                                                                                                                              | Purpose                                                                                                                                                                                                          |
| ---------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `copilot_runtime_host_start`       | `(const uint8_t* argv_json, size_t argv_json_len, const uint8_t* env_json, size_t env_json_len) → uint32_t`                                                                                                                                                                | Start the runtime host; `argv_json` is a JSON array (e.g., `["copilot","--embedded-host"]`), `env_json` is an optional JSON object of environment overrides. Returns server handle (0 = failure).                |
| `copilot_runtime_host_shutdown`    | `(uint32_t server_id) → bool`                                                                                                                                                                                                                                              | Shut down the runtime host identified by `server_id`.                                                                                                                                                            |
| `copilot_runtime_connection_open`  | `(uint32_t server_id, void(*on_outbound)(void* user_data, const uint8_t* data, size_t len), void* user_data, const uint8_t* ext_source, size_t ext_source_len, const uint8_t* ext_name, size_t ext_name_len, const uint8_t* conn_token, size_t conn_token_len) → uint32_t` | Open a bidirectional connection; registers `on_outbound` callback for runtime→Java data delivery. `ext_source`, `ext_name`, `conn_token` are nullable metadata buffers. Returns connection handle (0 = failure). |
| `copilot_runtime_connection_write` | `(uint32_t connection_id, const uint8_t* data, size_t len) → bool`                                                                                                                                                                                                         | Write a JSON-RPC frame from Java into the runtime. Native side copies the buffer synchronously before returning.                                                                                                 |
| `copilot_runtime_connection_close` | `(uint32_t connection_id) → bool`                                                                                                                                                                                                                                          | Close a connection.                                                                                                                                                                                              |

The outbound callback signature: `void on_outbound(void* user_data, const uint8_t* data, size_t len)` — invoked by native code (potentially on native threads) to deliver JSON-RPC responses and notifications back to Java.

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

### 3.1 — Maven module structure for per-platform classifier JARs

**Question:** How should the Maven project be structured to produce the coordination artifact plus 8 classifier JARs?

ADR-007 specifies publishing `copilot-sdk-java-runtime:VERSION:<classifier>` artifacts alongside the existing `copilot-sdk-java` coordination artifact. Options:

| Option | Structure                                                                                                                                  | Trade-off                                                                                                                                                                                                                  |
| ------ | ------------------------------------------------------------------------------------------------------------------------------------------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| A      | Single `pom.xml` with Maven Assembly Plugin producing classifier JARs as attached artifacts                                                | Simpler build, but classifier JARs are secondary artifacts of the main module. Maven Central treats them as the same artifact — consumers declare `<classifier>linux-x64</classifier>` on the same `copilot-sdk-java` GAV. |
| B      | Multi-module reactor: parent `pom.xml` → `copilot-sdk-java` (existing) + `copilot-sdk-java-runtime` (new module producing classifier JARs) | Cleaner separation, DJL-style. The runtime module has its own GAV. But adds build complexity and the monorepo's `java/` directory currently has a single `pom.xml`.                                                        |
| C      | Single module, classifiers produced by a custom Maven plugin or build-helper-maven-plugin to attach additional artifacts                   | Middle ground. The classifier JARs are attached artifacts of a new `copilot-sdk-java-runtime` artifact built by its own `pom.xml` adjacent to the main SDK pom.                                                            |

**Spike needed:** Look at how DJL's `pytorch-native` module produces classifier JARs. Verify whether `maven-assembly-plugin` or `build-helper-maven-plugin` is the right tool for attaching pre-built native binaries as classifier artifacts.

**Recommendation:** Option B — a new `copilot-sdk-java-runtime` module with its own `pom.xml` that produces 8 classifier JARs. The main `copilot-sdk-java` artifact declares an optional dependency on the runtime module. This matches the DJL pattern and keeps the existing build untouched.

**Resolution:**

### 3.2 — How do native binaries enter the build?

**Question:** Where do the `runtime.node` binaries come from during the Maven build, and how are they placed into the classifier JARs?

The .NET PR uses MSBuild targets to copy `runtime.node` from `runtimes/<rid>/native/`. The Rust PR uses a `build.rs` script that downloads/extracts from npm package tarballs. For Java, options:

| Option | Mechanism                                                                                                        | Trade-off                                                                                                                            |
| ------ | ---------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------ |
| A      | Maven downloads pre-built tarballs from GitHub Releases during `generate-resources` phase                        | Requires network access at build time; must handle version pinning and integrity verification.                                       |
| B      | A CI workflow pre-stages the binaries into a known directory before `mvn` runs; Maven just copies them into JARs | Simpler POM; CI does the heavy lifting. Matches how the publish pipeline already works.                                              |
| C      | npm-based download (similar to the Rust SDK's approach) via `exec-maven-plugin` calling a Node.js script         | Leverages existing `test/harness` Node.js infrastructure in the monorepo. But adds a Node.js build dependency for the main artifact. |

**Spike needed:** Examine the `copilot-agent-runtime` publish pipeline (`publish-cli.yml`) to understand what artifacts are produced and how other SDKs consume them.

**Recommendation:** Option B for CI/publishing (the workflow stages binaries, Maven packages them). For local development, provide a script that fetches the binaries, but the main `mvn clean verify` should work without native binaries present (InProcess transport is optional).

**Resolution:**

### 3.3 — JNA binding interface design

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

### 3.4 — JNA callback threading and lifecycle

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

### 3.5 — Transport integration with `CopilotClient`

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

**Resolution:**

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

### 3.7 — Native binary extraction and caching

**Question:** What is the exact extraction and caching strategy for the `runtime.node` binary?

ADR-007 proposes extracting from classpath to `~/.copilot/runtime-cache/<version>/<classifier>/runtime.node`. Open questions:

1. **Version source:** Where does the version come from? `getClass().getPackage().getImplementationVersion()` relies on the JAR manifest. Is this set by the build? What about running from an IDE (un-jarred classes)?
2. **Atomicity:** If two JVM processes start simultaneously and both try to extract, how do we prevent corruption? Options: temp file + atomic rename, file locking, check-then-extract with size/checksum verification.
3. **Cache invalidation:** Should we verify integrity (e.g., file size or hash) on each startup, or trust the version-keyed path?
4. **Permissions:** On Unix, the extracted binary needs `chmod +x`. The ADR's `cached.toFile().setExecutable(true)` works — but note `runtime.node` is a shared library, not an executable. Shared libraries loaded via `dlopen` (which JNA uses internally) do **not** need execute permission on most Linux systems. Verify.
5. **Cleanup:** Should old versions in the cache be cleaned up? The .NET and Rust SDKs don't do this.

**Recommendation:** Use temp file + atomic rename for extraction. Trust the version-keyed path (no integrity check on subsequent loads). Don't clean up old versions. Set executable permission as a no-op safety measure. Use `<sdk-version>` from `pom.xml` injected into a `.properties` file in the JAR for version identification.

**Resolution:**

### 3.8 — JNA dependency management

**Question:** How should JNA be added as a dependency, and what version constraints apply?

The Java SDK currently has no JNA dependency. Adding it introduces:

1. **Version selection:** JNA 5.x is current. The latest is 5.16.0 (as of 2025). It supports Java 8+. The SDK targets Java 17.
2. **Transitive impact:** JNA brings `jna-platform` optionally. We likely only need `jna` (core), not `jna-platform`.
3. **Scope:** Should JNA be a required dependency or optional? If the SDK works without native binaries (subprocess transport), JNA is only needed for InProcess transport. Making it `<optional>true</optional>` means consumers using only CLI transport don't pull it in.
4. **GraalVM native-image:** JNA has established `native-image.properties` in its JAR. Verify this works for the callback pattern we need.

**Recommendation:** Add JNA as an `<optional>true</optional>` dependency. Only required when using InProcess transport. Use `jna` (not `jna-platform`). Version 5.16.0 or later.

**Resolution:**

### 3.9 — `runtime.node` entrypoint argument format

**Question:** What arguments does `copilot_runtime_host_start` expect, and how are they determined?

The .NET PR passes an `entrypoint` path and `args`. The Rust PR similarly passes entrypoint and args as byte buffers. We need to understand:

1. What is the `entrypoint` parameter? Is it the path to the `runtime.node` binary itself, or a path to a Node.js entry script?
2. What are the `args`? JSON-formatted startup options? CLI-style flags?
3. Does the host need the `runtime.node` file path passed as entrypoint, or does it use the loaded library's own location?
4. How does authentication context (GitHub token, proxy URLs for E2E) flow into the in-process host?

**Spike needed:** Read the `copilot_runtime_host_start` implementation in `github/copilot-agent-runtime` `src/runtime/src/interop/cabi.rs` to understand the expected arguments. Alternatively, study how the .NET and Rust SDKs construct the entrypoint and args.

**Resolution:**

### 3.10 — Error handling and diagnostics

**Question:** How should FFI-level errors be surfaced to the Java SDK user?

The C ABI functions return `uint32_t` handles or `bool` success flags. When they fail:

1. Is there an error message channel? (e.g., a `copilot_runtime_last_error` function, or is error info logged to stderr?)
2. Should FFI failures be wrapped in a new exception type (e.g., `FfiTransportException`) or use existing SDK exception types?
3. How should the SDK handle a native crash/abort (e.g., Rust panic that unwinds through FFI)? JNA's protected mode can catch `SIGSEGV` on some platforms, but this is best-effort.
4. How should the SDK log FFI-level diagnostics (library loading, callback events)?

**Recommendation:** Wrap FFI failures in a new `FfiTransportException extends RuntimeException`. Use `java.util.logging` consistent with the rest of the SDK. Document that a native abort (Rust panic) terminates the JVM — this is the cost of in-process hosting, mitigated by the fact that the runtime is extensively tested.

**Resolution:**

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

### 3.12 — CI/CD workflow changes

**Question:** What GitHub Actions workflow changes are needed to build and test the InProcess transport?

The .NET PR modifies `dotnet-sdk-tests.yml` to add 6 lines for InProcess test configuration. The Rust PR adds 87 lines to `rust-sdk-tests.yml` with Linux/macOS CI jobs.

For Java:

1. Does the existing `java-sdk-tests.yml` workflow need modification, or does a separate workflow handle InProcess tests?
2. How are the native binaries provisioned in CI? Downloaded from a release? Built from source?
3. Which CI runner platforms need InProcess test coverage? (linux-x64 and darwin-arm64 minimum?)
4. Should InProcess tests be gated behind a `runtime.node` availability check to avoid failing when the binary isn't present?

**Recommendation:** Modify the existing `java-sdk-tests.yml` to add InProcess test jobs on linux-x64 and darwin-arm64 runners. Native binaries are downloaded from the `copilot-agent-runtime` release artifacts. InProcess tests run as a separate Maven profile.

**Resolution:**

### 3.13 — Classpath-first or path-first native resolution?

**Question:** In what order should the SDK look for the `runtime.node` binary?

Options for resolution order:

1. `COPILOT_CLI_PATH` environment variable → explicit path to the runtime binary
2. Classpath resource (`native/<classifier>/runtime.node`) → from classifier JAR
3. Bundled CLI location (existing `CliServerManager` path) → the current subprocess path, but load the `.so`/`.dylib`/`.dll` sibling

The .NET PR resolves the entrypoint from `COPILOT_CLI_PATH` and falls back to the bundled CLI location. The Rust PR discovers or extracts the platform library alongside the embedded CLI.

**Recommendation:** Resolution order: `COPILOT_CLI_PATH` (explicit) → classpath resource (classifier JAR) → alongside bundled CLI. This matches the .NET pattern and gives operators an override.

**Resolution:**

### 3.14 — `@CopilotExperimental` annotation on InProcess API

**Question:** Should the InProcess transport API be annotated with `@CopilotExperimental`?

The existing SDK marks experimental features with `@CopilotExperimental` (compile-time check via `CopilotExperimentalProcessor`). The .NET PR's InProcess transport appears to be non-experimental (it's opt-in via connection type). The Rust PR's `Transport::InProcess` is additive.

**Recommendation:** Yes, annotate with `@CopilotExperimental` initially. The InProcess transport depends on the Rust runtime's C ABI stability and the ongoing TypeScript migration. Remove the annotation when the C ABI and runtime are declared stable.

**Resolution:**

---


### 3.15 Additional human generated questions while reviewing the first draft of this plan, committed in 292a9036aa

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

### TDD discipline for all implementation steps

Every implementation step in this phase **must** follow this test-driven workflow:

1. **Write tests first.** Before writing or modifying production code for a step, write the unit tests (and integration tests where specified) that define the expected behavior. Tests should initially fail (red).
2. **Implement until green.** Write the minimum production code to make all tests pass.
3. **Refactor.** Clean up the implementation while keeping tests green. Run `mvn spotless:apply` to ensure formatting compliance.
4. **Gate before proceeding.** All tests from the current step **and all prior steps** must pass (`mvn verify`) before moving to the next step. Do not proceed with a step if any prior step's tests are broken.
5. **Coverage expectations per step:**
   - Every public method must have at least one test exercising the success path and one test exercising the primary failure/edge-case path.
   - Error handling paths (e.g., missing native binary, failed `host_start`, callback on closed connection) must have explicit tests — do not assume "it would throw."
   - Platform-specific behavior (OS/arch detection, library naming) must be tested with parameterized tests covering all 8 platform combinations where feasible, using mocked system properties.
   - Thread-safety-sensitive code (callback handling, stream bridging, shutdown draining) must have concurrency tests — e.g., multiple threads writing/reading simultaneously, shutdown during active callback.
6. **Test isolation.** Each step's tests must be runnable independently of whether a real `runtime.node` binary is present. Unit tests must use mocks, test doubles, or minimal test native libraries — never depend on the real runtime binary. Only E2E integration tests (step 4.7) require the real binary.
7. **No skipping tests.** Do not annotate tests with `@Disabled` or `@Ignore` to work around failures. If a test cannot pass, fix the production code or fix the test.

### 4.1 — Platform detection utility

**What:** `PlatformDetector` class that determines `os`, `arch`, `libc` and produces the classifier string.

**Files to create:**

- `java/src/main/java/com/github/copilot/ffi/PlatformDetector.java`

**Tests:** Unit tests with mocked system properties, test ELF binary fragments for PT_INTERP parsing.

- `java/src/test/java/com/github/copilot/ffi/PlatformDetectorTest.java`

**Gating criteria:** Correct classifier output for all 8 platform combinations. Musl detection works against a test ELF binary.

### 4.2 — Native binary extraction and caching

**What:** `NativeRuntimeLoader` class that locates `runtime.node` on the classpath, extracts to cache, and returns the filesystem path.

**Files to create:**

- `java/src/main/java/com/github/copilot/ffi/NativeRuntimeLoader.java`

**Tests:** Unit tests with classpath resources, temp directory extraction, atomic rename behavior.

- `java/src/test/java/com/github/copilot/ffi/NativeRuntimeLoaderTest.java`

**Gating criteria:** Extracts binary to `~/.copilot/runtime-cache/<version>/<classifier>/runtime.node`. Handles concurrent extraction safely.

### 4.3 — JNA binding interface and implementation

**What:** `NativeBinding` interface, `JnaNativeBinding` implementation, JNA `Callback` for outbound data.

**Files to create:**

- `java/src/main/java/com/github/copilot/ffi/NativeBinding.java`
- `java/src/main/java/com/github/copilot/ffi/JnaNativeBinding.java`
- `java/src/main/java/com/github/copilot/ffi/OutboundCallback.java`
- `java/src/main/java/com/github/copilot/ffi/FfiTransportException.java`

**Tests:** Unit tests using a test native library with minimal C ABI (or mock/spy on JNA calls).

- `java/src/test/java/com/github/copilot/ffi/JnaNativeBindingTest.java`

**Gating criteria:** Can load a native library, call functions, receive callbacks. Error cases wrapped in `FfiTransportException`.

### 4.4 — FFI runtime host and transport streams

**What:** `FfiRuntimeHost` class that manages the full lifecycle: `host_start` → `connection_open` → duplex stream bridging → `connection_close` → `host_shutdown`. Provides `InputStream`/`OutputStream` compatible with `JsonRpcClient`.

**Files to create:**

- `java/src/main/java/com/github/copilot/ffi/FfiRuntimeHost.java`

**Tests:**

- `java/src/test/java/com/github/copilot/ffi/FfiRuntimeHostTest.java`

**Gating criteria:** Full lifecycle works with a test native library. Callback data flows through `InputStream`. Write data reaches `connection_write`. Shutdown drains active callbacks.

### 4.5 — Transport integration with `CopilotClient`

**What:** `Transport` enum, `setTransport()` on `CopilotClientOptions`, InProcess code path in `CopilotClient` that uses `FfiRuntimeHost` instead of `CliServerManager`.

**Files to modify:**

- `java/src/main/java/com/github/copilot/rpc/CopilotClientOptions.java` — add `transport` field
- `java/src/main/java/com/github/copilot/CopilotClient.java` — InProcess connection path

**Files to create:**

- `java/src/main/java/com/github/copilot/ffi/Transport.java`

**Tests:** Unit test that InProcess transport selection uses `FfiRuntimeHost`.

- `java/src/test/java/com/github/copilot/CopilotClientTransportTest.java`

**Gating criteria:** `new CopilotClientOptions().setTransport(Transport.IN_PROCESS)` routes through FFI host. `COPILOT_SDK_DEFAULT_CONNECTION=inprocess` env var works. CLI transport unchanged.

### 4.6 — Maven module for per-platform classifier JARs

**What:** New `copilot-sdk-java-runtime` Maven module that packages `runtime.node` binaries into classifier JARs.

**Files to create:**

- `java/copilot-sdk-java-runtime/pom.xml`
- Assembly descriptors for classifier JAR packaging
- `native/<classifier>/platform.properties` metadata files

**Gating criteria:** `mvn package` produces 8 classifier JARs with correct resource paths (`native/<classifier>/runtime.node`).

### 4.7 — E2E integration test

**What:** Failsafe IT that exercises InProcess transport with a real `runtime.node` binary.

**Files to create:**

- `java/src/test/java/com/github/copilot/e2e/InProcessTransportIT.java`

**Snapshot files:** Reuse existing snapshots or create new ones as needed.

**Gating criteria:** Client connects, creates session, sends message, receives response — all via InProcess FFI transport. Runs in CI where `runtime.node` is available.

### 4.8 — CI workflow updates

**What:** Modify `java-sdk-tests.yml` to add InProcess test jobs.

**Files to modify:**

- `.github/workflows/java-sdk-tests.yml`

**Gating criteria:** CI runs InProcess E2E tests on linux-x64 and darwin-arm64. Tests are skipped gracefully when `runtime.node` is not available.

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
| **GraalVM native-image**  | Verify JNA callback pattern works under native-image. Add reachability metadata if needed.                                                                      |
| **Windows path handling** | `runtime.node` on Windows is `copilot_runtime.dll`. Path separators, temp directory behavior differ.                                                            |
| **Thread safety**         | `FfiRuntimeHost` must be thread-safe. Callback invocations come from native threads.                                                                            |
| **Memory management**     | JNA `Callback` instances must not be GC'd while native holds the function pointer. `Pointer`/`Memory` objects must be freed correctly.                          |
| **Graceful degradation**  | If `runtime.node` is not on the classpath and no CLI path is configured, the SDK should produce a clear error message, not a `ClassNotFoundException` from JNA. |
| **Spotless/Checkstyle**   | All new code must pass `mvn spotless:check` and Checkstyle. Javadoc required on public APIs.                                                                    |
