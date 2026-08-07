# Spike — Validate Callback Flow with Real runtime.node

**Question:** Why does `copilot_runtime_host_start` hang (or return 0) when
called from the Java SDK's InProcess transport during E2E tests?

**Context:** In the shepherd-task run for PR #2272 (issue #2271), CCA's
60-minute session budget expired while validating its work. The Phase 2 local
validation also hung on `AskUserTest.testShouldReceiveChoicesInUserInputRequest`
— the `main` thread was blocked on `CompletableFuture.get()` waiting for the
`FfiRuntimeHost` to start, while the `copilot-ffi-host-start` thread was stuck
inside the JNA native call to `copilot_runtime_host_start`.

The Rust side (`embedded_host.rs`) has **zero logging**, and the Java side
(`FfiRuntimeHost.runHostStartOnBlockingThread`) calls `future.get()` with **no
timeout**. This spike isolates the exact native call with full instrumentation.

## What this spike does

1. **Downloads the real `runtime.node`** using the same `fetch-native.mjs` from
   the `copilot-native` module (pinned version from `nodejs/package-lock.json`).
2. **Loads `runtime.node` via JNA** and calls the real C ABI entry points.
3. **Adds diagnostic logging** with timestamps and thread IDs before/after every
   native call.
4. **Adds a 60-second timeout** on `host_start` (the Rust side has a 30 s
   `READY_TIMEOUT` internally, so 60 s gives headroom).
5. **Dumps relevant threads** on timeout to diagnose where the hang occurs.

## Prerequisites

- JDK 17+
- Maven 3.9+
- Node.js and npm (for fetching `runtime.node`)
- Must be run from within the `copilot-sdk` monorepo (needs
  `nodejs/package-lock.json` for version pinning)

## Build

```sh
cd spike-post-agentic-01-test-parity-validate-callback-flow-with-real-runtime_node
mvn clean package
```

This downloads `runtime.node` during `generate-resources` and produces an
executable uber-jar.

## Run

```sh
java -jar target/real-runtime-callback-spike-0.1.0.jar
```

Or with an explicit runtime.node path:

```sh
java -jar target/real-runtime-callback-spike-0.1.0.jar /path/to/runtime.node
```

## Expected outcomes

### Happy path (host_start succeeds)
```
[STEP 3] host_start completed in <N> ms, serverHandle=<non-zero>
[STEP 4] connection_open returned connHandle=<non-zero>
```

### Timeout (host_start hangs)
```
[STEP 3] TIMEOUT after 60000 ms waiting for host_start!
--- Thread dump (relevant threads) ---
```
The thread dump will show where `copilot_runtime_host_start` is stuck:
- If `spike-host-start` is in JNA's `invokeInt` → the native call hasn't returned
- This maps to the Rust `embedded_host::start()` function which:
  1. Spawns a child via `spawn_and_serve_background(command)`
  2. Waits on a `Condvar` for up to 30 s (`READY_TIMEOUT`)
  3. The child must call `notify_ready(server_id)` to unblock

### Failure (host_start returns 0)
```
[STEP 3] host_start returned 0 (failure)
```
This means the Rust side explicitly returned 0. Possible causes:
- argv_json parse failure
- Child process spawn failure
- 30 s readiness timeout elapsed (child never called `notify_ready`)

## Relationship to spike-3-4

This spike is structurally based on spike-3-4 (JNA callback threading) but
replaces the toy Rust DLL with the real `runtime.node` binary. The callback
instrumentation pattern (AtomicInteger tracking, thread-ID logging) is carried
over from spike-3-4.
