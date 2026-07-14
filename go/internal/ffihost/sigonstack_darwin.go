// SPDX-License-Identifier: MIT

//go:build copilot_inprocess && darwin

package ffihost

import (
	"encoding/binary"
	"unsafe"

	"github.com/ebitengine/purego"
)

// Darwin `struct sigaction` layout (16 bytes, little-endian on amd64/arm64):
//
//	offset 0:  union __sigaction_u sa_handler/sa_sigaction (8 bytes, pointer)
//	offset 8:  sigset_t            sa_mask                 (4 bytes, uint32)
//	offset 12: int                 sa_flags                (4 bytes)
const (
	darwinSigactionSize = 16
	darwinFlagsOffset   = 12
	saOnStack           = 0x0001 // SA_ONSTACK on Darwin
	sigDfl              = 0      // SIG_DFL
	sigIgn              = 1      // SIG_IGN
	maxSignal           = 31     // NSIG-1 on Darwin
)

// rearmForeignSignalHandlers re-adds the SA_ONSTACK flag to any signal handler
// installed by the native runtime (libnode/libuv, loaded via dlopen) that
// omitted it. The Go runtime aborts with "non-Go code set up signal handler
// without SA_ONSTACK flag" when such a signal (notably SIGCHLD, signal 20 on
// Darwin) is delivered while a Go-managed child process is reaped. libuv
// installs a SIGCHLD handler without SA_ONSTACK, which poisons every subsequent
// os/exec child reaped by Go in the same process (enforced by the Go runtime on
// both macOS and Linux; the Linux variant lives in sigonstack_linux.go).
//
// We preserve each foreign handler and merely OR in SA_ONSTACK, so libuv's child
// watching keeps working while the Go runtime stays happy. Handlers left at
// SIG_DFL/SIG_IGN and Go's own handlers (which already carry SA_ONSTACK) are
// untouched. Best-effort: any failure is silently ignored, since the worst case
// is the pre-existing crash.
func rearmForeignSignalHandlers(_ uintptr) {
	handle, err := purego.Dlopen("/usr/lib/libSystem.B.dylib", purego.RTLD_NOW|purego.RTLD_GLOBAL)
	if err != nil || handle == 0 {
		return
	}

	var sigaction func(sig int32, act, oact unsafe.Pointer) int32
	if !bindSigaction(handle, &sigaction) {
		return
	}

	for sig := int32(1); sig <= maxSignal; sig++ {
		var cur [darwinSigactionSize]byte
		if sigaction(sig, nil, unsafe.Pointer(&cur[0])) != 0 {
			continue
		}
		handler := binary.LittleEndian.Uint64(cur[0:8])
		if handler == sigDfl || handler == sigIgn {
			continue
		}
		flags := binary.LittleEndian.Uint32(cur[darwinFlagsOffset : darwinFlagsOffset+4])
		if flags&saOnStack != 0 {
			continue
		}
		binary.LittleEndian.PutUint32(cur[darwinFlagsOffset:darwinFlagsOffset+4], flags|saOnStack)
		sigaction(sig, unsafe.Pointer(&cur[0]), nil)
	}
}

// bindSigaction resolves libc's sigaction into fn, converting the panic
// RegisterLibFunc raises on a missing symbol into a false return.
func bindSigaction(handle uintptr, fn *func(sig int32, act, oact unsafe.Pointer) int32) (ok bool) {
	defer func() {
		if recover() != nil {
			ok = false
		}
	}()
	purego.RegisterLibFunc(fn, handle, "sigaction")
	return true
}
