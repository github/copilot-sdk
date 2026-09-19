// SPDX-License-Identifier: MIT

//go:build copilot_inprocess && linux

package ffihost

import (
	"runtime"
	"syscall"
	"unsafe"
)

const (
	linuxSaOnStack = 0x08000000
	linuxSigDfl    = 0
	linuxSigIgn    = 1
	linuxSigChild  = 17
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
// installed by the native runtime that omitted it. Tokio's process-global
// SIGCHLD registration through signal-hook-registry chains Go's handler but
// replaces its flags without SA_ONSTACK. The Go runtime aborts with "non-Go code
// set up signal handler without SA_ONSTACK flag" when SIGCHLD (signal 17 on
// Linux) is delivered while a Go-managed child process is reaped.
//
// We preserve each foreign handler and merely OR in SA_ONSTACK, so Tokio's child
// watching keeps working while the Go runtime stays happy. Handlers left at
// SIG_DFL/SIG_IGN and Go's own handlers (which already carry SA_ONSTACK) are
// untouched. Best-effort: any failure is silently ignored, since the worst case
// is the pre-existing crash.
func rearmForeignSignalHandlers(_ uintptr) {
	for sig := 1; sig <= linuxMaxSignal; sig++ {
		rearmLinuxSignalHandler(sig)
	}
}

func rearmLinuxSignalHandler(sig int) {
	var action linuxSigaction
	if !linuxGetSigaction(sig, &action) {
		return
	}
	if action.handler == linuxSigDfl || action.handler == linuxSigIgn {
		return
	}
	if action.flags&linuxSaOnStack != 0 {
		return
	}
	action.flags |= linuxSaOnStack
	linuxSetSigaction(sig, &action)
}

func protectChildProcessSignalHandler() func() {
	return protectLinuxSignalHandler(linuxSigChild)
}

func protectLinuxSignalHandler(sig int) func() {
	stop := make(chan struct{})
	ready := make(chan struct{})
	stopped := make(chan struct{})
	go func() {
		defer close(stopped)
		rearmLinuxSignalHandler(sig)
		close(ready)
		for {
			select {
			case <-stop:
				return
			default:
				rearmLinuxSignalHandler(sig)
				runtime.Gosched()
			}
		}
	}()
	<-ready
	return func() {
		close(stop)
		<-stopped
		rearmLinuxSignalHandler(sig)
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
