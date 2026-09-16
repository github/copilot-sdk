//go:build copilot_inprocess && linux

package ffihost

import (
	"os"
	"os/signal"
	"syscall"
	"testing"
	"unsafe"
)

func TestRearmForeignSignalHandlersAddsOnStack(t *testing.T) {
	signals := make(chan os.Signal, 1)
	signal.Notify(signals, syscall.SIGUSR1)
	defer signal.Stop(signals)

	var original linuxSigaction
	if !linuxGetSigaction(int(syscall.SIGUSR1), &original) {
		t.Fatal("failed to read SIGUSR1 action")
	}
	defer linuxSetSigaction(int(syscall.SIGUSR1), &original)

	withoutOnStack := original
	withoutOnStack.flags &^= linuxSaOnStack
	if !linuxSetSigaction(int(syscall.SIGUSR1), &withoutOnStack) {
		t.Fatal("failed to clear SA_ONSTACK")
	}

	rearmForeignSignalHandlers(0)

	var rearmed linuxSigaction
	if !linuxGetSigaction(int(syscall.SIGUSR1), &rearmed) {
		t.Fatal("failed to read rearmed SIGUSR1 action")
	}
	if rearmed.flags&linuxSaOnStack == 0 {
		t.Fatal("SA_ONSTACK was not restored")
	}
}

func TestHostStartRearmsSignalHandlersAfterConnectionOpen(t *testing.T) {
	signals := make(chan os.Signal, 1)
	signal.Notify(signals, syscall.SIGUSR1)
	defer signal.Stop(signals)

	var original linuxSigaction
	if !linuxGetSigaction(int(syscall.SIGUSR1), &original) {
		t.Fatal("failed to read SIGUSR1 action")
	}
	defer linuxSetSigaction(int(syscall.SIGUSR1), &original)

	host := &Host{
		cliEntrypoint: "copilot",
		lib: &ffiLibrary{
			hostStart: func(unsafe.Pointer, uintptr, unsafe.Pointer, uintptr) uint32 {
				return 1
			},
			connectionOpen: func(uint32, uintptr, uintptr, unsafe.Pointer, uintptr, unsafe.Pointer, uintptr, unsafe.Pointer, uintptr) uint32 {
				withoutOnStack := original
				withoutOnStack.flags &^= linuxSaOnStack
				if !linuxSetSigaction(int(syscall.SIGUSR1), &withoutOnStack) {
					t.Fatal("failed to clear SA_ONSTACK during connection initialization")
				}
				return 2
			},
			connectionClose: func(uint32) bool { return true },
			hostShutdown:    func(uint32) bool { return true },
		},
		recv: newReceiveBuffer(),
	}
	if err := host.Start(); err != nil {
		t.Fatal(err)
	}
	defer host.Dispose()

	var rearmed linuxSigaction
	if !linuxGetSigaction(int(syscall.SIGUSR1), &rearmed) {
		t.Fatal("failed to read rearmed SIGUSR1 action")
	}
	if rearmed.flags&linuxSaOnStack == 0 {
		t.Fatal("SA_ONSTACK was not restored after connection initialization")
	}
}
