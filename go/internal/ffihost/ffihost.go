// Package ffihost hosts the Copilot runtime in-process by loading the native
// runtime library (runtime.node — a Rust cdylib) and driving JSON-RPC over its
// C ABI (FFI) instead of spawning the CLI as a child process and talking over
// stdio/TCP.
//
// The native copilot_runtime_host_start export spawns the residual CLI worker
// itself (`<entrypoint> --embedded-host --no-auto-update`), so the SDK never
// launches the worker directly; it only pumps opaque LSP `Content-Length:`-framed
// JSON-RPC bytes across the boundary:
//
//   - client → server frames go to copilot_runtime_connection_write
//   - server → client frames arrive on a native callback that feeds a
//     thread-safe receive buffer read by the JSON-RPC client
//
// The existing internal/jsonrpc2 client handles framing unchanged — this is a
// transport swap, not a new protocol. Host exposes an io.WriteCloser (client →
// server) and io.ReadCloser (server → client) that plug straight into
// jsonrpc2.NewClient.
//
// The C ABI (shared with the .NET, Node.js, Python, and Rust SDKs):
//
//	uint32 copilot_runtime_host_start(uint8 *argv, size_t argv_len,
//	                                  uint8 *env, size_t env_len);
//	bool   copilot_runtime_host_shutdown(uint32 server_id);
//	uint32 copilot_runtime_connection_open(uint32 server_id, outbound cb,
//	                                       void *user_data,
//	                                       uint8 *a, size_t a_len,
//	                                       uint8 *b, size_t b_len,
//	                                       uint8 *c, size_t c_len);
//	bool   copilot_runtime_connection_write(uint32 conn_id, uint8 *bytes, size_t len);
//	bool   copilot_runtime_connection_close(uint32 conn_id);
//	// outbound callback:
//	void   outbound(void *user_data, uint8 *bytes, size_t len);
//
// The native binding uses github.com/ebitengine/purego so the library is loaded
// at runtime with CGO disabled, preserving the SDK's pure-Go build and
// cross-compilation.
package ffihost

import (
	"encoding/json"
	"fmt"
	"io"
	"runtime"
	"strings"
	"sync"
	"unsafe"

	"github.com/ebitengine/purego"
)

const symbolPrefix = "copilot_runtime_"

// ffiLibrary binds the copilot_runtime_* C ABI exports of a loaded cdylib.
type ffiLibrary struct {
	hostStart       func(argv unsafe.Pointer, argvLen uintptr, env unsafe.Pointer, envLen uintptr) uint32
	hostShutdown    func(serverID uint32) bool
	connectionOpen  func(serverID uint32, cb uintptr, userData uintptr, a unsafe.Pointer, aLen uintptr, b unsafe.Pointer, bLen uintptr, c unsafe.Pointer, cLen uintptr) uint32
	connectionWrite func(connID uint32, bytes unsafe.Pointer, length uintptr) bool
	connectionClose func(connID uint32) bool
}

// The cdylib may only be loaded once per process; a second load of a different
// path is unsupported (matches the .NET/Node/Python/Rust hosts). Guard it here.
var (
	loadMu            sync.Mutex
	loadedLibrary     *ffiLibrary
	loadedLibraryPath string
)

func loadLibrary(libraryPath string) (lib *ffiLibrary, err error) {
	loadMu.Lock()
	defer loadMu.Unlock()

	if loadedLibrary != nil {
		if loadedLibraryPath != libraryPath {
			return nil, fmt.Errorf(
				"an in-process FFI runtime library is already loaded from %q; loading a different library from %q in the same process is not supported",
				loadedLibraryPath, libraryPath)
		}
		return loadedLibrary, nil
	}

	handle, err := openLibrary(libraryPath)
	if err != nil {
		return nil, fmt.Errorf("loading FFI runtime library %q: %w", libraryPath, err)
	}

	// RegisterLibFunc panics if a symbol is missing; convert that to an error so
	// callers get a clean failure instead of a crash.
	defer func() {
		if r := recover(); r != nil {
			err = fmt.Errorf("binding FFI runtime library %q: %v", libraryPath, r)
			lib = nil
		}
	}()

	bound := &ffiLibrary{}
	purego.RegisterLibFunc(&bound.hostStart, handle, symbolPrefix+"host_start")
	purego.RegisterLibFunc(&bound.hostShutdown, handle, symbolPrefix+"host_shutdown")
	purego.RegisterLibFunc(&bound.connectionOpen, handle, symbolPrefix+"connection_open")
	purego.RegisterLibFunc(&bound.connectionWrite, handle, symbolPrefix+"connection_write")
	purego.RegisterLibFunc(&bound.connectionClose, handle, symbolPrefix+"connection_close")

	loadedLibrary = bound
	loadedLibraryPath = libraryPath
	return bound, nil
}

// Host hosts the Copilot runtime in-process via its native C ABI.
//
// Construct with Create, call Start to spawn the worker and open the FFI
// connection, wire Writer/Reader into jsonrpc2.NewClient, and call Dispose to
// tear everything down.
type Host struct {
	libraryPath   string
	cliEntrypoint string
	environment   map[string]string
	extraArgs     []string
	lib           *ffiLibrary

	serverID     uint32
	connectionID uint32

	recv *receiveBuffer

	disposeMu sync.Mutex
	disposed  bool

	// callbackHandle is the purego callback trampoline address. purego keeps the
	// underlying Go closure alive for the process lifetime, so it (and this Host,
	// which the closure captures) must not be relied upon to be freed.
	callbackHandle uintptr
	// Serializes teardown against in-flight native callbacks so the receive
	// buffer is not fed after it is closed.
	callbackMu      sync.Mutex
	activeCallbacks int
}

// Create resolves the cdylib next to the CLI entrypoint and prepares the host.
// It does not spawn the worker; call Start for that. environment may be nil.
func Create(cliEntrypoint string, environment map[string]string, extraArgs []string) (*Host, error) {
	libraryPath, err := ResolveLibraryPath(cliEntrypoint)
	if err != nil {
		return nil, err
	}
	lib, err := loadLibrary(libraryPath)
	if err != nil {
		return nil, err
	}
	return &Host{
		libraryPath:   libraryPath,
		cliEntrypoint: cliEntrypoint,
		environment:   environment,
		extraArgs:     append([]string(nil), extraArgs...),
		lib:           lib,
		recv:          newReceiveBuffer(),
	}, nil
}

// Start spawns the worker and opens the FFI connection. It blocks until the
// worker connects back and signals readiness (up to ~30s), so callers should
// run it off any latency-sensitive goroutine.
func (h *Host) Start() error {
	argv := h.buildArgv()
	env := h.buildEnv()

	var argvPtr, envPtr unsafe.Pointer
	if len(argv) > 0 {
		argvPtr = unsafe.Pointer(&argv[0])
	}
	if len(env) > 0 {
		envPtr = unsafe.Pointer(&env[0])
	}

	h.serverID = h.lib.hostStart(argvPtr, uintptr(len(argv)), envPtr, uintptr(len(env)))
	// Keep the JSON buffers alive across the (synchronous) native call.
	runtime.KeepAlive(argv)
	runtime.KeepAlive(env)
	if h.serverID == 0 {
		return fmt.Errorf("copilot_runtime_host_start failed (library %q, entrypoint %q)", h.libraryPath, h.cliEntrypoint)
	}

	// host_start spawned the worker child via libuv's uv_spawn, which installs a
	// SIGCHLD handler without SA_ONSTACK on its first call. On macOS the Go runtime
	// aborts ("non-Go code set up signal handler without SA_ONSTACK flag") when it
	// later reaps one of its own os/exec children, so re-add SA_ONSTACK to that
	// foreign handler now that it exists (no-op off Darwin, and before the first
	// spawn there is nothing to fix — hence here rather than at library load).
	rearmForeignSignalHandlers()

	h.callbackHandle = purego.NewCallback(h.onOutbound)
	h.connectionID = h.lib.connectionOpen(h.serverID, h.callbackHandle, 0, nil, 0, nil, 0, nil, 0)
	if h.connectionID == 0 {
		h.lib.hostShutdown(h.serverID)
		h.serverID = 0
		return fmt.Errorf("copilot_runtime_connection_open failed")
	}
	return nil
}

// Writer returns the client → server frame sink (plug into jsonrpc2 as stdin).
func (h *Host) Writer() io.WriteCloser { return hostWriter{h} }

// Reader returns the server → client frame source (plug into jsonrpc2 as stdout).
func (h *Host) Reader() io.ReadCloser { return h.recv }

func (h *Host) buildArgv() []byte {
	// A `.js` entrypoint (dev) is launched via node; the packaged single-file CLI
	// embeds its own Node and is invoked directly. `--no-auto-update` pins the
	// worker to the runtime package matching the loaded cdylib (avoids ABI skew).
	var argv []string
	if strings.HasSuffix(strings.ToLower(h.cliEntrypoint), ".js") {
		argv = []string{"node", h.cliEntrypoint, "--embedded-host", "--no-auto-update"}
	} else {
		argv = []string{h.cliEntrypoint, "--embedded-host", "--no-auto-update"}
	}
	argv = append(argv, h.extraArgs...)
	b, _ := json.Marshal(argv)
	return b
}

func (h *Host) buildEnv() []byte {
	if len(h.environment) == 0 {
		return nil
	}
	b, _ := json.Marshal(h.environment)
	return b
}

// onOutbound is the native server → client callback, invoked on a foreign
// runtime thread. The native pointer is only valid for this call, so the bytes
// are copied out before returning. Nothing may panic across the FFI boundary.
func (h *Host) onOutbound(userData uintptr, bytesPtr uintptr, bytesLen uintptr) uintptr {
	_ = userData
	h.callbackMu.Lock()
	if h.disposed {
		h.callbackMu.Unlock()
		return 0
	}
	h.activeCallbacks++
	h.callbackMu.Unlock()

	defer func() {
		h.callbackMu.Lock()
		h.activeCallbacks--
		h.callbackMu.Unlock()
		// Never let a panic unwind into native code.
		_ = recover()
	}()

	if bytesPtr != 0 && bytesLen > 0 {
		// The native runtime delivers the outbound frame as a raw buffer address
		// (uintptr) plus length. Materialize a slice over it just long enough to
		// copy the bytes into Go-owned memory before returning to native code.
		//nolint:govet // FFI callback receives the buffer address as an integer; converting it to a pointer to copy out is the intended, checked-length use.
		src := unsafe.Slice((*byte)(unsafe.Pointer(bytesPtr)), int(bytesLen))
		buf := make([]byte, len(src))
		copy(buf, src)
		h.recv.feed(buf)
	}
	return 0
}

func (h *Host) writeFrame(frame []byte) (int, error) {
	h.disposeMu.Lock()
	closed := h.disposed || h.connectionID == 0
	connID := h.connectionID
	h.disposeMu.Unlock()
	if closed {
		return 0, fmt.Errorf("the in-process runtime connection is closed")
	}
	if len(frame) == 0 {
		return 0, nil
	}
	ok := h.lib.connectionWrite(connID, unsafe.Pointer(&frame[0]), uintptr(len(frame)))
	runtime.KeepAlive(frame)
	if !ok {
		return 0, fmt.Errorf("failed to write a frame to the in-process runtime connection")
	}
	return len(frame), nil
}

// Dispose closes the FFI connection, shuts down the native host, and releases
// resources. It is idempotent and waits for any in-flight outbound callback to
// finish before closing the receive buffer.
func (h *Host) Dispose() {
	h.disposeMu.Lock()
	if h.disposed {
		h.disposeMu.Unlock()
		return
	}
	h.disposed = true
	connID := h.connectionID
	serverID := h.serverID
	h.connectionID = 0
	h.serverID = 0
	h.disposeMu.Unlock()

	// Stop accepting new callbacks and wait for in-flight ones to drain before
	// closing the receive buffer they feed.
	for {
		h.callbackMu.Lock()
		if h.activeCallbacks == 0 {
			h.callbackMu.Unlock()
			break
		}
		h.callbackMu.Unlock()
		runtime.Gosched()
	}

	if connID != 0 {
		h.lib.connectionClose(connID)
	}
	if serverID != 0 {
		h.lib.hostShutdown(serverID)
	}
	h.recv.Close()
}

// hostWriter adapts Host into the io.WriteCloser jsonrpc2 writes request frames to.
type hostWriter struct{ h *Host }

func (w hostWriter) Write(p []byte) (int, error) { return w.h.writeFrame(p) }

func (w hostWriter) Close() error {
	w.h.Dispose()
	return nil
}
