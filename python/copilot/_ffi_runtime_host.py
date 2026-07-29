"""In-process (FFI) hosting of the Copilot runtime.

Instead of spawning the Copilot CLI as a child process and talking JSON-RPC over
stdio/TCP, the in-process transport loads the runtime's native shared library
(``runtime.node`` — a Rust ``cdylib``) into this process and drives JSON-RPC over
its C ABI (FFI). The native ``host_start`` export spawns the residual worker
itself, so the SDK never launches the worker directly; it only pumps opaque LSP
``Content-Length:``-framed JSON-RPC bytes across the boundary:

- client → server frames go to ``copilot_runtime_connection_write``
- server → client frames arrive on a native callback that feeds a thread-safe
  receive buffer

The existing :class:`~copilot._jsonrpc.JsonRpcClient` handles framing unchanged —
this is a transport swap, not a new protocol. The host exposes a *process-like*
adapter (``stdin``/``stdout``/``stderr``/``poll``) so ``JsonRpcClient`` can drive
it exactly like a :class:`subprocess.Popen`.

The C ABI (shared with the .NET, Node.js, and Rust SDKs)::

    uint32 copilot_runtime_host_start(uint8 *argv, size_t argv_len,
                                      uint8 *env, size_t env_len);
    bool   copilot_runtime_host_shutdown(uint32 server_id);
    uint32 copilot_runtime_connection_open(uint32 server_id, outbound cb,
                                           void *user_data,
                                           uint8 *a, size_t a_len,
                                           uint8 *b, size_t b_len,
                                           uint8 *c, size_t c_len);
    bool   copilot_runtime_connection_write(uint32 conn_id,
                                            uint8 *bytes, size_t len);
    bool   copilot_runtime_connection_close(uint32 conn_id);
    // outbound callback:
    void   outbound(void *user_data, uint8 *bytes, size_t len);
"""

from __future__ import annotations

import ctypes
import json
import logging
import os
import sys
import threading
import time
from collections.abc import Sequence
from pathlib import Path

logger = logging.getLogger("copilot.ffi")

_SYMBOL_PREFIX = "copilot_runtime_"

# The C ABI outbound callback: void(void *user_data, uint8 *bytes, size_t len).
_OutboundCallback = ctypes.CFUNCTYPE(
    None, ctypes.c_void_p, ctypes.POINTER(ctypes.c_uint8), ctypes.c_size_t
)


def get_prebuilds_folder() -> str | None:
    """Return the ``prebuilds/<platform>`` folder name for the current host.

    Matches the napi-rs ``<node-platform>-<arch>`` layout the runtime package
    ships (e.g. ``linux-x64``, ``darwin-arm64``, ``win32-x64``), including the
    musl (Alpine) variants. Returns ``None`` for unsupported platforms.
    """
    if sys.platform.startswith("linux"):
        platform_name = "linuxmusl" if _is_musl() else "linux"
    elif sys.platform == "darwin":
        platform_name = "darwin"
    elif sys.platform == "win32":
        platform_name = "win32"
    else:
        return None

    machine = _normalize_machine()
    if machine is None:
        return None
    return f"{platform_name}-{machine}"


def _normalize_machine() -> str | None:
    import platform

    machine = platform.machine().lower()
    if machine in ("x86_64", "amd64", "x64"):
        return "x64"
    if machine in ("arm64", "aarch64"):
        return "arm64"
    return None


def _is_musl() -> bool:
    """Detect whether the current Linux system uses musl libc (e.g. Alpine)."""
    if sys.platform != "linux":
        return False
    try:
        import subprocess

        result = subprocess.run(["ldd", "--version"], capture_output=True, text=True, timeout=5)
        return "musl" in (result.stdout + result.stderr).lower()
    except (FileNotFoundError, OSError, Exception):  # noqa: BLE001
        return False


def _natural_library_name() -> str:
    """The natural platform shared-library file name for the runtime cdylib.

    The ``.node`` file renamed to what a Rust ``cdylib`` would be called on this
    OS. The library is loaded by absolute path, so the on-disk name is ours.
    """
    if sys.platform == "win32":
        return "copilot_runtime.dll"
    if sys.platform == "darwin":
        return "libcopilot_runtime.dylib"
    return "libcopilot_runtime.so"


def resolve_library_path(cli_entrypoint: str) -> str | None:
    """Resolve the native runtime library next to the given CLI entrypoint.

    Checks, in order:

    1. The natural platform library name next to the CLI (bundled/flat layout,
       what the Python download-at-first-use path writes).
    2. ``prebuilds/<platform>/runtime.node`` next to the CLI (dev/package layout).

    Returns the absolute path, or ``None`` when neither exists.
    """
    directory = Path(cli_entrypoint).resolve().parent

    flat = directory / _natural_library_name()
    if flat.is_file():
        return str(flat)

    folder = get_prebuilds_folder()
    if folder is not None:
        prebuilt = directory / "prebuilds" / folder / "runtime.node"
        if prebuilt.is_file():
            return str(prebuilt)

    return None


# The cdylib may only be loaded once per process; a second load of a *different*
# path is unsupported (matches the Node/Rust hosts). Guard it here.
_loaded_library: ctypes.CDLL | None = None
_loaded_library_path: str | None = None
_load_lock = threading.Lock()


class _FfiLibrary:
    """Binds the ``copilot_runtime_*`` C ABI exports of a loaded cdylib."""

    def __init__(self, lib: ctypes.CDLL) -> None:
        self._lib = lib

        self.host_start = getattr(lib, f"{_SYMBOL_PREFIX}host_start")
        self.host_start.argtypes = [
            ctypes.c_char_p,
            ctypes.c_size_t,
            ctypes.c_char_p,
            ctypes.c_size_t,
        ]
        self.host_start.restype = ctypes.c_uint32

        self.host_shutdown = getattr(lib, f"{_SYMBOL_PREFIX}host_shutdown")
        self.host_shutdown.argtypes = [ctypes.c_uint32]
        self.host_shutdown.restype = ctypes.c_bool

        self.connection_open = getattr(lib, f"{_SYMBOL_PREFIX}connection_open")
        self.connection_open.argtypes = [
            ctypes.c_uint32,
            _OutboundCallback,
            ctypes.c_void_p,
            ctypes.c_char_p,
            ctypes.c_size_t,
            ctypes.c_char_p,
            ctypes.c_size_t,
            ctypes.c_char_p,
            ctypes.c_size_t,
        ]
        self.connection_open.restype = ctypes.c_uint32

        self.connection_write = getattr(lib, f"{_SYMBOL_PREFIX}connection_write")
        self.connection_write.argtypes = [
            ctypes.c_uint32,
            ctypes.c_char_p,
            ctypes.c_size_t,
        ]
        self.connection_write.restype = ctypes.c_bool

        self.connection_close = getattr(lib, f"{_SYMBOL_PREFIX}connection_close")
        self.connection_close.argtypes = [ctypes.c_uint32]
        self.connection_close.restype = ctypes.c_bool


def _load_library(library_path: str) -> _FfiLibrary:
    global _loaded_library, _loaded_library_path
    with _load_lock:
        if _loaded_library is not None:
            if _loaded_library_path != library_path:
                raise RuntimeError(
                    f"An in-process FFI runtime library is already loaded from "
                    f"'{_loaded_library_path}'; loading a different library from "
                    f"'{library_path}' in the same process is not supported."
                )
            return _FfiLibrary(_loaded_library)

        # Load with immediate binding (RTLD_NOW) on POSIX, matching the .NET/Rust
        # hosts. The runtime cdylib from the npm platform package is self-contained;
        # eager binding surfaces any load problem here rather than at first call.
        if sys.platform == "win32":
            lib = ctypes.WinDLL(library_path)
        else:
            lib = ctypes.CDLL(library_path, mode=os.RTLD_NOW | os.RTLD_LOCAL)
        _loaded_library = lib
        _loaded_library_path = library_path
        return _FfiLibrary(lib)


class _ReceiveBuffer:
    """Thread-safe byte buffer feeding blocking ``read(n)`` from a producer thread.

    The native outbound callback (invoked on a foreign runtime thread) appends
    frames via :meth:`feed` without ever blocking; the JSON-RPC reader thread
    drains them via :meth:`read`, which blocks until data or EOF.
    """

    def __init__(self) -> None:
        self._buffer = bytearray()
        self._closed = False
        self._cond = threading.Condition()

    def feed(self, data: bytes) -> None:
        with self._cond:
            if self._closed:
                return
            self._buffer.extend(data)
            self._cond.notify_all()

    def close(self) -> None:
        with self._cond:
            self._closed = True
            self._cond.notify_all()

    def read(self, size: int) -> bytes:
        if size <= 0:
            return b""
        with self._cond:
            while not self._buffer and not self._closed:
                self._cond.wait()
            if not self._buffer:
                return b""  # EOF
            chunk = bytes(self._buffer[:size])
            del self._buffer[:size]
            return chunk

    def readline(self) -> bytes:
        """Read through the next ``\\n`` (inclusive), blocking until available.

        Returns whatever remains (possibly without a trailing newline) at EOF, or
        ``b""`` if the buffer is empty and closed. Mirrors the blocking
        ``BufferedReader.readline`` semantics :class:`JsonRpcClient` expects when
        parsing LSP ``Content-Length:`` headers.
        """
        with self._cond:
            while b"\n" not in self._buffer and not self._closed:
                self._cond.wait()
            newline_index = self._buffer.find(b"\n")
            if newline_index == -1:
                # EOF with no newline: return the remaining bytes (may be empty).
                line = bytes(self._buffer)
                self._buffer.clear()
                return line
            end = newline_index + 1
            line = bytes(self._buffer[:end])
            del self._buffer[:end]
            return line


class _FfiStdin:
    """Writable side of the process-like adapter; forwards frames to the runtime."""

    def __init__(self, host: FfiRuntimeHost) -> None:
        self._host = host

    def write(self, data: bytes) -> int:
        self._host._write_frame(data)
        return len(data)

    def flush(self) -> None:
        # connection_write enqueues synchronously, so there is nothing to flush.
        pass


class _FfiProcessAdapter:
    """A ``subprocess.Popen``-shaped view over an :class:`FfiRuntimeHost`.

    :class:`~copilot._jsonrpc.JsonRpcClient` only needs ``stdin`` (writable),
    ``stdout`` (blocking ``read``), an optional ``stderr``, and ``poll()``. The
    in-process transport has no OS pipes, so this adapter bridges those to the
    FFI host's frame plumbing.
    """

    def __init__(self, host: FfiRuntimeHost) -> None:
        self._host = host
        self.stdin = _FfiStdin(host)
        self.stdout = host._receive_buffer
        # No separate error stream in-process; JsonRpcClient skips the stderr
        # thread when this is falsy.
        self.stderr = None

    def poll(self) -> int | None:
        """Return ``None`` while the connection is live, ``0`` once closed."""
        return None if not self._host._disposed else 0

    def terminate(self) -> None:
        self._host.dispose()

    def kill(self) -> None:
        self._host.dispose()

    def wait(self, timeout: float | None = None) -> int:  # noqa: ARG002
        self._host.dispose()
        return 0


class FfiRuntimeHost:
    """Hosts the Copilot runtime in-process via its native C ABI.

    Construct with :meth:`create`, then :meth:`start` to spawn the worker and open
    the FFI connection. Expose :attr:`process` to :class:`JsonRpcClient`, and call
    :meth:`dispose` to tear everything down.
    """

    def __init__(
        self,
        library_path: str,
        cli_entrypoint: str,
        environment: dict[str, str] | None = None,
        args: Sequence[str] = (),
    ) -> None:
        self._library_path = library_path
        self._cli_entrypoint = cli_entrypoint
        self._environment = environment
        self._extra_args = list(args)
        self._lib = _load_library(library_path)

        self._server_id = 0
        self._connection_id = 0
        self._disposed = False
        self._dispose_lock = threading.Lock()

        self._receive_buffer = _ReceiveBuffer()
        # Keep a strong reference to the ctypes callback for its whole lifetime;
        # dropping it while native code can still invoke it is a use-after-free.
        self._outbound_callback: ctypes._FuncPointer | None = None
        # Serializes teardown against in-flight native callbacks.
        self._active_callbacks = 0
        self._callback_lock = threading.Lock()

        self._process = _FfiProcessAdapter(self)

    @property
    def process(self) -> _FfiProcessAdapter:
        """The ``subprocess.Popen``-shaped adapter for :class:`JsonRpcClient`."""
        return self._process

    @staticmethod
    def create(
        cli_entrypoint: str,
        environment: dict[str, str] | None = None,
        args: Sequence[str] = (),
    ) -> FfiRuntimeHost:
        """Resolve the cdylib next to the CLI entrypoint and prepare the host.

        Raises:
            RuntimeError: If the native runtime library cannot be found.
        """
        full_entrypoint = str(Path(cli_entrypoint).resolve())
        library_path = resolve_library_path(full_entrypoint)
        if library_path is None:
            raise RuntimeError(
                "In-process FFI runtime library not found next to "
                f"'{full_entrypoint}'. Download it with "
                "`python -m copilot download-runtime --in-process`, or set "
                "COPILOT_CLI_PATH to a runtime package that ships it."
            )
        return FfiRuntimeHost(library_path, full_entrypoint, environment, args)

    def _build_argv(self) -> bytes:
        # A `.js` entrypoint (dev) is launched via node; the packaged single-file
        # CLI embeds its own Node and is invoked directly. `--no-auto-update`
        # pins the worker to the runtime package matching the loaded cdylib.
        if self._cli_entrypoint.lower().endswith(".js"):
            argv = ["node", self._cli_entrypoint, "--embedded-host", "--no-auto-update"]
        else:
            argv = [self._cli_entrypoint, "--embedded-host", "--no-auto-update"]
        argv.extend(self._extra_args)
        return json.dumps(argv).encode("utf-8")

    def _build_env(self) -> bytes | None:
        if not self._environment:
            return None
        obj = {k: v for k, v in self._environment.items() if v is not None}
        if not obj:
            return None
        return json.dumps(obj).encode("utf-8")

    def start_blocking(self) -> None:
        """Spawn the worker and open the FFI connection (blocks up to ~30s).

        Must be run off the event loop (e.g. via :func:`asyncio.to_thread`);
        ``host_start`` blocks until the worker connects back and signals
        readiness.
        """
        argv = self._build_argv()
        env = self._build_env()

        self._server_id = self._lib.host_start(argv, len(argv), env, len(env) if env else 0)
        if not self._server_id:
            raise RuntimeError(
                f"copilot_runtime_host_start failed (library '{self._library_path}', "
                f"entrypoint '{self._cli_entrypoint}')."
            )

        self._outbound_callback = _OutboundCallback(self._on_outbound)
        self._connection_id = self._lib.connection_open(
            self._server_id,
            self._outbound_callback,
            None,
            None,
            0,
            None,
            0,
            None,
            0,
        )
        if not self._connection_id:
            self._outbound_callback = None
            self._lib.host_shutdown(self._server_id)
            self._server_id = 0
            raise RuntimeError("copilot_runtime_connection_open failed.")

    def _on_outbound(
        self,
        _user_data: int | None,
        bytes_ptr: ctypes._Pointer,
        bytes_len: int,
    ) -> None:
        """Native server → client callback (invoked on a foreign runtime thread).

        The native pointer is only valid for this call, so the bytes are copied
        out before returning. Exceptions must not cross the FFI boundary, so
        everything is caught and logged.
        """
        with self._callback_lock:
            if self._disposed:
                return
            self._active_callbacks += 1
        try:
            if bytes_ptr and bytes_len > 0:
                data = ctypes.string_at(bytes_ptr, bytes_len)
                self._receive_buffer.feed(data)
        except Exception:  # noqa: BLE001
            logger.error("In-process FFI inbound callback failed", exc_info=True)
        finally:
            with self._callback_lock:
                self._active_callbacks -= 1

    def _write_frame(self, frame: bytes) -> None:
        if self._disposed or not self._connection_id:
            raise RuntimeError("The in-process runtime connection is closed.")
        ok = self._lib.connection_write(self._connection_id, frame, len(frame))
        if not ok:
            raise RuntimeError("Failed to write a frame to the in-process runtime connection.")

    def dispose(self) -> None:
        """Close the FFI connection, shut down the native host, release resources.

        Idempotent. Waits for any in-flight outbound callback to finish before
        dropping the callback reference to avoid a use-after-free.
        """
        with self._dispose_lock:
            if self._disposed:
                return
            self._disposed = True

        # Stop accepting new callbacks and wait for in-flight ones to drain.
        with self._callback_lock:
            pass  # _disposed is set; new callbacks bail out immediately.
        while True:
            with self._callback_lock:
                if self._active_callbacks == 0:
                    break
            time.sleep(0.001)

        try:
            if self._connection_id:
                self._lib.connection_close(self._connection_id)
                self._connection_id = 0
        except Exception:  # noqa: BLE001
            logger.debug("Error closing in-process FFI connection", exc_info=True)

        try:
            if self._server_id:
                self._lib.host_shutdown(self._server_id)
                self._server_id = 0
        except Exception:  # noqa: BLE001
            logger.debug("Error shutting down in-process FFI host", exc_info=True)

        self._receive_buffer.close()
        # Safe to drop now: no native code can invoke the callback after
        # connection_close, and all in-flight callbacks have drained.
        self._outbound_callback = None
