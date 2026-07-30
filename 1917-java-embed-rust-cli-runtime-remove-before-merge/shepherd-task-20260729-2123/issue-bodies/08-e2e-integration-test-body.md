## Overview

Create a Failsafe integration test that exercises the InProcess FFI transport end-to-end with a real `runtime.node` binary: client connects, creates a session, sends a message, and receives a response — all via the InProcess transport through `FfiRuntimeHost`.

**This is task 4.8 of 9 in the implementation plan.** Tasks are assigned, completed, and merged serially in this listed order. Tasks 4.1–4.7 are complete on the base branch before this task begins.

**Branch:** `edburns/1917-java-embed-rust-cli-runtime-dd-3039145` on `upstream`

## Plan and supporting resources

On the `edburns/1917-java-embed-rust-cli-runtime-dd-3039145` branch, the directory `1917-java-embed-rust-cli-runtime-remove-before-merge` contains the plan (`1917-embed-cli-runtime-ignorance-reduction-plan.md`) and supporting resources (spikes, prototypes, diagrams).

**Read the entire plan before working.**

## Relevant plan sections to carefully re-read

- **Section 3.11 — E2E testing with InProcess transport** — Resolution: Read the full evidence in `1917-java-embed-rust-cli-runtime-remove-before-merge/spike-3-11-replay-proxy-and-in-process/`. Key answers:
  1. **Replay proxy works with InProcess** — the replay proxy intercepts HTTP calls to `COPILOT_API_URL`. The runtime reads `COPILOT_API_URL` from the native process environment block.
  2. **Use real `runtime.node` binary** — from the `copilot-native` module (task 4.7).
  3. **No mock native library for E2E** — only unit tests use mocks.
  4. **Reuse existing YAML snapshots** — HTTP traffic is identical regardless of transport.
  5. **Run full E2E suite under both transports** — subprocess (existing job A) and InProcess (new job B with `-Pinprocess` Maven profile).
- **`InProcessEnvGuard`** requirement — calls `SetEnvironmentVariableW` (Windows) or `setenv()` (Linux/macOS) via JNA to mutate process environment before `host_start`. Restores on `close()`. See `spike-3-11/java-inprocess-e2e-win32-x64/` for proven implementation.
- **Concurrency must be 1** — `InProcessEnvGuard` mutates process-global state. Use `surefire.forkCount=1` or JUnit 5 `@ResourceLock`.
- **`@SkipInProcess` annotation** — JUnit 5 condition annotation for tests incompatible with InProcess transport (e.g., per-client environment variables — see issue #1934).
- **Section 3.12 — CI/CD workflow changes** — Resolution: New `java-sdk-inprocess` job in `java-sdk-tests.yml`. Activates `-Pinprocess` Maven profile. Scope: `ubuntu-latest` (linux-x64) only.
- **Section 4.8 — E2E integration test** (the primary task description).
- **Hard scope invariant:** Linux-x64 only. No other platform E2E tests in this phase.

## Resolved decisions that constrain this task

- **Maven profile for InProcess:** `-Pinprocess` profile with properties:
  - `COPILOT_SDK_DEFAULT_CONNECTION=inprocess`
  - `forkCount=1`, `parallel=none` in `maven-failsafe-plugin`
- **`InProcessEnvGuard`** — Must be created in `com.github.copilot.ffi` (or test harness package). Calls native env-setting functions via JNA. Implements `AutoCloseable`.
- **`E2ETestContext.createClient()` dispatch** — When `COPILOT_SDK_DEFAULT_CONNECTION=inprocess`, apply `InProcessEnvGuard` before starting client. Close in `@AfterEach` / try-with-resources.
- **Test reuse** — Existing YAML snapshots work. The spike confirmed a successful ping-pong round trip via InProcess transport in 1.1s.
- **ABI version sensitivity** — Version `1.0.73`+ from `nodejs/package-lock.json` is required (has `copilot_runtime_host_start`).

## Deliverables

### Files to create

1. **`java/sdk/src/test/java/com/github/copilot/e2e/InProcessTransportIT.java`** — Failsafe IT that:
   - Starts the replay proxy (via `E2ETestContext` / `CapiProxy`).
   - Uses `InProcessEnvGuard` to set `COPILOT_API_URL` in the native process environment.
   - Creates a `CopilotClient` with `RuntimeConnection.forInProcess()`.
   - Exercises: connect → create session → send message → receive response → disconnect.
   - Verifies the response is received correctly.
   - Cleans up: client close, env guard restore, proxy shutdown.

2. **`java/sdk/src/test/java/com/github/copilot/ffi/InProcessEnvGuard.java`** (or in test harness package) — Utility that:
   - Saves current env values for specified keys.
   - Calls `SetEnvironmentVariableW` (Windows) or `setenv()` (Linux/macOS) via JNA.
   - Implements `AutoCloseable` — restores saved values on `close()`.
   - Uses JNA for cross-platform native env manipulation.

3. **`java/sdk/src/test/java/com/github/copilot/e2e/SkipInProcess.java`** (optional) — JUnit 5 `@DisabledIf` condition annotation for tests incompatible with InProcess transport.

### Files to modify

4. **`java/sdk/pom.xml`** — Add the `-Pinprocess` Maven profile:
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

5. **Snapshot files** — Reuse existing snapshots or create new ones as needed for the InProcess smoke test.

## Gating tests and criteria

1. **InProcess E2E passes:** `InProcessTransportIT` passes on `ubuntu-latest` with the real `runtime.node` binary on classpath.
2. **Full lifecycle verified:** Client connects, creates session, sends message, receives response — all via InProcess FFI transport.
3. **Env guard works:** `InProcessEnvGuard` correctly sets and restores native process environment variables.
4. **Profile activation:** `mvn verify -Pinprocess` runs the InProcess E2E tests with `forkCount=1`.
5. **Standard E2E unaffected:** `mvn verify` (without `-Pinprocess`) runs existing subprocess E2E tests unchanged.
6. **All prior tests pass:** `mvn verify` from `java/` passes.
7. **Spotless compliance:** `mvn spotless:check` passes.

## Out of scope

- Running the full E2E suite under InProcess transport (this task creates the smoke test; running the full suite under both transports is a CI concern handled in task 4.9).
- Testing on any platform other than `linux-x64`.
- CI workflow changes (task 4.9).
