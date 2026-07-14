//go:build copilot_inprocess && (darwin || linux || windows)

// Package ffihost hosts the Copilot runtime in-process by loading its native
// library and driving JSON-RPC over the runtime's C ABI.
//
// It pumps opaque LSP Content-Length-framed JSON-RPC bytes across the boundary:
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
	"sync/atomic"
	"unsafe"

	"github.com/ebitengine/purego"
)

const symbolPrefix = "copilot_runtime_"

// ffiLibrary binds the copilot_runtime_* C ABI exports of a loaded cdylib.
type ffiLibrary struct {
	handle          uintptr
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

var (
	outboundCallbackOnce   sync.Once
	outboundCallbackHandle uintptr
	outboundTargets        sync.Map
	nextOutboundToken      atomic.Uint64
)

func sharedOutboundCallback() uintptr {
	outboundCallbackOnce.Do(func() {
		outboundCallbackHandle = purego.NewCallback(routeOutbound)
	})
	return outboundCallbackHandle
}

func routeOutbound(userData uintptr, bytesPtr uintptr, bytesLen uintptr) uintptr {
	target, ok := outboundTargets.Load(userData)
	if !ok {
		return 0
	}
	return target.(*Host).onOutbound(bytesPtr, bytesLen)
}

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

	bound := &ffiLibrary{handle: handle}
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
// Construct with Create, call Start to open the FFI connection, wire
// Writer/Reader into jsonrpc2.NewClient, and call Dispose to tear everything
// down.
type Host struct {
	libraryPath   string
	cliEntrypoint string
	environment   map[string]string
	args          []string
	lib           *ffiLibrary

	// lifecycleMu serializes native start/write/shutdown operations. hostStart
	// cannot be interrupted, so Dispose waits for it before closing native IDs.
	lifecycleMu sync.Mutex
	// mu serializes disposal with native callbacks so the receive buffer cannot
	// be fed after it is closed.
	mu           sync.Mutex
	serverID     uint32
	connectionID uint32
	disposed     bool
	// activeCallbacks counts outbound native callbacks currently executing.
	activeCallbacks int

	recv *receiveBuffer

	callbackToken uintptr
}

// Create resolves the native library and prepares the host. environment and
// args contain SDK-managed runtime options.
func Create(cliEntrypoint string, environment map[string]string, args []string) (*Host, error) {
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
		args:          append([]string(nil), args...),
		lib:           lib,
		recv:          newReceiveBuffer(),
	}, nil
}

// Start opens the FFI connection. Native startup may block, so callers should
// run it off any latency-sensitive goroutine.
func (h *Host) Start() error {
	h.lifecycleMu.Lock()
	defer h.lifecycleMu.Unlock()

	h.mu.Lock()
	if h.disposed {
		h.mu.Unlock()
		return fmt.Errorf("the in-process runtime host is disposed")
	}
	h.mu.Unlock()

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
	// SIGCHLD handler without SA_ONSTACK on its first call. The Go runtime aborts
	// ("non-Go code set up signal handler without SA_ONSTACK flag") when it later
	// reaps one of its own os/exec children (e.g. a test-spawned MCP server) and
	// the delivered SIGCHLD lands on a non-signal stack. Re-add SA_ONSTACK to that
	// foreign handler now that it exists (implemented on darwin+linux; a no-op on
	// other platforms, and before the first spawn there is nothing to fix — hence
	// here rather than at library load).
	rearmForeignSignalHandlers(h.lib.handle)

	callbackHandle := sharedOutboundCallback()
	callbackToken := uintptr(nextOutboundToken.Add(1))
	outboundTargets.Store(callbackToken, h)
	h.callbackToken = callbackToken
	h.connectionID = h.lib.connectionOpen(h.serverID, callbackHandle, callbackToken, nil, 0, nil, 0, nil, 0)
	if h.connectionID == 0 {
		outboundTargets.Delete(callbackToken)
		h.callbackToken = 0
		h.lib.hostShutdown(h.serverID)
		rearmForeignSignalHandlers(h.lib.handle)
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
	argv = append(argv, h.args...)
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
func (h *Host) onOutbound(bytesPtr uintptr, bytesLen uintptr) uintptr {
	h.mu.Lock()
	if h.disposed {
		h.mu.Unlock()
		return 0
	}
	h.activeCallbacks++
	h.mu.Unlock()

	defer func() {
		h.mu.Lock()
		h.activeCallbacks--
		h.mu.Unlock()
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
	h.lifecycleMu.Lock()
	defer h.lifecycleMu.Unlock()

	h.mu.Lock()
	disposed := h.disposed
	h.mu.Unlock()
	connID := h.connectionID
	if disposed || connID == 0 {
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
	h.lifecycleMu.Lock()
	defer h.lifecycleMu.Unlock()

	h.mu.Lock()
	if h.disposed {
		h.mu.Unlock()
		return
	}
	// Publish disposed under the same lock onOutbound uses to check it, so no new
	// callback can pass the check and increment activeCallbacks after the drain
	// loop below observes zero.
	h.disposed = true
	connID := h.connectionID
	serverID := h.serverID
	callbackToken := h.callbackToken
	h.connectionID = 0
	h.serverID = 0
	h.callbackToken = 0
	h.mu.Unlock()

	if callbackToken != 0 {
		outboundTargets.Delete(callbackToken)
	}

	// Stop accepting new callbacks and wait for in-flight ones to drain before
	// closing the receive buffer they feed.
	for {
		h.mu.Lock()
		if h.activeCallbacks == 0 {
			h.mu.Unlock()
			break
		}
		h.mu.Unlock()
		runtime.Gosched()
	}

	if connID != 0 {
		h.lib.connectionClose(connID)
	}
	if serverID != 0 {
		h.lib.hostShutdown(serverID)
		// libuv may restore a previously saved SIGCHLD action while tearing down
		// its final child watcher, so repair the process-wide handler again after
		// shutdown before Go reaps another os/exec child.
		rearmForeignSignalHandlers(h.lib.handle)
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
