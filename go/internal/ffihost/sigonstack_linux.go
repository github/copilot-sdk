// SPDX-License-Identifier: MIT

//go:build copilot_inprocess && linux

package ffihost

import (
	"syscall"
	"unsafe"
)

const (
	linuxSaOnStack = 0x08000000
	linuxSigDfl    = 0
	linuxSigIgn    = 1
	linuxMaxSignal = 31
)

// linuxSigaction matches the kernel rt_sigaction ABI used by the Go runtime on
// Linux amd64 and arm64.
type linuxSigaction struct {
	handler  uintptr
	flags    uint64
	restorer uintptr
	mask     uint64
}

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
func rearmForeignSignalHandlers(_ uintptr) {
	for sig := 1; sig <= linuxMaxSignal; sig++ {
		var action linuxSigaction
		if !linuxGetSigaction(sig, &action) {
			continue
		}
		if action.handler == linuxSigDfl || action.handler == linuxSigIgn {
			continue
		}
		if action.flags&linuxSaOnStack != 0 {
			continue
		}
		action.flags |= linuxSaOnStack
		linuxSetSigaction(sig, &action)
	}
}

func linuxGetSigaction(signal int, action *linuxSigaction) bool {
	_, _, errno := syscall.RawSyscall6(
		syscall.SYS_RT_SIGACTION,
		uintptr(signal),
		0,
		uintptr(unsafe.Pointer(action)),
		unsafe.Sizeof(action.mask),
		0,
		0,
	)
	return errno == 0
}

func linuxSetSigaction(signal int, action *linuxSigaction) bool {
	_, _, errno := syscall.RawSyscall6(
		syscall.SYS_RT_SIGACTION,
		uintptr(signal),
		uintptr(unsafe.Pointer(action)),
		0,
		unsafe.Sizeof(action.mask),
		0,
		0,
	)
	return errno == 0
}
