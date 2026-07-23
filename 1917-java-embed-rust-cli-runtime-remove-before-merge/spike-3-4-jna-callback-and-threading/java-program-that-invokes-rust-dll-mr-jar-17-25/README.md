# Multi-Release JAR Spike: JNA (JDK 17) vs FFM (JDK 25)

Demonstrates the multi-release JAR pattern for native callback bridging,
matching the Copilot SDK's existing `InternalExecutorProvider` approach.

## Prerequisites

- JDK 25 (to build — compiles both baseline and overlay)
- Maven 3.9+
- The Rust test DLL (see `../rust-dll/README.md`)

## Build the Rust DLL first

```sh
cd ../rust-dll
cargo build
```

## Build

```sh
mvn package
```

On JDK 25+, the `java25-multi-release` profile activates automatically and
compiles `src/main/java25/` into `META-INF/versions/25/`.

## Run on JDK 25 (FFM path)

```sh
java --enable-native-access=ALL-UNNAMED ^
     -Djava.library.path=../rust-dll/target/debug ^
     -jar target/jna-callback-mrjar-spike-0.1.0.jar 5
```

Logs will show `[JDK-25/FFM]` — upcall stub executes directly on the native
thread, no new Java thread per callback.

## Run on JDK 17 (JNA path)

```sh
path\to\jdk17\bin\java ^
     -Djna.library.path=../rust-dll/target/debug ^
     -jar target/jna-callback-mrjar-spike-0.1.0.jar 5
```

Logs will show `[JDK-17/JNA]` — each callback creates a new JNA-managed thread.

## What it demonstrates

The **same JAR** produces different behavior depending on the JVM version:

| JVM    | Class loaded                       | Callback mechanism | Thread behavior                |
| ------ | ---------------------------------- | ------------------ | ------------------------------ |
| JDK 17 | `NativeBindingProvider` (baseline) | JNA `Callback`     | New Java thread per invocation |
| JDK 25 | `NativeBindingProvider` (overlay)  | FFM upcall stub    | Executes on native thread      |
