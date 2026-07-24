# Spike 3.9 — C ABI Parameter Semantics

**Date:** 2026-07-24
**Method:** Source code review of all five SDK implementations (Rust, .NET, Node.js, Go, Python) that call the same C ABI, plus the Rust SDK's `ffi.rs` which binds the C ABI types directly.

---

## Summary

This spike answers all 11 open questions from section 3.9 by examining how every existing SDK implementation constructs parameters for each of the 5 C ABI functions. The answers below are derived entirely from production code — not from speculation.

---

## Complete Call-by-Call Parameter Reference

### `copilot_runtime_host_start(argv_json, argv_json_len, env_json, env_json_len)`

#### Parameter 1–2: `argv_json` / `argv_json_len`

**Format:** A UTF-8 encoded JSON array of strings. The byte array is the JSON text; `argv_json_len` is the byte length of that text.

**Content:** The array always starts with the entrypoint path, then `--embedded-host`, then `--no-auto-update`, then any additional CLI arguments:

- **For a packaged binary entrypoint** (no `.js` extension):
  ```json
  ["/full/path/to/copilot", "--embedded-host", "--no-auto-update", ...extra_args]
  ```
- **For a `.js` dev entrypoint** (extension is `.js`):
  ```json
  ["node", "/full/path/to/index.js", "--embedded-host", "--no-auto-update", ...extra_args]
  ```

**Required flags:**

- `--embedded-host` — **Always required.** Tells the runtime it is being hosted in-process (the worker child connects back to the host rather than listening on a port).
- `--no-auto-update` — **Always required.** Pins the spawned worker to the bundled version, preventing ABI skew if the user's `~/.copilot/pkg` contains a newer version.

**Optional additional arguments** (appended after the two required flags):

| Argument                                  | When appended                                                                                                                             | Source     |
| ----------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------- | ---------- |
| `--log-level <level>`                     | When `options.logLevel` is set (e.g., `"debug"`, `"info"`, `"warn"`, `"error"`)                                                           | All 5 SDKs |
| `--auth-token-env COPILOT_SDK_AUTH_TOKEN` | When `options.githubToken` is provided. Tells the runtime to read the auth token from the named env var (which is passed via `env_json`). | All 5 SDKs |
| `--no-auto-login`                         | When `useLoggedInUser` is false (default when `githubToken` is provided)                                                                  | All 5 SDKs |
| `--session-idle-timeout <seconds>`        | When `sessionIdleTimeoutSeconds > 0`                                                                                                      | All 5 SDKs |
| `--remote`                                | When `enableRemoteSessions` is true                                                                                                       | All 5 SDKs |

**Nullability:** `argv_json` must NOT be null — it always contains at least the entrypoint path and the two required flags. A null or empty argv would be a programming error (host_start would return 0).

#### Parameter 3–4: `env_json` / `env_json_len`

**Format:** A UTF-8 encoded JSON object (`{"key": "value", ...}`) or null pointer with length 0.

**Content — the complete key inventory:**

| Key                      | Value                                         | When set                                                           |
| ------------------------ | --------------------------------------------- | ------------------------------------------------------------------ |
| `COPILOT_SDK_AUTH_TOKEN` | The GitHub personal access token or app token | When `options.githubToken` is provided                             |
| `COPILOT_HOME`           | Path to the Copilot home/base directory       | When `options.baseDirectory` is set                                |
| `COPILOT_DISABLE_KEYTAR` | `"1"`                                         | When `options.mode == "empty"` (disables keytar credential access) |

These are the **only** three keys used by any of the five SDK implementations. No proxy URLs, no log level (that's an argv flag), no other keys.

**Nullability:** CAN be passed as a null pointer with length 0. All SDKs check "if environment is empty, pass null/0." The Rust SDK returns `None`, the .NET SDK returns `null`, the Node.js SDK passes `null, 0`, the Go SDK passes `nil, 0`, the Python SDK passes `None, 0`.

#### Return value

- Non-zero `uint32_t` — a server handle (server ID) used in subsequent calls.
- Zero — failure. **There is no companion error-retrieval function.** No `copilot_runtime_last_error` export exists in the C ABI. The only diagnostic is internal logging by the runtime. All SDKs format an error message from the library path and entrypoint path when host_start returns 0.

---

### `copilot_runtime_connection_open(server_id, on_outbound, user_data, ext_source, ext_source_len, ext_name, ext_name_len, conn_token, conn_token_len)`

#### Parameter 1: `server_id`

The server handle returned by `host_start`. Must be non-zero.

#### Parameter 2: `on_outbound`

A function pointer with signature `void(void* user_data, const uint8_t* data, size_t len)`. This is the callback the native runtime invokes to deliver server→client JSON-RPC frames back to the host.

**Threading:** The callback is invoked on native/foreign threads (not the calling thread). All SDKs handle this:

- Rust: Tokio `mpsc::UnboundedSender` is `Send`
- .NET: `Channel<byte[]>` is thread-safe
- Go: mutex-protected `receiveBuffer`
- Python: `threading.Condition`-protected `_ReceiveBuffer`
- Node.js: koffi delivers via a threadsafe function to the event loop

**Buffer lifetime:** The `data` pointer is only valid for the duration of the callback invocation. All SDKs copy the bytes before returning from the callback.

#### Parameter 3: `user_data`

A `void*` cookie passed back as the first argument of every `on_outbound` invocation. Used for routing in languages that need it.

**Values used by each SDK:**

| SDK           | `user_data` value                                                     | Why                                                                              |
| ------------- | --------------------------------------------------------------------- | -------------------------------------------------------------------------------- |
| Rust          | Pointer to `Box<CallbackState>` (contains `mpsc::Sender`)             | Routes frames to the channel                                                     |
| .NET (modern) | `GCHandle.ToIntPtr(this)` — a handle to the `FfiRuntimeHost` instance | `OnOutboundStatic` recovers `this` via `GCHandle.FromIntPtr`                     |
| .NET (legacy) | `IntPtr.Zero` (null)                                                  | Uses an instance delegate that captures `this` directly                          |
| Go            | A `uintptr` token (monotonic counter) stored in a `sync.Map`          | `routeOutbound` looks up the `*Host` by token                                    |
| Python        | `None` (null)                                                         | Uses a bound method (`self._on_outbound`) as the callback, which captures `self` |
| Node.js       | `null`                                                                | koffi registered callback captures `this` via closure                            |

**Conclusion for Java:** Passing `null` for `user_data` is safe with the real runtime — the runtime simply passes whatever value you give back to the callback unmodified. The Java implementation should pass `Pointer.NULL` and rely on Java closure/field capture for routing, exactly as Python, .NET-legacy, and Node.js do.

#### Parameters 4–9: `ext_source`, `ext_source_len`, `ext_name`, `ext_name_len`, `conn_token`, `conn_token_len`

**Every single SDK implementation passes null/0 for all three:**

| SDK           | ext_source            | ext_name              | conn_token            |
| ------------- | --------------------- | --------------------- | --------------------- |
| Rust          | `std::ptr::null(), 0` | `std::ptr::null(), 0` | `std::ptr::null(), 0` |
| .NET (modern) | `null, UIntPtr.Zero`  | `null, UIntPtr.Zero`  | `null, UIntPtr.Zero`  |
| .NET (legacy) | `null, UIntPtr.Zero`  | `null, UIntPtr.Zero`  | `null, UIntPtr.Zero`  |
| Go            | `nil, 0`              | `nil, 0`              | `nil, 0`              |
| Python        | `None, 0`             | `None, 0`             | `None, 0`             |
| Node.js       | `null, 0`             | `null, 0`             | `null, 0`             |

**Semantics:** These are reserved/future extension points in the C ABI. The plan table notes their semantics are "under investigation in Q3.9." Based on the naming and position:

- `ext_source` — likely an extension/plugin source identifier (e.g., a VS Code extension ID)
- `ext_name` — likely a human-readable name for the extension
- `conn_token` — likely a per-connection authentication token

**But none of these are used in any production SDK today.** The Java implementation must pass `null, 0` for all three, exactly as every other SDK does.

#### Return value

- Non-zero `uint32_t` — a connection handle (connection ID) used in `connection_write` and `connection_close`.
- Zero — failure. No error message retrieval; SDKs format their own error string. On failure, all SDKs immediately call `host_shutdown(server_id)` to clean up.

#### Multiple concurrent connections

The handle-per-connection design implies multiple connections are architecturally possible, but **every SDK implementation opens exactly one connection per server handle.** The pattern is always: `host_start` → one `connection_open` → use → `connection_close` → `host_shutdown`. The Java implementation should follow this same pattern: one connection per server, no concurrent connections.

---

### `copilot_runtime_connection_write(connection_id, data, len)`

#### Parameter 1: `connection_id`

The connection handle returned by `connection_open`.

#### Parameter 2–3: `data` / `len`

Raw bytes to send to the runtime. These are **LSP `Content-Length:`-framed JSON-RPC messages** — the same wire format used on stdio transport.

**Frame format (Question 10):** The framing is **LSP `Content-Length:` header-based**, NOT length-prefixed binary. The format is:

```
Content-Length: <n>\r\n\r\n<n bytes of JSON-RPC payload>
```

This is explicitly stated in multiple places:

- Rust `ffi.rs` line 9–10: "The framing is unchanged — the same LSP `Content-Length:` frames the stdio transport uses."
- Go `ffihost.go` line 6: "It pumps opaque LSP Content-Length-framed JSON-RPC bytes across the boundary"
- Python `_ffi_runtime_host.py` line 8: "the SDK never launches the worker directly; it only pumps opaque LSP `Content-Length:`-framed JSON-RPC bytes"
- Node.js `ffiRuntimeHost.ts` line 14: "LSP `Content-Length:`-framed JSON-RPC bytes are pumped across the ABI"

The existing `JsonRpcClient` in the Java SDK already handles this framing. No special frame encoding/decoding is needed at the FFI boundary — just pass the raw bytes through.

**Buffer lifetime (Question 11):** The native side copies the buffer synchronously before returning. Confirmed by:

- .NET `FfiRuntimeHost.cs` line ~206: "The bytes are read synchronously by the native side (it copies before returning), so the span does not need to outlive the call"
- Plan table: "Native side copies the buffer synchronously before returning."
- Rust `ffi.rs` `FfiShared` comment: "the bound exports are plain fn pointers — the native side copies buffers synchronously"
- Go: uses `runtime.KeepAlive(frame)` as defense-in-depth, but the call is synchronous

**Conclusion for Java:** The byte array passed to `connection_write` does not need to be kept alive after the JNA call returns. The native side copies it during the synchronous call.

#### Return value

- `true` — success (frame was enqueued for delivery to the runtime worker)
- `false` — failure (connection is closed or invalid)

---

### `copilot_runtime_connection_close(connection_id)`

#### Parameter: `connection_id`

The connection handle to close. After this call, the handle is invalid and must not be reused.

#### Return value

- `true` — success
- `false` — failure (already closed or invalid handle)

**Ordering constraint:** All SDKs call `connection_close` BEFORE `host_shutdown`. The Rust SDK also drains active callbacks (waits for `active_callbacks` AtomicUsize to reach 0) after setting a `closing` flag but before freeing the callback state.

---

### `copilot_runtime_host_shutdown(server_id)`

#### Parameter: `server_id`

The server handle to shut down. After this call, the handle is invalid.

#### Return value

- `true` — success
- `false` — failure (already shut down or invalid)

**Ordering:** Always called AFTER `connection_close`. The shutdown sequence across all SDKs is:

1. Set a "closing" flag to reject new callbacks
2. `connection_close(connection_id)`
3. Wait for active callbacks to drain (Rust/Python/Go do this explicitly)
4. `host_shutdown(server_id)`
5. Free callback state / release GC references

---

## Answers to the 11 Original Questions

| #   | Question                                          | Answer                                                                                                                                                                           |
| --- | ------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 1   | What is the full set of valid arguments for argv? | `[entrypoint, "--embedded-host", "--no-auto-update"]` plus optional `--log-level`, `--auth-token-env`, `--no-auto-login`, `--session-idle-timeout`, `--remote`. See table above. |
| 2   | What are the valid env_json keys?                 | Exactly three: `COPILOT_SDK_AUTH_TOKEN`, `COPILOT_HOME`, `COPILOT_DISABLE_KEYTAR`. Complete inventory above.                                                                     |
| 3   | Nullability of argv/env buffers?                  | `argv_json` must NOT be null (always has content). `env_json` CAN be null with len=0.                                                                                            |
| 4   | Error retrieval on host_start failure?            | No companion error function exists. Return 0 = failure; only diagnostic is runtime-internal logging.                                                                             |
| 5   | `ext_source` semantics?                           | Reserved/future. All SDKs pass null. Java must pass null.                                                                                                                        |
| 6   | `ext_name` semantics?                             | Reserved/future. All SDKs pass null. Java must pass null.                                                                                                                        |
| 7   | `conn_token` semantics?                           | Reserved/future. All SDKs pass null. Java must pass null. Not related to global auth token.                                                                                      |
| 8   | `user_data = null` safe?                          | Yes, confirmed by .NET-legacy, Python, and Node.js all passing null. The runtime passes it back unmodified. Java should pass null and use closure capture.                       |
| 9   | Multiple concurrent connections?                  | Architecturally possible but unused. All SDKs open exactly one connection per server. Java should do the same.                                                                   |
| 10  | Wire frame format?                                | LSP `Content-Length:\r\n\r\n` framing — identical to stdio transport. NOT binary length-prefixed.                                                                                |
| 11  | Buffer lifetime for connection_write?             | Native copies synchronously before returning. Java byte array does not need to survive past the JNA call.                                                                        |

---

## Implementation Guidance for Java

Based on the complete parameter analysis above, the Java `FfiRuntimeHost` should:

1. **Build `argv_json`** as a JSON array starting with the resolved entrypoint (prefixed with `"node"` if `.js`), followed by `"--embedded-host"`, `"--no-auto-update"`, then optional flags based on `CopilotClientOptions`.

2. **Build `env_json`** as a JSON object with only the three known keys when applicable, or pass `null` with length 0 when no environment overrides are needed.

3. **Pass `Pointer.NULL`** for `user_data` in `connection_open`, and hold the JNA `Callback` instance as a strong field reference (spike 3.4's design).

4. **Pass `null, 0`** for all three metadata buffers (`ext_source`, `ext_name`, `conn_token`) in `connection_open`.

5. **Use the existing `JsonRpcClient`** framing unchanged — the FFI boundary carries the same LSP-framed bytes as the stdio transport.

6. **Do not retain the byte array** after `connection_write` returns — native copies synchronously.

7. **Shutdown sequence:** set closing flag → `connection_close` → drain active callbacks → `host_shutdown` → release callback reference.
