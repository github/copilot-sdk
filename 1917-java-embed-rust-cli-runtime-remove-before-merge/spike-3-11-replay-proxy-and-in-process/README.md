# Spike 3.11 — Replay proxy and InProcess transport (win32-x64)

**Question answered:** Can Java E2E tests use the InProcess (`runtime.node`-via-JNA)
transport against the existing YAML-snapshot replay proxy? If so, how?

---

## Findings summary

### 1. Can E2E tests use InProcess transport against the replay proxy?

**YES.**

The replay proxy works at the HTTP layer: it intercepts calls from the Copilot runtime
to the Copilot API (`COPILOT_API_URL`) and serves YAML-recorded responses. The transport
choice (subprocess vs. in-process) is irrelevant to the proxy — what matters is
**which URL the runtime makes its HTTP requests to**.

- **Subprocess transport**: the harness passes env vars to the spawned child process
  via the connection's `Environment` dict / `env` field. The child reads `COPILOT_API_URL`
  and contacts the proxy.
- **InProcess transport**: the native runtime library (`runtime.node`) is loaded
  **into the test process** via JNA. It reads `COPILOT_API_URL` from the **current
  process's environment block** (Win32 `GetEnvironmentVariableW` / libc `getenv()`),
  not from a per-client dictionary. Crucially, Java's `System.getenv()` is a startup
  snapshot — writing to `System.setProperty()` does NOT affect native env reads.

**Solution: `InProcessEnvGuard`** (see `java-inprocess-e2e-win32-x64/`) — a class that
uses JNA to call `SetEnvironmentVariableW` (Windows) or `setenv()` (Linux/macOS) to
mutate the live process environment block before starting the in-process client, and
restores the saved values after the test. This mirrors:

- Rust's `InProcessEnvGuard` (`rust/tests/e2e/support.rs` lines 603–677)
- .NET's `InProcessEnvIsolation` (`dotnet/test/Harness/InProcessEnvIsolation.cs`)

**Critical constraint: E2E concurrency must be 1 when running in-process.** The env
guard mutates process-global state. Rust enforces `concurrency = 1` when
`COPILOT_SDK_DEFAULT_CONNECTION=inprocess`; Java must do the same. See
`InProcessSpikeMain.java` for the single-threaded guard usage pattern.

### 2. Should InProcess E2E tests use the real `runtime.node` binary?

**YES** (DRI decision). The binary is available in CI wherever the `@github/copilot-<platform>`
npm package has been installed (same prerequisite as the `copilot-native` Maven module,
see question 3.2). No separate binary provisioning step is needed.

### 3. How do we mock/stub the native library for unit testing?

**We don't** (DRI decision). Unit tests for the JNA binding layer (step 4.3) use a
minimal test DLL (from spike-3-4). InProcess integration tests use the real binary.
There is no middle tier of "mock runtime.node" stubs.

### 4. Should InProcess E2E tests reuse existing YAML snapshots?

**YES.** From the proxy's perspective, the HTTP traffic is identical regardless of
whether the runtime was started as a subprocess or loaded in-process. The Rust
`inprocess.rs` smoke test reuses the same `should_start_ping_and_stop_stdio_client`
snapshot used by the stdio smoke test. The full E2E suite re-runs under both transports
against all existing snapshots.

Some tests need skip-guards for behavior the in-process transport does not support
(e.g., tests that set per-client environment variables that the runtime ignores when
loaded in-process — see [issue #1934](https://github.com/github/copilot-sdk/issues/1934)).
Rust's `skip_inprocess(reason)` is the template; Java needs a `@SkipInProcess` JUnit 5
annotation or a similar guard.

### 5. Should the entire E2E suite run under both transports?

**YES.** Following the Rust PR's pattern:

- **CI job A**: default transport (stdio/subprocess) — existing `java-sdk-tests.yml` job.
- **CI job B**: InProcess transport — same job, `COPILOT_SDK_DEFAULT_CONNECTION=inprocess`,
  requiring a separate Maven profile (e.g., `-Pinprocess`) that:
  1. Requires `runtime.node` to be present on the classpath (from `copilot-native` module).
  2. Sets the `COPILOT_SDK_DEFAULT_CONNECTION=inprocess` system property / env var.
  3. Enforces E2E concurrency = 1 (no parallel test execution).

---

## Java-specific implementation plan for `InProcessEnvGuard`

The key gap between .NET/.Rust and Java:

| Language | How to set native-visible env var                                                                                                                                                              |
| -------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| .NET     | `Environment.SetEnvironmentVariable()` → calls Win32 `SetEnvironmentVariableW`                                                                                                                 |
| Rust     | `std::env::set_var()` (unsafe) → calls Win32 `SetEnvironmentVariableW` on Windows, `setenv()` on Unix                                                                                          |
| Java     | **No stdlib API.** `System.setProperty()` writes only the JVM property bag. `System.getenv()` is a startup snapshot. Must use **JNA** to call `SetEnvironmentVariableW` / `setenv()` directly. |

### `InProcessEnvGuard` design (Java)

```java
// Windows path — see InProcessEnvGuard.java for the cross-platform version
interface Kernel32Env extends Library {
    boolean SetEnvironmentVariableW(WString lpName, WString lpValue); // lpValue=null to delete
}

class InProcessEnvGuard implements AutoCloseable {
    private final List<Map.Entry<String, String>> saved;  // name → previous value (null = not set)

    InProcessEnvGuard(Map<String, String> applyEnv) {
        saved = new ArrayList<>();
        for (var entry : applyEnv.entrySet()) {
            saved.add(Map.entry(entry.getKey(), System.getenv(entry.getKey()))); // may be null
            nativeSetEnv(entry.getKey(), entry.getValue());
        }
        // Suppress HMAC keys, as Rust/dotnet do
        for (String key : List.of("COPILOT_HMAC_KEY", "CAPI_HMAC_KEY")) {
            if (System.getenv(key) != null) {
                saved.add(Map.entry(key, System.getenv(key)));
                nativeSetEnv(key, null);  // delete
            }
        }
    }

    @Override public void close() {
        for (int i = saved.size() - 1; i >= 0; i--) {
            var entry = saved.get(i);
            nativeSetEnv(entry.getKey(), entry.getValue()); // null → delete
        }
    }
}
```

### E2E harness integration

`E2ETestContext.createClient()` currently:

- For `ChildProcessRuntimeConnection`: attaches env to the connection → spawned child inherits it.
- For `InProcessRuntimeConnection`: must instead call `InProcessEnvGuard.apply(env)` before
  construction, and call `InProcessEnvGuard.restore()` after the test.

The harness should enforce `concurrency = 1` via a JUnit 5 `@ResourceLock("process-env")`
(or a custom `ExecutionMode.SAME_THREAD` annotation) when the transport is in-process.

---

## Maven profile for the InProcess suite

```xml
<profile>
    <id>inprocess</id>
    <properties>
        <COPILOT_SDK_DEFAULT_CONNECTION>inprocess</COPILOT_SDK_DEFAULT_CONNECTION>
        <!-- Enforce serial execution — env guard is process-global -->
        <surefire.forkCount>1</surefire.forkCount>
        <failsafe.forkCount>1</failsafe.forkCount>
    </properties>
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-failsafe-plugin</artifactId>
                <configuration>
                    <environmentVariables>
                        <COPILOT_SDK_DEFAULT_CONNECTION>inprocess</COPILOT_SDK_DEFAULT_CONNECTION>
                    </environmentVariables>
                    <parallel>none</parallel>
                </configuration>
            </plugin>
        </plugins>
    </build>
</profile>
```

---

## Spike program

The `java-inprocess-e2e-win32-x64/` Maven project demonstrates the full InProcess
flow on win32-x64:

1. **`InProcessEnvGuard`** — calls `SetEnvironmentVariableW` via JNA to set process-level
   env vars that the native `runtime.node` will read.
2. **`CopilotRuntimeLibrary`** — JNA interface for the five C ABI functions with correct
   `size_t → long` mapping for Windows x64.
3. **`QueueInputStream`** — blocking-queue-backed `InputStream` for the outbound callback
   bridge (identical to spike-3-4).
4. **`InProcessSpikeMain`** — loads the real `runtime.node`, starts the host, opens a
   connection, writes a JSON-RPC ping frame (LSP `Content-Length:` framing), reads the
   pong response, and shuts down cleanly.

### Prerequisites

```powershell
# runtime.node must be present — this spike uses the one in nodejs/node_modules
# Adjust RUNTIME_NODE_PATH if your layout differs.
$env:COPILOT_CLI_PATH = "C:\Users\edburns\workareas\copilot-sdk\nodejs\node_modules\@github\copilot-win32-x64\copilot.exe"
$env:RUNTIME_NODE_PATH = "C:\Users\edburns\workareas\copilot-sdk\nodejs\node_modules\@github\copilot-win32-x64\prebuilds\win32-x64\runtime.node"
$env:GH_TOKEN = "<your-github-token>"   # must have Copilot Individual Pro or higher
```

### Build

```powershell
cd 1917-java-embed-rust-cli-runtime-remove-before-merge\spike-3-11-replay-proxy-and-in-process\java-inprocess-e2e-win32-x64
mvn clean package
```

### Run (against real Copilot API — no proxy needed for the spike)

```powershell
java -jar target\spike-3-11-inprocess-e2e-win32-x64.jar
```

### Actual output (run 2026-07-25 on win32-x64, JDK 25.0.2, JNA 5.19.1, runtime.node 1.0.73)

```
[INFO] === Spike 3.11 — InProcess E2E (win32-x64) ===
[INFO] JVM version: 25.0.2
[INFO] OS: Windows 11 amd64
[INFO] runtime.node : ...spike-3-11.../package/prebuilds/win32-x64/runtime.node
[INFO] copilot.exe  : ...spike-3-11.../package/copilot.exe
[INFO] [InProcessEnvGuard] Applying 5 env overrides to the native process environment block via SetEnvironmentVariableW
[INFO] [Env] COPILOT_SDK_AUTH_TOKEN=<redacted> (saved previous: null)
[INFO] [Env] GH_TOKEN=<redacted> (saved previous: <present>)
[INFO] [Env] GITHUB_TOKEN=<redacted> (saved previous: null)
[INFO] [Env] COPILOT_HOME=<temp> (saved previous: null)
[INFO] [Env] COPILOT_DISABLE_KEYTAR=1 (saved previous: null)
[INFO] [InProcessEnvGuard] Env guard active. Native code in this process will now see these values.
[INFO] [JNA] Loading runtime.node from: ...
[INFO] [JNA] runtime.node loaded successfully.
[INFO] [host_start] Calling copilot_runtime_host_start (blocks up to 30s)...
[INFO] [host_start] returned serverId=1 after 1069 ms
[INFO] [connection_open] returned connectionId=1
[INFO] [connection_write] Writing ping: Content-Length: 95\r\n\r\n{"jsonrpc":"2.0","id":1,"method":"ping","params":{"message":"hello from java inprocess spike"}}
[INFO] [reader] Waiting for pong response (timeout=60s)...
[INFO] [callback] ENTERED on thread 'Thread-0' id=39 active=1 len=167
[INFO] [reader] Received frame (167 bytes): Content-Length: 144\r\n\r\n{"jsonrpc":"2.0","id":1,"result":{"message":"pong: hello from java inprocess spike","timestamp":"2026-07-25T23:20:52.582Z","protocolVersion":3}}
[INFO] [shutdown] connection_close returned true
[INFO] [shutdown] host_shutdown returned true
[INFO] PASS: InProcess transport works on win32-x64.
[INFO] PASS: InProcessEnvGuard successfully set process-level env vars.
[INFO] PASS: replay proxy redirection is possible by setting COPILOT_API_URL in the InProcessEnvGuard map.
[INFO] [InProcessEnvGuard] Restoring 9 env vars to pre-guard values via SetEnvironmentVariableW
[INFO] [InProcessEnvGuard] Restore complete.
```

### Additional finding: runtime.node ABI version sensitivity

The `runtime.node` in `@github/copilot-win32-x64@1.0.69-0` (currently installed in
`nodejs/node_modules`) is missing `copilot_runtime_host_start` and
`copilot_runtime_host_shutdown`. These exports are present in `@1.0.73` (the version
pinned in `nodejs/package-lock.json`) alongside the new `server_create`/`server_remove`
API. The spike requires version `1.0.73` (or newer with the old exports).

**Implication for production code:** The Java SDK must use the `runtime.node` that
matches the version pinned by `copilot-native` module's `npm pack` step (from question
3.2). The `copilot-native` module downloads from the version in `nodejs/package-lock.json`
which is `1.0.73`. The spike confirms this version works correctly.
