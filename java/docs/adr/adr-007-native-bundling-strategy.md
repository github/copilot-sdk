# ADR-007: Native runtime bundling strategy — per-platform classifier JARs

## Context and Problem Statement

The Copilot SDK for Java currently has no embedded runtime. It depends on an externally provided runtime process (see epic [#1917](https://github.com/github/copilot-sdk/issues/1917)). The ongoing Rust port of the `copilot-agent-runtime` repository is reaching the point where the runtime can be consumed as a native shared library without requiring a Node.js process, making it practical to embed the runtime directly in the SDK JAR.

### The runtime artifact

The artifact to be embedded is `runtime.node`, a Rust [`cdylib`](#references) produced by the `src/runtime` crate in `github/copilot-agent-runtime` using the [napi-rs](#references) build toolchain. Despite the `.node` file extension (a naming convention of napi-rs), this is an ordinary platform-specific shared library (`.so` on Linux, `.dylib` on macOS, `.dll` on Windows). It exposes two front doors built over the same internal engine:

- **[napi](#references) front door** — loaded by a Node.js process as a native addon (current CLI path).
- **[C ABI](#references) front door** — a fixed set of approximately 12 `extern "C"` lifecycle and transport entry points (`copilot_runtime_server_create`, `copilot_runtime_connection_open`, etc.) that any language can call in-process via [FFI](#references) ([JNA](#references) for Java, Python/cffi, C#/`DllImport`, Go/purego) **without a Node.js process**. All API methods travel as JSON-RPC data through this fixed transport; the export list never changes as the method set grows.

The `cli-native.node` addon — a separate, smaller artifact that provides ICU4X text segmentation, Win32 API wrappers, and terminal UI helpers — is a CLI-only artifact used by the Ink/React terminal interface. It is **not needed** by the Java SDK.

### Note on the active Rust migration

As of 2026-07, the `runtime.node` binary is being built up iteratively as TypeScript runtime code is ported into it. It is **not** being reduced; it is growing with each port PR. The `embedded_host.rs` module in the runtime currently spawns a short-lived child process to service method bodies not yet ported to Rust. This internal Node.js dependency shrinks with each port PR and is expected to disappear entirely when the migration completes. The C ABI surface and loading mechanism described in this ADR are stable regardless of migration progress.

### Platform dimensions

The runtime must be built for each unique combination of OS, CPU architecture, and (on Linux) C runtime variant. The build system in `github/copilot-agent-runtime` produces eight Rust target triples:

| Platform label | Rust triple | Constraint |
|---------------|-------------|------------|
| `linux-x64` | `x86_64-unknown-linux-gnu` | [glibc](#references) ≥ 2.28 (Debian 10+, Ubuntu 20.04+, RHEL 8+) |
| `linux-arm64` | `aarch64-unknown-linux-gnu` | glibc ≥ 2.28 |
| `linuxmusl-x64` | `x86_64-unknown-linux-musl` | dynamically links [musl libc](#references) (Alpine Linux) |
| `linuxmusl-arm64` | `aarch64-unknown-linux-musl` | dynamically links musl libc |
| `darwin-x64` | `x86_64-apple-darwin` | macOS, Intel |
| `darwin-arm64` | `aarch64-apple-darwin` | macOS, Apple Silicon |
| `win32-x64` | `x86_64-pc-windows-msvc` | [MSVC CRT](#references) statically linked (`+crt-static`) |
| `win32-arm64` | `aarch64-pc-windows-msvc` | MSVC CRT statically linked (`+crt-static`) |

The GNU/Linux glibc minimum of 2.28 is enforced at build time via a Microsoft/vscode-linux-build-agent sysroot and verified post-build by `script/linux/verify-glibc-requirements.sh`. The musl binaries are **not** fully statically linked; they dynamically link musl libc (`-C target-feature=-crt-static` is explicitly set at build time).

The **common case** (Windows × 2 + macOS × 2 + GNU/Linux × 2) requires **6 binaries**. Supporting Alpine Linux adds 2 more musl binaries for a total of **8**.

### Platform selection is 100% deterministic

The correct binary can be selected at runtime without any heuristics, using only standard Java and OS APIs:

1. **OS**: `System.getProperty("os.name")` — distinguishes Windows, macOS, and Linux unambiguously.
2. **Architecture**: `System.getProperty("os.arch")` — `"amd64"` and `"x86_64"` both map to `x64`; `"aarch64"` and `"arm64"` both map to `arm64`.
3. **Linux libc variant**: Read the first 2 KB of `/proc/self/exe` and parse the [ELF](#references) PT_INTERP segment (the dynamic linker path). If the interpreter path contains `/ld-musl-` → musl; if it contains `/ld-linux-` → glibc. This requires no subprocess, no PATH lookup, and works inside containers. This is the same approach used by the `detect-libc` npm package (its primary, most reliable detection method).

### Size baseline

Measured from `github/copilot-agent-runtime` release `cli-1.0.69-2` (2026-07-06):

| Platform | `runtime.node` (uncompressed) | Compressed (~40% deflate) |
|----------|------------------------------|--------------------------|
| `linux-x64` | 64.7 MB | ~25.9 MB |
| `linux-arm64` | 55.5 MB | ~22.2 MB |
| `linuxmusl-x64` | 64.4 MB | ~25.8 MB |
| `linuxmusl-arm64` | 55.3 MB | ~22.1 MB |
| `darwin-x64` | 57.3 MB | ~22.9 MB |
| `darwin-arm64` | 48.1 MB | ~19.2 MB |
| `win32-x64` | 55.9 MB | ~22.4 MB |
| `win32-arm64` | 48.4 MB | ~19.4 MB |

The published Java SDK JAR (`copilot-sdk-java-1.0.6-preview.1.jar`) is currently **1.53 MB**. A monolithic JAR containing all 6 common-case native binaries would be approximately **132 MB** compressed; all 8 including musl would be approximately **180 MB** compressed.

All native dependencies within the runtime (`rustls`/`aws-lc-rs` for TLS, `rusqlite` with `bundled` feature for SQLite, `zlib-rs` for compression) are statically compiled into the binary. There are no dependencies on system OpenSSL, libgit2, or libz.

## Considered Options

### Option 1: Monolithic JAR — all platform binaries in one artifact

All 6 (or 8) `runtime.node` binaries are bundled inside the single `copilot-sdk-java` artifact. At runtime the SDK extracts and loads the one matching the current platform; the remaining 5–7 are carried silently.

**Advantages:**
- Single `<dependency>` in `pom.xml`; zero extra configuration for users.
- Familiar pattern: [ONNX Runtime](#references) (`onnxruntime-1.21.0.jar`, **130 MB**, all platforms) demonstrates this is an accepted norm in the Java ML ecosystem.

**Drawbacks:**
- Every user downloads every platform regardless of their target. A developer on Apple Silicon downloads 105+ MB of Linux and Windows binaries they will never use.
- Build tooling (thin Docker layers, incremental CI caches, artifact registries) penalises large JARs. A single 132–180 MB JAR invalidates the entire cache whenever any platform's binary changes.
- Maven's dependency resolution has no mechanism to supply platform-appropriate variants automatically; platform selection must happen entirely at runtime inside the JAR.
- Conflicts with the principle that Maven artifacts should be reproducible and minimal.

### Option 2: Per-platform classifier JARs ([DJL](#references) style)

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

Each classifier JAR contains only the `runtime.node` binary for that platform (~19–26 MB compressed) plus a small `.properties` metadata file. The coordination artifact selects and loads the matching native at startup.

This is the same pattern used by DJL's PyTorch native artifacts (`pytorch-native-cpu-2.5.1-linux-x86_64.jar`, `pytorch-native-cpu-2.5.1-osx-aarch64.jar`, etc.), Netty's `netty-tcnative-boringssl-static` per-platform JARs, and others.

Build tools can be configured to resolve the correct classifier automatically:

- **Maven**: `<classifier>${os.detected.classifier}</classifier>` via [os-maven-plugin](#references).
- **Gradle**: variant-aware dependency resolution with attribute matching.
- **Uber-jar builds**: include all classifiers; the coordination artifact picks the right one at runtime.

**Advantages:**
- Default download is the tiny coordination artifact (~1.5 MB) plus one platform JAR (~20–26 MB compressed) — approximately **22–28 MB total** vs. 132–180 MB for a monolithic JAR.
- Each platform JAR changes independently; CI caches and Docker layers for unchanged platforms are preserved across releases.
- Users building for a single known platform (most production deployments) pay exactly the cost of that platform.
- Follows well-established Maven ecosystem conventions; standard tooling ([os-maven-plugin](#references), Gradle variant resolution) handles classifier selection.
- Aligns with DJL's proven distribution strategy for large native ML runtimes.

**Drawbacks:**
- Requires publishing 6–8 additional Maven artifacts per release.
- Users building portable über-JARs must explicitly include all classifiers they wish to support.
- Slightly more complex `pom.xml` / `build.gradle` for users who need cross-platform packaging.

### Option 3: Download-on-demand (DJL thin placeholder style)

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

## Decision Outcome

**Chosen: Option 2 — per-platform classifier JARs.**

### Rationale

1. **User download cost matches actual need.** Most users run on one OS and architecture. Option 2 makes their download approximately 22–28 MB (coordination JAR + one platform JAR), versus 132–180 MB for Option 1 and an unbounded deferred network cost for Option 3.

2. **Proven ecosystem pattern.** DJL, Netty, and others have established the per-classifier pattern as the correct Maven idiom for large native binaries. Build tooling already knows how to handle it; users and framework integrations (Spring Boot, Quarkus, Micronaut) are familiar with it.

3. **Cache efficiency.** Individual platform JARs change only when that platform's binary changes. Unchanged platform JARs are never re-downloaded or re-cached by CI or developer machines.

4. **No operational dependencies.** Unlike Option 3, no external download service is required at runtime. The artifact is self-contained once resolved by Maven/Gradle.

5. **Size per platform is acceptable.** At ~20–26 MB compressed per platform, each classifier JAR is well within the range of routinely used native JARs in the Java ecosystem (DJL PyTorch osx-aarch64: 37 MB; ONNX Runtime per platform: ~20–30 MB before bundling).

6. **Option 3 remains composable.** A download-on-demand fallback can be layered on top of Option 2 for users who prefer it without changing the primary distribution model. The coordination artifact can attempt classpath lookup first, then fall back to a cached download if no matching classifier JAR is present.

## Consequences

- A new Maven module (`copilot-sdk-java-runtime` or similar) is introduced to hold the per-platform native JARs. The existing `copilot-sdk-java` coordination artifact depends on it.
- The coordination artifact gains a platform detection and native loading component that:
  1. Detects OS, architecture, and Linux libc variant deterministically as described above.
  2. Locates the matching `runtime.node` binary on the classpath (via `getResourceAsStream` from the classifier JAR).
  3. Extracts the binary to a temporary or cached location (e.g., `~/.copilot/runtime-cache/`) if not already present.
  4. Loads it via [JNA](#references) using the C ABI entry points.
- The release pipeline for `github/copilot-agent-runtime` must produce the per-platform `runtime.node` binaries as inputs to the Java SDK publish workflow. The per-platform `pkg-tarballs-<platform>` artifacts from the `publish-cli.yml` workflow are the authoritative source.
- Each release of `copilot-sdk-java` publishes 6 (or 8) classifier JARs to Maven Central alongside the coordination JAR.
- The version of the bundled `runtime.node` is recorded in the coordination JAR's manifest and queryable at runtime, enabling diagnostics and mismatch detection.
- `cli-native.node` is not bundled. It provides only terminal UI features (ICU4X text segmentation, Win32 APIs, OS desktop notifications) that are irrelevant to the Java SDK's programmatic API surface.

## Related work items

- https://github.com/github/copilot-sdk/issues/1917 — Epic: Embed Rust-based Copilot CLI Runtime and cease requiring Node.js
- https://devdiv.visualstudio.com/DevDiv/_workitems/edit/3028097
- https://github.com/github/copilot-sdk/pull/1901 dotnet: in-process FFI runtime hosting (InProcess transport)
- https://github.com/github/copilot-sdk/pull/1915 Add in-process FFI transport for Rust and TypeScript SDKs

### References

| Term | Definition | Link |
|------|------------|------|
| **FFI** (Foreign Function Interface) | A mechanism by which code written in one language can call functions defined in another. In this ADR, Java calls into the Rust runtime shared library via JNA's FFI layer. | https://en.wikipedia.org/wiki/Foreign_function_interface |
| **JNA** (Java Native Access) | A Java library that provides easy access to native shared libraries without requiring the JNI boilerplate. Used here to call the `extern "C"` C ABI entry points exported by `runtime.node`. | https://github.com/java-native-access/jna |
| **napi-rs** | A Rust framework for building native Node.js addons using the Node-API (napi) stable ABI. Produces the `.node` file and generates TypeScript type declarations automatically. | https://napi.rs/ |
| **cdylib** | A Rust `crate-type` that produces a C-compatible dynamic shared library (`.so` / `.dylib` / `.dll`). Distinct from `dylib` (Rust-to-Rust only) and `staticlib`. | https://doc.rust-lang.org/reference/linkage.html |
| **napi (Node-API)** | A stable C ABI provided by Node.js for building native addons that remain binary-compatible across Node.js versions. `napi-rs` generates Rust code against this interface. | https://nodejs.org/api/n-api.html |
| **C ABI** (Application Binary Interface) | The low-level contract between a compiled binary and its callers: calling conventions, data type layouts, symbol naming. An `extern "C"` ABI uses C's conventions, making a library callable from any language that speaks C FFI. | https://en.wikipedia.org/wiki/Application_binary_interface |
| **ELF PT_INTERP** | A segment in an [ELF](https://man7.org/linux/man-pages/man5/elf.5.html) binary (the Linux/Unix executable format) that records the path of the dynamic linker/interpreter. On glibc systems this path is `/lib64/ld-linux-x86-64.so.2`; on musl systems it is `/lib/ld-musl-x86_64.so.1`. Inspecting it is the most reliable way to detect glibc vs. musl at runtime without executing a subprocess. | https://man7.org/linux/man-pages/man5/elf.5.html |
| **glibc** (GNU C Library) | The standard C runtime library on most mainstream Linux distributions (Debian, Ubuntu, RHEL, Fedora, SLES). Binaries linked against glibc require the same version or newer to be present at runtime. The `runtime.node` glibc build requires glibc ≥ 2.28. | https://www.gnu.org/software/libc/ |
| **musl libc** | An alternative C standard library optimised for static linking and used as the default libc on Alpine Linux. Not binary-compatible with glibc; a separate `runtime.node` build is required. | https://musl.libc.org/ |
| **MSVC CRT** (Microsoft Visual C++ Runtime) | The C runtime library shipped with Visual Studio. When compiled with `+crt-static` (as `runtime.node` is on Windows), it is statically linked into the binary and the end-user does not need to install the Visual C++ Redistributable. | https://learn.microsoft.com/en-us/cpp/c-runtime-library/c-run-time-library-reference |
| **DJL** (Deep Java Library) | Amazon's open-source Java framework for ML inference, used here as a reference for the per-platform classifier JAR distribution pattern. Its PyTorch native artifacts (`pytorch-native-cpu-*-<platform>.jar`) are the direct model for the proposed `copilot-sdk-java-runtime:VERSION:<classifier>` artifacts. | https://djl.ai/ |
| **os-maven-plugin** | A Maven extension that detects the current OS and architecture and exposes them as properties (e.g., `${os.detected.classifier}`) so that `<classifier>` values can be resolved at build time rather than hardcoded. | https://github.com/trustin/os-maven-plugin |
| **ONNX Runtime** | Microsoft's cross-platform ML inference runtime, used in this ADR as the size comparable for a monolithic all-platform JAR (~130 MB, Option 1). | https://onnxruntime.ai/ |

Additional source references:

- DJL native distribution pattern: https://github.com/deepjavalibrary/djl/tree/master/engines/pytorch/pytorch-native
- DJL `Platform.fromSystem()` (OS/arch detection): https://github.com/deepjavalibrary/djl/blob/master/api/src/main/java/ai/djl/util/Platform.java
- `detect-libc` npm package (ELF PT_INTERP libc detection): https://github.com/lovell/detect-libc
- `github/copilot-agent-runtime` C ABI front door (`cabi.rs`): `src/runtime/src/interop/cabi.rs`
- `github/copilot-agent-runtime` build target definitions: `script/build-runtime.ts`
- `github/copilot-agent-runtime` glibc sysroot and verification: `script/linux/install-sysroot.cjs`, `script/linux/verify-glibc-requirements.sh`
- ONNX Runtime Java on Maven Central (size comparable): https://repo1.maven.org/maven2/com/microsoft/onnxruntime/onnxruntime/1.21.0/

