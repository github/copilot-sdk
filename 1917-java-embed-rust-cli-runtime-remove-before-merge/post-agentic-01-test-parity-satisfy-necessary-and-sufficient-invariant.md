# Prompt: Satisfy the necessary-and-sufficient runtime artifact invariant

## Goal

When `cd java && mvn clean verify -Pinprocess` is invoked, all tests pass cleanly. Currently the InProcess tests hang or fail because `copilot_runtime_host_start` cannot find the copilot CLI executable to spawn as a child process.

## Branch

Work on branch `edburns/review-copilot-pr-2272` in `copilot-sdk-01`. The current HEAD is `f36371ac`.

## Background

The InProcess transport loads `runtime.node` (a Rust cdylib) via JNA and calls `copilot_runtime_host_start(argv_json, env_json)`. Internally, the Rust code in `embedded_host.rs` uses `argv[0]` from `argv_json` as the program in `Command::new(program)` to spawn a child process — the copilot CLI binary that services TypeScript method bodies not yet ported to Rust.

Today the classifier JAR (`copilot-sdk-java-runtime-*-linux-x64.jar`) only contains `native/linux-x64/runtime.node`. The copilot CLI executable is **not** included, even though it ships in the same `@github/copilot-linux-x64` npm tarball at path `package/copilot`.

The `resolveInProcessEntrypoint()` method in `CopilotClient.java` tries to find the copilot CLI via `COPILOT_CLI_PATH` env, `options.getCliPath()`, or PATH — all independent of where `runtime.node` was resolved. This is wrong. The copilot CLI must come from the **same** package as `runtime.node` to avoid version skew.

## The necessary-and-sufficient runtime artifact invariant

The classifier JAR must contain **both**:
- `native/<classifier>/runtime.node` — the cdylib loaded via JNA
- `native/<classifier>/copilot` — the CLI executable passed as `argv[0]` to `host_start`

These two files must come from the same `@github/copilot-<platform>` npm package version. This matches how the .NET SDK bundles both under `runtimes/<rid>/native/`.

## Changes required

### 1. `java/copilot-native/scripts/fetch-native.mjs` — also extract the copilot binary

Currently the script extracts only `package/prebuilds/<classifier>/runtime.node` from the npm tarball. It must **also** extract `package/copilot` and stage it at `target/native-staging/<classifier>/native/<classifier>/copilot`.

After extraction, set the executable permission on the copilot binary (`chmod +x` or `fs.chmodSync(..., 0o755)`).

The tarball paths are:
- `package/prebuilds/<classifier>/runtime.node` → `<staging>/<classifier>/native/<classifier>/runtime.node` (already done)
- `package/copilot` → `<staging>/<classifier>/native/<classifier>/copilot` (NEW)

On Windows the binary is named `copilot.exe` and lives at `package/copilot.exe` in the tarball.

### 2. `java/sdk/src/main/java/com/github/copilot/ffi/NativeRuntimeLoader.java` — add method to resolve the copilot CLI from the same location as runtime.node

Add a new public method `resolveEntrypoint()` that returns the path to the copilot CLI executable. The logic is:

1. Call `resolve()` to get the path to `runtime.node` (e.g. `~/.copilot/runtime-cache/<version>/linux-x64/runtime.node`)
2. Look for `copilot` (or `copilot.exe` on Windows) in the **same directory** as the resolved `runtime.node`
3. If found and is a regular file, return it
4. If not found, throw `IOException` with a clear message

When `resolve()` extracts `runtime.node` from the classpath to the cache directory, it must **also** extract `native/<classifier>/copilot` to the same cache directory. Update the extraction logic in `resolve()` (or `resolveFromClasspathOrBundledCli`) to extract the copilot binary alongside `runtime.node`. The copilot binary is a classpath resource at `native/<classifier>/copilot`.

After extraction, ensure the copilot binary has executable permission (`Files.setPosixFilePermissions` or similar, guarded for non-POSIX systems).

### 3. `java/sdk/src/main/java/com/github/copilot/CopilotClient.java` — simplify `resolveInProcessEntrypoint()`

Replace the current three-step independent resolution with:

```java
private static String resolveInProcessEntrypoint(CopilotClientOptions options) throws IOException {
    return NativeRuntimeLoader.resolveEntrypoint().toString();
}
```

The copilot CLI is always derived from the same location as `runtime.node`. There are no other loading mechanisms. No `COPILOT_CLI_PATH` check. No `options.getCliPath()` check. No PATH search. The InProcess entrypoint comes from the bundled classifier JAR, period.

If the user has NOT configured `RuntimeConnection.forInProcess()`, this method is never called — the SDK falls back to the existing subprocess transport via `CliServerManager`, which uses `COPILOT_CLI_PATH` / PATH as before. That fallback path is unchanged.

### 4. `java/sdk/src/test/java/com/github/copilot/E2ETestContext.java` — simplify InProcess test setup

In `applyContextOptions()`, the InProcess branch currently creates an `InProcessEnvGuard` that sets `COPILOT_CLI_PATH` in the native env. This is no longer needed because `resolveInProcessEntrypoint` no longer reads `COPILOT_CLI_PATH`.

The `InProcessEnvGuard` is still needed for other env vars (`COPILOT_API_URL`, `GITHUB_TOKEN`, etc.) that the Rust runtime reads from the process environment. But `COPILOT_CLI_PATH` should be removed from `buildInProcessEnvironment()`.

In `buildInProcessEnvironment()`, remove the line:
```java
env.put("COPILOT_CLI_PATH", cliPath);
```

### 5. Verify the classifier JAR contents

After `mvn clean package -pl copilot-native`, the classifier JAR at `java/copilot-native/target/copilot-sdk-java-runtime-*-linux-x64.jar` must contain:
```
native/linux-x64/runtime.node
native/linux-x64/copilot
native/linux-x64/platform.properties
```

### 6. Verify tests pass

Run `cd java && mvn clean verify -Pinprocess` and confirm all tests pass. The `-Pinprocess` profile activates the `copilot-native` module build and sets `COPILOT_SDK_DEFAULT_CONNECTION=inprocess` for the E2E tests.

## What NOT to change

- Do NOT change the subprocess transport path (`CliServerManager`, `TcpRuntimeConnection`, `StdioRuntimeConnection`). Those paths continue to use `COPILOT_CLI_PATH` / PATH / `options.getCliPath()` as before.
- Do NOT add `COPILOT_CLI_PATH` as a resolution mechanism for the InProcess entrypoint. InProcess uses only the bundled artifact.
- Do NOT change the `RuntimeConnection.forInProcess()` API or `InProcessRuntimeConnection` class.
- Do NOT change the Rust code in `copilot-agent-runtime`.
- Do NOT change any code outside the `java/` directory except this prompt file.

## Key file locations

| File | Purpose |
|------|---------|
| `java/copilot-native/scripts/fetch-native.mjs` | Downloads and extracts native binaries from npm |
| `java/copilot-native/pom.xml` | Builds the classifier JAR |
| `java/sdk/src/main/java/com/github/copilot/CopilotClient.java` | `resolveInProcessEntrypoint()` at ~line 481 |
| `java/sdk/src/main/java/com/github/copilot/ffi/NativeRuntimeLoader.java` | `resolve()` and `findRuntimeOnPath()` |
| `java/sdk/src/main/java/com/github/copilot/ffi/FfiRuntimeHost.java` | `start()` calls `buildArgvJson(entrypointPath, ...)` |
| `java/sdk/src/test/java/com/github/copilot/E2ETestContext.java` | `buildInProcessEnvironment()` and `applyContextOptions()` |
| `java/sdk/src/test/java/com/github/copilot/ffi/NativeRuntimeLoaderTest.java` | Unit tests for NativeRuntimeLoader |

## Verification command

```bash
cd java && mvn clean verify -Pinprocess
```

All tests must pass. No hangs. No timeouts.
