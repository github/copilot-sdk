# Java JNA Callback Spike

Tests JNA callback invocation from a native (Rust) thread, bridging data into
a `QueueInputStream` (`BlockingQueue`-backed `InputStream`).

## Prerequisites

- JDK 17+
- Maven 3.9+
- Rust toolchain (`cargo`)

## Build the Rust DLL first

```sh
cd ../rust-dll
cargo build
```

This produces `target/debug/callback_test.dll` (Windows) or
`target/debug/libcallback_test.so` (Linux).

## Build the Java program

```sh
cd ../java-program-that-invokes-rust-dll
mvn package -q
```

## Run

```sh
java -Djna.library.path=../rust-dll/target/debug -jar target/jna-callback-spike-0.1.0.jar [burstCount]
```

`burstCount` (default 5) controls how many messages the native thread sends via
the callback.

## What it demonstrates

1. **JNA thread attachment** — each callback invocation gets a new JNA-attached
   Java thread (automatic, no manual `AttachCurrentThread` needed).
2. **PipedStream failure** — `PipedInputStream`/`PipedOutputStream` does NOT work
   because JNA's short-lived callback threads cause "Write end dead" errors.
3. **QueueInputStream success** — `BlockingQueue<byte[]>`-backed `InputStream`
   has no thread-affinity checks and works correctly from any thread.
4. **Callback GC protection** — the `OutboundCallback` is held as a strong
   reference to prevent GC (dangling native function pointer → JVM crash).
5. **Active callback tracking** — `AtomicInteger` mirrors Rust's `AtomicUsize`
   pattern for safe drain-before-shutdown.
