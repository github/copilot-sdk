# Rust test DLL for spike 3.4

Produces a `cdylib` (`.dll` on Windows, `.so` on Linux, `.dylib` on macOS) that
simulates the Copilot runtime's C ABI entry points with heavy instrumentation.

## Build

```sh
cd rust-dll
cargo build
```

The built library will be at:

- Windows: `target/debug/callback_test.dll`
- Linux: `target/debug/libcallback_test.so`
- macOS: `target/debug/libcallback_test.dylib`

## What it does

Exports 5 `extern "C"` functions mirroring the real `runtime.node` C ABI:

| Function           | Behavior                                                                      |
| ------------------ | ----------------------------------------------------------------------------- |
| `host_start`       | Returns dummy handle `42`. Logs entry/exit.                                   |
| `host_shutdown`    | Logs and returns `true`.                                                      |
| `connection_open`  | Spawns a **new native thread** that invokes the callback `burst_count` times. |
| `connection_write` | Logs the data received from Java.                                             |
| `connection_close` | Logs and returns `true`.                                                      |

The key behavior is `connection_open`: the callback is invoked on a **different
native thread** than the caller, exactly like the real runtime. This lets us
verify JNA's automatic thread attachment and the PipedStream bridging strategy.
