# ADR-007: Native runtime bundling strategy: per-platform classifier JARs

## Context and problem statement

The Copilot SDK for Java supports an experimental in-process connection that loads the Copilot agent runtime as a native shared library. The existing stdio, TCP, and URI connections remain the default behavior unless the user explicitly selects the in-process connection.

### The runtime artifact

The artifact to be embedded is `runtime.node`, a Rust [`cdylib`](#references) produced by the `src/runtime` crate in `github/copilot-agent-runtime` using the [napi-rs](#references) build toolchain. Despite the `.node` file extension (a naming convention of napi-rs), this is an ordinary platform-specific shared library (`.so` on Linux, `.dylib` on macOS, `.dll` on Windows). It exposes two front doors built over the same internal engine:

* **[napi](#references) front door**: loaded by a Node.js process as a native addon for the current CLI path.
* **[C ABI](#references) front door**: a fixed set of 5 `extern "C"` lifecycle and transport entry points that any language can call in-process via [FFI](#references) ([JNA](#references) for Java, Python/cffi, C#/`DllImport`, Go/purego). All API methods travel as JSON-RPC data through this fixed transport; the export list does not change as the method set grows.

  | Entry point                        | C signature                                                                                                                                                                                                                                                                | Purpose                                                                                                                                                                                                                                 |
  | ---------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
  | `copilot_runtime_host_start`       | `(const uint8_t* argv_json, size_t argv_json_len, const uint8_t* env_json, size_t env_json_len) → uint32_t`                                                                                                                                                                | Start the runtime host; `argv_json` is a JSON array (e.g., `["copilot","--embedded-host"]`), `env_json` is an optional JSON object of environment overrides. Returns a server handle (0 = failure).                                     |
  | `copilot_runtime_host_shutdown`    | `(uint32_t server_id) → bool`                                                                                                                                                                                                                                              | Shut down the runtime host identified by `server_id`.                                                                                                                                                                                   |
  | `copilot_runtime_connection_open`  | `(uint32_t server_id, void(*on_outbound)(void* user_data, const uint8_t* data, size_t len), void* user_data, const uint8_t* ext_source, size_t ext_source_len, const uint8_t* ext_name, size_t ext_name_len, const uint8_t* conn_token, size_t conn_token_len) → uint32_t` | Open a bidirectional connection on the server; registers the `on_outbound` callback for runtime→SDK data delivery. `ext_source`, `ext_name`, and `conn_token` are nullable metadata buffers. Returns a connection handle (0 = failure). |
  | `copilot_runtime_connection_write` | `(uint32_t connection_id, const uint8_t* data, size_t len) → bool`                                                                                                                                                                                                         | Write a JSON-RPC frame from the SDK into the runtime. The native side copies the buffer synchronously before returning.                                                                                                                 |
  | `copilot_runtime_connection_close` | `(uint32_t connection_id) → bool`                                                                                                                                                                                                                                          | Close a connection.                                                                                                                                                                                                                     |

  The outbound callback signature: `void on_outbound(void* user_data, const uint8_t* data, size_t len)` — invoked by native code (potentially on native threads) to deliver JSON-RPC responses and notifications back to the SDK.

The `cli-native.node` addon — a separate, smaller artifact that provides ICU4X text segmentation, Win32 API wrappers, and terminal UI helpers — is a CLI-only artifact used by the Ink/React terminal interface. It is **not needed** by the Java SDK.

### Note on the active Rust migration

As of 2026-08, the `runtime.node` binary is being built up iteratively as TypeScript runtime code is ported into it. It is **not** being reduced; it is growing with each port PR. The `embedded_host.rs` module currently starts a child `copilot --embedded-host` process to service method bodies not yet ported to Rust.

The classifier JAR therefore contains a version-matched pair during the migration:

* `runtime.node`: loaded into the Java process through JNA
* `copilot` or `copilot.exe`: started internally by `copilot_runtime_host_start`
* `platform.properties`: classifier and runtime version metadata

The Java SDK does not independently spawn this child for JSON-RPC transport. The native runtime owns the transitional embedded-host process. The bundled CLI requirement disappears after the Rust migration is complete, while the C ABI and Java loading mechanism remain stable.

### Platform dimensions

The runtime must be built for each unique combination of OS, CPU architecture, and (on Linux) C runtime variant. The build system in `github/copilot-agent-runtime` produces eight Rust target triples:

| Platform label    | Rust triple                  | Constraint                                                       |
| ----------------- | ---------------------------- | ---------------------------------------------------------------- |
| `linux-x64`       | `x86_64-unknown-linux-gnu`   | [glibc](#references) ≥ 2.28 (Debian 10+, Ubuntu 20.04+, RHEL 8+) |
| `linux-arm64`     | `aarch64-unknown-linux-gnu`  | glibc ≥ 2.28                                                     |
| `linuxmusl-x64`   | `x86_64-unknown-linux-musl`  | dynamically links [musl libc](#references) (Alpine Linux)        |
| `linuxmusl-arm64` | `aarch64-unknown-linux-musl` | dynamically links musl libc                                      |
| `darwin-x64`      | `x86_64-apple-darwin`        | macOS, Intel                                                     |
| `darwin-arm64`    | `aarch64-apple-darwin`       | macOS, Apple Silicon                                             |
| `win32-x64`       | `x86_64-pc-windows-msvc`     | [MSVC CRT](#references) statically linked (`+crt-static`)        |
| `win32-arm64`     | `aarch64-pc-windows-msvc`    | MSVC CRT statically linked (`+crt-static`)                       |

The GNU/Linux glibc minimum of 2.28 is enforced at build time via a Microsoft/vscode-linux-build-agent sysroot and verified post-build by `script/linux/verify-glibc-requirements.sh`. The musl binaries are **not** fully statically linked; they dynamically link musl libc (`-C target-feature=-crt-static` is explicitly set at build time).

The **common case** (Windows × 2 + macOS × 2 + GNU/Linux × 2) requires **6 binaries**. Supporting Alpine Linux adds 2 more musl binaries for a total of **8**.

### Platform selection

The loader selects a classifier at runtime using standard Java and OS APIs:

1. **OS**: `System.getProperty("os.name")` distinguishes Windows, macOS, and Linux.
1. **Architecture**: `System.getProperty("os.arch")` maps `"amd64"`, `"x86_64"`, and `"x64"` to `x64`, and maps `"aarch64"` and `"arm64"` to `arm64`.
1. **Linux libc variant**: The loader reads the first 2 KB of `/proc/self/exe` and parses the [ELF](#references) PT_INTERP segment. An interpreter containing `/ld-musl-` selects musl, while `/ld-linux-` selects glibc.

If the Linux executable cannot be read or its interpreter is not recognized, the implementation falls back to the GNU/Linux classifier for the detected architecture. Unsupported operating systems and architectures fail with `IllegalStateException`.

### Size baseline

Measured from `github/copilot-agent-runtime` release `cli-1.0.69-2` (2026-07-06):

| Platform          | `runtime.node` (uncompressed) | Compressed (~40% deflate) |
| ----------------- | ----------------------------- | ------------------------- |
| `linux-x64`       | 64.7 MB                       | ~25.9 MB                  |
| `linux-arm64`     | 55.5 MB                       | ~22.2 MB                  |
| `linuxmusl-x64`   | 64.4 MB                       | ~25.8 MB                  |
| `linuxmusl-arm64` | 55.3 MB                       | ~22.1 MB                  |
| `darwin-x64`      | 57.3 MB                       | ~22.9 MB                  |
| `darwin-arm64`    | 48.1 MB                       | ~19.2 MB                  |
| `win32-x64`       | 55.9 MB                       | ~22.4 MB                  |
| `win32-arm64`     | 48.4 MB                       | ~19.4 MB                  |

The published Java SDK JAR (`copilot-sdk-java-1.0.6-preview.1.jar`) is currently **1.53 MB**. A future runtime-only monolithic JAR containing all 6 common-case native binaries would be approximately **132 MB** compressed; all 8 including musl would be approximately **180 MB** compressed.

These runtime-only estimates do not describe the current migration artifact. The current development `linux-x64` classifier JAR also contains the version-matched CLI executable and is approximately **152 MB compressed**. Its staged contents are approximately **133 MB** for `runtime.node` and **170 MB** for `copilot` before JAR compression.

All native dependencies within the runtime (`rustls`/`aws-lc-rs` for TLS, `rusqlite` with `bundled` feature for SQLite, `zlib-rs` for compression) are statically compiled into the binary. There are no dependencies on system OpenSSL, libgit2, or libz.

## Considered options

### Option 1: Monolithic JAR with all platform binaries

All 6 (or 8) platform artifact sets are bundled inside a single monolithic artifact. At runtime the SDK extracts and loads the one matching the current platform; the remaining 5–7 are carried silently.

**Advantages:**

- Single `<dependency>` in `pom.xml`; zero extra configuration for users.
- Familiar pattern: [ONNX Runtime](#references) (`onnxruntime-1.21.0.jar`, **130 MB**, all platforms) demonstrates this is an accepted norm in the Java ML ecosystem.

**Drawbacks:**

- Every user downloads every platform regardless of their target. A developer on Apple Silicon downloads 105+ MB of Linux and Windows binaries they will never use.
- Build tooling (thin Docker layers, incremental CI caches, artifact registries) penalises large JARs. A single 132–180 MB JAR invalidates the entire cache whenever any platform's binary changes.
- Maven's dependency resolution has no mechanism to supply platform-appropriate variants automatically; platform selection must happen entirely at runtime inside the JAR.
- Conflicts with the principle that Maven artifacts should be reproducible and minimal.

### Option 2: Per-platform classifier JARs

A small, pure-Java coordination artifact (`copilot-sdk-java`, ~1.5 MB) is published alongside separate per-platform native artifacts differentiated by Maven classifier:

```
com.github:copilot-sdk-java-runtime:VERSION:linux-x64
com.github:copilot-sdk-java-runtime:VERSION:linux-arm64
com.github:copilot-sdk-java-runtime:VERSION:linuxmusl-x64
com.github:copilot-sdk-java-runtime:VERSION:linuxmusl-arm64
com.github:copilot-sdk-java-runtime:VERSION:darwin-x64
com.github:copilot-sdk-java-runtime:VERSION:darwin-arm64
com.github:copilot-sdk-java-runtime:VERSION:win32-x64
com.github:copilot-sdk-java-runtime:VERSION:win32-arm64
```

Each classifier JAR contains `runtime.node`, `platform.properties`, and, during the active Rust migration, the version-matched `copilot` or `copilot.exe` embedded-host executable. The coordination artifact selects and loads the matching native when the user selects the in-process connection.

This is the same pattern used by DJL's PyTorch native artifacts (`pytorch-native-cpu-2.5.1-linux-x86_64.jar`, `pytorch-native-cpu-2.5.1-osx-aarch64.jar`, etc.), Netty's `netty-tcnative-boringssl-static` per-platform JARs, and others.

Build tools can be configured to resolve the correct classifier automatically:

- **Maven**: `<classifier>${os.detected.classifier}</classifier>` via [os-maven-plugin](#references).
- **Gradle**: variant-aware dependency resolution with attribute matching.
- **Uber-jar builds**: include all classifiers; the coordination artifact picks the right one at runtime.

**Advantages:**

* The long-term runtime-only download is the coordination artifact plus one platform JAR instead of every platform binary.
- Each platform JAR changes independently; CI caches and Docker layers for unchanged platforms are preserved across releases.
- Users building for a single known platform (most production deployments) pay exactly the cost of that platform.
- Follows well-established Maven ecosystem conventions; standard tooling ([os-maven-plugin](#references), Gradle variant resolution) handles classifier selection.
- Aligns with DJL's proven distribution strategy for large native ML runtimes.

**Drawbacks:**

- Requires publishing 6–8 additional Maven artifacts per release.
- Users building portable über-JARs must explicitly include all classifiers they wish to support.
- Slightly more complex `pom.xml` / `build.gradle` for users who need cross-platform packaging.

### Option 3: Download on demand

The SDK ships a minimal placeholder that detects the current platform at runtime and downloads the correct `runtime.node` binary from a distribution endpoint (GitHub Releases or a CDN) on first use, caching it locally (e.g., `~/.copilot/runtime-cache/`).

**Advantages:**

- Zero native binary content in any published Maven artifact; total download at `mvn install` is negligible.
- Identical user experience to the current "externally provided runtime" model during the download, which most CLI users already accept.

**Drawbacks:**

- Requires internet access on first run. Offline environments (air-gapped enterprise, CI without outbound HTTP) break silently or require manual pre-seeding.
- Introduces a network dependency into an otherwise pure library artifact, which violates Maven Central's expectations for reproducible builds.
- Adds an operational concern: distribution endpoint availability, CDN costs, URL stability across versions.
- Makes JVM startup non-deterministic in latency (first run downloads 20–26 MB).
- Cannot be pre-warmed by dependency management tooling; no `mvn dependency:resolve` analogue works for a runtime download.

## Decision outcome

**Chosen: Option 2, per-platform classifier JARs, with Option 1 available through a consumer-built monolithic JAR.** Consumers can use `maven-assembly-plugin` to merge the platform classifiers they need.

### Rationale

1. **User download cost matches actual need.** Most users run on one OS and architecture. Option 2 avoids downloading every platform artifact. During the active migration, each classifier also carries the embedded-host executable and is larger than the runtime-only target.

2. **Proven ecosystem pattern.** DJL, Netty, and others have established the per-classifier pattern as the correct Maven idiom for large native binaries. Build tooling already knows how to handle it; users and framework integrations (Spring Boot, Quarkus, Micronaut) are familiar with it.

3. **Cache efficiency.** Individual platform JARs change only when that platform's binary changes. Unchanged platform JARs are never re-downloaded or re-cached by CI or developer machines.

4. **No operational dependencies.** Unlike Option 3, no external download service is required at runtime. The artifact is self-contained once resolved by Maven/Gradle.

5. **The distribution model remains valid as artifact size changes.** The current transitional classifier is large because it contains both the runtime and CLI. The classifier model still prevents users from downloading artifacts for unrelated platforms, and its size decreases when the embedded-host executable is no longer required.

6. **Option 3 remains composable.** A download-on-demand fallback can be layered on top of Option 2 for users who prefer it without changing the primary distribution model. The coordination artifact can attempt classpath lookup first, then fall back to a cached download if no matching classifier JAR is present.

7. See [How to support classifier and monolithic JARs](#how-to-support-classifier-and-monolithic-jars) for more details.

### Transport selection and failure behavior

Adding a classifier JAR does not change the client's connection automatically. Users opt in with:

```java
CopilotClientOptions options = new CopilotClientOptions()
    .setConnection(RuntimeConnection.forInProcess());
```

The `COPILOT_SDK_DEFAULT_CONNECTION=inprocess` environment variable also selects the in-process connection when no explicit or legacy subprocess options override it.

The selected connection is strict:

* If the user selects in-process and native resolution or startup fails, `CopilotClient.start()` fails.
* The SDK does not silently retry with stdio or TCP.
* If the user does not select in-process, classifier JARs are ignored and the existing stdio, TCP, or URI behavior remains unchanged.

### Runtime resolution order

When the in-process connection is selected, the Java loader resolves `runtime.node` in this order:

1. `COPILOT_CLI_PATH`: accept either a flat sibling `runtime.node` or the npm `prebuilds/<classifier>/runtime.node` layout.
1. Classpath resource: extract `native/<classifier>/runtime.node` and the bundled CLI from the classifier or monolithic JAR into a cache keyed by SDK version, native package version, and classifier.
1. PATH compatibility fallback: find `copilot` on `PATH` and accept a flat sibling `runtime.node`.

If none succeeds, startup fails. The PATH fallback does not claim to support every npm or Homebrew installation layout.

### Current platform scope

The platform detector recognizes the 8 classifiers listed in this ADR. The Maven build binds native packaging only for a host matching an implemented classifier. Windows x64 hosts package `win32-x64` automatically, Apple Silicon macOS hosts package `darwin-arm64` automatically, and Linux x64 glibc hosts can opt in with `copilot.native.libc=glibc`. The `inprocess` test profile selects the matching implemented classifier automatically on all three hosts. Every path validates the host before downloading or packaging native files. Linux x64 musl and other unsupported hosts build only the OS-neutral placeholder, sources, and Javadoc artifacts unless they explicitly request in-process tests, which fail during host validation. Additional classifier artifacts remain follow-up work.

Maven Central release and snapshot workflows build the Windows classifier on Windows, the Darwin classifier on Apple Silicon macOS, and the Linux classifier on Ubuntu from the same immutable source. The Windows and macOS jobs each upload only their verified classifier and checksum manifest. The Ubuntu job verifies and attaches both classifiers, builds `linux-x64` with the glibc opt-in, and performs the only Maven deployment. Release signing therefore covers the neutral artifacts and all three classifiers in one deployment.

## Binding technology: JNA over Panama FFM

A secondary decision within the scope of this ADR is _how_ the coordination artifact calls the C ABI entry points once the correct `runtime.node` binary has been loaded. Two candidates were considered: [JNA](#references) and the [Foreign Function & Memory API](#references) (FFM, the product of [Project Panama](#references), final since Java 22 via [JEP 454](#references)).

**Chosen: JNA.** FFM was considered and deliberately deferred, for the following reasons:

1. **Java baseline.** The SDK supports Java 17, where FFM does not exist (it finalized in Java 22). A JNA-based binding is therefore required regardless; adopting FFM today would mean maintaining two parallel binding implementations, not replacing one with the other.

2. **Consumer-side configuration burden.** FFM downcalls and upcalls are restricted operations under the JDK's integrity-by-default direction ([JEP 472](#references)). An FFM-based SDK would require every consumer to grant native access explicitly — `--enable-native-access=<module>` (or `ALL-UNNAMED` for classpath applications) on the launcher, or an `Enable-Native-Access` manifest attribute. JNA requires no consumer-side configuration today. For an SDK, this flag becomes every downstream application's problem and a predictable source of support issues. (JNA is on the same enforcement trajectory eventually, as it uses JNI internally; this consideration buys time, not immunity.)

3. **No realizable performance benefit.** FFM's principal advantage over JNA is the elimination of per-call reflective marshalling overhead. The C ABI surface here is a fixed set of 5 entry points carrying JSON-RPC bytes; JSON serialization and deserialization cost dominates the call path, and call frequency is bounded by agent-interaction rates rather than tight loops. The latency difference between JNA and FFM is expected to be unmeasurable in end-to-end SDK usage. This calculus would change only if the transport moved to a high-frequency or shared-memory framing model.

4. **Upcall lifetime complexity.** The transport is bidirectional: the runtime delivers JSON-RPC responses and server-initiated requests back into Java from native threads. JNA's `Callback` mechanism handles foreign-thread attachment with well-established semantics. FFM upcall stubs require explicit `Arena` lifetime management, where a stub whose arena is closed while the Rust side still holds the function pointer results in a JVM crash. This shifts lifetime reasoning that JNA encapsulates onto the binding layer.

5. **GraalVM native-image maturity.** JNA's behavior under GraalVM native-image is well established with mature reachability metadata. FFM support in native-image (particularly for upcalls) is newer and varies by GraalVM release. Plausible SDK consumers (e.g., Quarkus/Micronaut-based CLI tools) compile to native images, so this is a compatibility surface the SDK should not destabilize without verification.

6. **FFM's safety advantages do not apply to this ABI shape.** FFM's `MemorySegment` bounds and lifetime checking pays off when Java code performs structural manipulation of native memory. This surface passes strings through a fixed transport; there is little structural memory work to make safe.

### Preserving the FFM migration path

FFM is regarded as the likely eventual binding technology: the JEP 472 endgame applies enforcement pressure to JNA as well, and a 5-function stable C ABI makes a future migration inexpensive. To keep that path open at low cost:

- The binding layer is abstracted behind a small internal interface (native load + downcall + upcall registration), so that an FFM implementation can be introduced later — for example, as a multi-release JAR selecting FFM on Java 22+ — without changes to the transport or API layers.
- The decision should be revisited when (a) the SDK's minimum Java baseline moves past 17, or (b) JDK releases begin enforcing `--illegal-native-access=deny` by default, whichever comes first.

## How to support classifier and monolithic JARs

### Classpath resource convention and platform detection

#### Each classifier JAR uses a well-known resource path

Each per-platform JAR places its artifacts under a deterministic path:

```
native/darwin-arm64/runtime.node
native/darwin-arm64/platform.properties
native/darwin-arm64/copilot
```

Windows classifiers use `copilot.exe`. The CLI entrypoint remains in the classifier while the runtime requires the transitional embedded host.

When `maven-assembly-plugin` creates the uber-JAR, it unpacks all dependencies and merges them. The resulting uber-JAR contains the selected platforms:

```
com/github/copilot/sdk/...          (Java classes)
native/linux-x64/runtime.node
native/linux-x64/copilot
native/linux-arm64/runtime.node
native/linux-arm64/copilot
native/linuxmusl-x64/runtime.node
native/linuxmusl-x64/copilot
native/linuxmusl-arm64/runtime.node
native/linuxmusl-arm64/copilot
native/darwin-x64/runtime.node
native/darwin-x64/copilot
native/darwin-arm64/runtime.node
native/darwin-arm64/copilot
native/win32-x64/runtime.node
native/win32-x64/copilot.exe
native/win32-arm64/runtime.node
native/win32-arm64/copilot.exe
```

#### The coordination artifact selects at runtime through the classloader

`NativeRuntimeLoader` detects the current classifier and requests `native/<classifier>/runtime.node`, `native/<classifier>/platform.properties`, and `native/<classifier>/copilot` from the classloader. It uses the native package version from `platform.properties` as part of the cache identity, writes each executable artifact to a unique sibling temporary file, forces the file contents to storage, and atomically publishes the completed file into `~/.copilot/runtime-cache/<sdk-version>/<native-version>/<classifier>/`.

On non-Windows platforms, the loader makes the temporary CLI executable and verifies its executable status before atomic publication. A nonempty but non-executable cached CLI is repaired instead of being accepted as valid.

#### JNA loads from the extracted path

Once extracted to a known filesystem path, JNA loads it directly:

```java
CopilotRuntimeLibrary runtime =
    Native.load(extractedPath.toString(), CopilotRuntimeLibrary.class);
```

#### The same code works in both modes

Classloader resource lookup works identically whether:

* The native artifacts live in a separate classifier JAR on the classpath.
* The artifacts have been merged into an uber-JAR by `maven-assembly-plugin`.

The classloader searches the entire classpath, so the Java loading code does not change between the two consumption models.

### Consumer-side assembly plugin configuration

A consumer building a portable uber-jar would configure:

```xml
<plugin>
    <artifactId>maven-assembly-plugin</artifactId>
    <configuration>
        <descriptorRefs>
            <descriptorRef>jar-with-dependencies</descriptorRef>
        </descriptorRefs>
    </configuration>
</plugin>
```

With all classifier JARs declared as dependencies:

```xml
<dependencies>
    <dependency>
        <groupId>com.github</groupId>
        <artifactId>copilot-sdk-java</artifactId>
        <version>${copilot.version}</version>
    </dependency>
    <!-- Include the platforms you need -->
    <dependency>
        <groupId>com.github</groupId>
        <artifactId>copilot-sdk-java-runtime</artifactId>
        <version>${copilot.version}</version>
        <classifier>linux-x64</classifier>
    </dependency>
    <dependency>
        <groupId>com.github</groupId>
        <artifactId>copilot-sdk-java-runtime</artifactId>
        <version>${copilot.version}</version>
        <classifier>darwin-arm64</classifier>
    </dependency>
    <!-- Repeat for each target platform -->
</dependencies>
```

### Why this works cleanly

| Concern                      | How it's handled                                                                                                                   |
| ---------------------------- | ---------------------------------------------------------------------------------------------------------------------------------- |
| No resource path collisions  | Each platform has its own subdirectory (`native/<classifier>/`)                                                                    |
| Extraction only happens once | Cached to `~/.copilot/runtime-cache/<sdk-version>/<native-version>/<classifier>/`                                                   |
| Works without uber-JAR too   | The classloader finds the same resource in a separate classifier JAR                                                               |
| Subset selection             | Consumer declares only the classifiers they need; missing platforms get a clear error at runtime                                   |
| JNA loading                  | `Native.load(path, interface)` loads from an absolute filesystem path after extraction                                             |

The pattern follows DJL's `LibUtils.loadLibrary()` approach: detect the platform, construct the resource path, extract when needed, and load from an absolute path.

## Consequences

* The `copilot-sdk-java-runtime` Maven module holds the per-platform classifier JARs. Users add the classifier for each platform they intend to run.
* Users selecting in-process mode also add JNA. The coordination artifact does not force native dependencies on users who keep the default subprocess connection.
* The coordination artifact includes platform detection and native loading code that:
  1. Detects OS, architecture, and Linux libc variant deterministically as described above.
  2. Locates the matching `runtime.node` binary on the classpath (via `getResourceAsStream` from the classifier JAR).
  3. Extracts `runtime.node` and the transitional CLI entrypoint into `~/.copilot/runtime-cache/` if valid cached files are not already present.
  4. Loads it via [JNA](#references) using the C ABI entry points, per the [binding technology decision](#binding-technology-jna-over-panama-ffm) above. The JNA-specific code is confined behind an internal binding interface to preserve a future FFM migration path.
* A validated supported-host profile fetches the pinned matching `@github/copilot-<classifier>` npm package, verifies its SHA-512 integrity from `nodejs/package-lock.json`, and packages the version-matched runtime and CLI files.
* The current release work publishes the `linux-x64`, `win32-x64`, and `darwin-arm64` classifiers. The planned classifier set expands to the other detected platforms.
* Adding an implemented platform requires validated host activation, a profile that supplies the classifier and platform CLI filename, and lifecycle bindings for the shared host validation, fetch, script test, package, and verification executions.
* `cli-native.node` is not bundled. It provides terminal UI features that are irrelevant to the Java SDK's programmatic API surface.

## Related work items

* https://github.com/github/copilot-sdk/issues/1917: Epic to embed the Rust-based Copilot CLI runtime
* https://devdiv.visualstudio.com/DevDiv/_workitems/edit/3028097
* https://github.com/github/copilot-sdk/pull/1901: .NET in-process FFI runtime hosting
* https://github.com/github/copilot-sdk/pull/1915: In-process FFI transport for Rust and TypeScript SDKs

### References

| Term                                        | Definition                                                                                                                                                                                                                                                                                                                                                                                           | Link                                                                                 |
| ------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------ |
| **FFI** (Foreign Function Interface)        | A mechanism by which code written in one language can call functions defined in another. In this ADR, Java calls into the Rust runtime shared library via JNA's FFI layer.                                                                                                                                                                                                                           | https://en.wikipedia.org/wiki/Foreign_function_interface                             |
| **JNA** (Java Native Access)                | A Java library that provides easy access to native shared libraries without requiring the JNI boilerplate. Used here to call the `extern "C"` C ABI entry points exported by `runtime.node`.                                                                                                                                                                                                         | https://github.com/java-native-access/jna                                            |
| **napi-rs**                                 | A Rust framework for building native Node.js addons using the Node-API (napi) stable ABI. Produces the `.node` file and generates TypeScript type declarations automatically.                                                                                                                                                                                                                        | https://napi.rs/                                                                     |
| **cdylib**                                  | A Rust `crate-type` that produces a C-compatible dynamic shared library (`.so` / `.dylib` / `.dll`). Distinct from `dylib` (Rust-to-Rust only) and `staticlib`.                                                                                                                                                                                                                                      | https://doc.rust-lang.org/reference/linkage.html                                     |
| **napi (Node-API)**                         | A stable C ABI provided by Node.js for building native addons that remain binary-compatible across Node.js versions. `napi-rs` generates Rust code against this interface.                                                                                                                                                                                                                           | https://nodejs.org/api/n-api.html                                                    |
| **C ABI** (Application Binary Interface)    | The low-level contract between a compiled binary and its callers: calling conventions, data type layouts, symbol naming. An `extern "C"` ABI uses C's conventions, making a library callable from any language that speaks C FFI.                                                                                                                                                                    | https://en.wikipedia.org/wiki/Application_binary_interface                           |
| **ELF PT_INTERP**                           | A segment in an [ELF](https://man7.org/linux/man-pages/man5/elf.5.html) binary (the Linux/Unix executable format) that records the path of the dynamic linker/interpreter. On glibc systems this path is `/lib64/ld-linux-x86-64.so.2`; on musl systems it is `/lib/ld-musl-x86_64.so.1`. Inspecting it is the most reliable way to detect glibc vs. musl at runtime without executing a subprocess. | https://man7.org/linux/man-pages/man5/elf.5.html                                     |
| **glibc** (GNU C Library)                   | The standard C runtime library on most mainstream Linux distributions (Debian, Ubuntu, RHEL, Fedora, SLES). Binaries linked against glibc require the same version or newer to be present at runtime. The `runtime.node` glibc build requires glibc ≥ 2.28.                                                                                                                                          | https://www.gnu.org/software/libc/                                                   |
| **musl libc**                               | An alternative C standard library optimised for static linking and used as the default libc on Alpine Linux. Not binary-compatible with glibc; a separate `runtime.node` build is required.                                                                                                                                                                                                          | https://musl.libc.org/                                                               |
| **MSVC CRT** (Microsoft Visual C++ Runtime) | The C runtime library shipped with Visual Studio. When compiled with `+crt-static` (as `runtime.node` is on Windows), it is statically linked into the binary and the end-user does not need to install the Visual C++ Redistributable.                                                                                                                                                              | https://learn.microsoft.com/en-us/cpp/c-runtime-library/c-run-time-library-reference |
| **Project Panama**                          | The OpenJDK project that produced the Foreign Function & Memory API as the modern, supported replacement for JNI-based native interop.                                                                                                                                                                                                                                                               | https://openjdk.org/projects/panama/                                                 |
| **FFM** (Foreign Function & Memory API)     | The `java.lang.foreign` API for calling native functions and managing native memory from Java, finalized in Java 22. Considered and deferred as the binding technology for this SDK; see [Binding technology](#binding-technology-jna-over-panama-ffm).                                                                                                                                              | https://docs.oracle.com/en/java/javase/22/core/foreign-function-and-memory-api.html  |
| **JEP 454**                                 | The JDK Enhancement Proposal that finalized the FFM API in Java 22.                                                                                                                                                                                                                                                                                                                                  | https://openjdk.org/jeps/454                                                         |
| **JEP 472**                                 | "Prepare to Restrict the Use of JNI" — part of the JDK's integrity-by-default direction under which native access (via JNI or FFM) requires explicit consumer opt-in (`--enable-native-access`). Drives both the FFM configuration-burden concern and the expectation that JNA itself will eventually require the same opt-in.                                                                       | https://openjdk.org/jeps/472                                                         |
| **DJL** (Deep Java Library)                 | Amazon's open-source Java framework for ML inference, used here as a reference for the per-platform classifier JAR distribution pattern. Its PyTorch native artifacts (`pytorch-native-cpu-*-<platform>.jar`) are the direct model for the proposed `copilot-sdk-java-runtime:VERSION:<classifier>` artifacts.                                                                                       | https://djl.ai/                                                                      |
| **os-maven-plugin**                         | A Maven extension that detects the current OS and architecture and exposes them as properties (e.g., `${os.detected.classifier}`) so that `<classifier>` values can be resolved at build time rather than hardcoded.                                                                                                                                                                                 | https://github.com/trustin/os-maven-plugin                                           |
| **ONNX Runtime**                            | Microsoft's cross-platform ML inference runtime, used in this ADR as the size comparable for a monolithic all-platform JAR (~130 MB, Option 1).                                                                                                                                                                                                                                                      | https://onnxruntime.ai/                                                              |

Additional source references:

- DJL native distribution pattern: https://github.com/deepjavalibrary/djl/tree/master/engines/pytorch/pytorch-native
- DJL `Platform.fromSystem()` (OS/arch detection): https://github.com/deepjavalibrary/djl/blob/master/api/src/main/java/ai/djl/util/Platform.java
- `detect-libc` npm package (ELF PT_INTERP libc detection): https://github.com/lovell/detect-libc
- `github/copilot-agent-runtime` C ABI front door (`cabi.rs`): `src/runtime/src/interop/cabi.rs`
- `github/copilot-agent-runtime` build target definitions: `script/build-runtime.ts`
- `github/copilot-agent-runtime` glibc sysroot and verification: `script/linux/install-sysroot.cjs`, `script/linux/verify-glibc-requirements.sh`
- ONNX Runtime Java on Maven Central (size comparable): https://repo1.maven.org/maven2/com/microsoft/onnxruntime/onnxruntime/1.21.0/
