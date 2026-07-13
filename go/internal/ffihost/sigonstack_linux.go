// SPDX-License-Identifier: MIT

//go:build linux

package ffihost

import (
	"encoding/binary"
	"unsafe"

	"github.com/ebitengine/purego"
)

// Linux `struct sigaction` layout for glibc and musl on amd64/arm64
// (little-endian):
//
//	offset 0:   sa_handler/sa_sigaction (8 bytes, pointer)
//	offset 8:   sigset_t sa_mask        (128 bytes)
//	offset 136: int      sa_flags       (4 bytes)
//	offset 144: sa_restorer             (8 bytes)
//
// Both glibc and musl use a 128-byte sigset_t here, so sa_flags is at offset 136
// and the whole struct is 152 bytes. We over-allocate slightly for safety.
const (
	linuxSigactionSize = 160
	linuxFlagsOffset   = 136
	linuxSaOnStack     = 0x08000000 // SA_ONSTACK on Linux
	linuxSigDfl        = 0          // SIG_DFL
	linuxSigIgn        = 1          // SIG_IGN
	linuxMaxSignal     = 31         // standard signals; covers SIGCHLD (17)
)

// libcCandidates are dlopen names to try when resolving sigaction fails through
// the already-loaded runtime library handle (e.g. when its libc symbols are not
// reachable via that handle). Covers glibc and musl.
var libcCandidates = []string{"libc.so.6", "libc.so", "libc.musl-x86_64.so.1", "libc.musl-aarch64.so.1"}

// rearmForeignSignalHandlers re-adds the SA_ONSTACK flag to any signal handler
// installed by the native runtime (libnode/libuv, loaded via dlopen) that
// omitted it. The Go runtime aborts with "non-Go code set up signal handler
// without SA_ONSTACK flag" when such a signal (notably SIGCHLD, signal 17 on
// Linux) is delivered while a Go-managed child process is reaped. libuv installs
// a SIGCHLD handler without SA_ONSTACK, which poisons every subsequent os/exec
// child reaped by Go in the same process.
//
// We preserve each foreign handler and merely OR in SA_ONSTACK, so libuv's child
// watching keeps working while the Go runtime stays happy. Handlers left at
// SIG_DFL/SIG_IGN and Go's own handlers (which already carry SA_ONSTACK) are
// untouched. Best-effort: any failure is silently ignored, since the worst case
// is the pre-existing crash.
//
// runtimeHandle is the dlopen handle of the native runtime; sigaction is
// resolved through it (dlsym searches the library's libc dependency) so we avoid
// guessing the libc path, with an explicit libc dlopen as a fallback.
func rearmForeignSignalHandlers(runtimeHandle uintptr) {
	var sigaction func(sig int32, act, oact unsafe.Pointer) int32
	if !resolveLinuxSigaction(runtimeHandle, &sigaction) {
		return
	}

	for sig := int32(1); sig <= linuxMaxSignal; sig++ {
		var cur [linuxSigactionSize]byte
		if sigaction(sig, nil, unsafe.Pointer(&cur[0])) != 0 {
			continue
		}
		handler := binary.LittleEndian.Uint64(cur[0:8])
		if handler == linuxSigDfl || handler == linuxSigIgn {
			continue
		}
		flags := binary.LittleEndian.Uint32(cur[linuxFlagsOffset : linuxFlagsOffset+4])
		if flags&linuxSaOnStack != 0 {
			continue
		}
		binary.LittleEndian.PutUint32(cur[linuxFlagsOffset:linuxFlagsOffset+4], flags|linuxSaOnStack)
		sigaction(sig, unsafe.Pointer(&cur[0]), nil)
	}
}

// resolveLinuxSigaction binds libc's sigaction into fn, first via the runtime
// library handle (whose libc dependency exports it) and then via an explicit
// libc dlopen. Returns false if no candidate resolves the symbol.
func resolveLinuxSigaction(runtimeHandle uintptr, fn *func(sig int32, act, oact unsafe.Pointer) int32) bool {
	if runtimeHandle != 0 && bindLinuxSigaction(runtimeHandle, fn) {
		return true
	}
	for _, name := range libcCandidates {
		handle, err := purego.Dlopen(name, purego.RTLD_NOW|purego.RTLD_GLOBAL)
		if err != nil || handle == 0 {
			continue
		}
		if bindLinuxSigaction(handle, fn) {
			return true
		}
	}
	return false
}

// bindLinuxSigaction resolves sigaction from handle into fn, converting the
// panic RegisterLibFunc raises on a missing symbol into a false return.
func bindLinuxSigaction(handle uintptr, fn *func(sig int32, act, oact unsafe.Pointer) int32) (ok bool) {
	defer func() {
		if recover() != nil {
			ok = false
		}
	}()
	purego.RegisterLibFunc(fn, handle, "sigaction")
	return true
}
