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

func TestHostRearmsSignalHandlersAroundNativeOperations(t *testing.T) {
	for _, entrypoint := range []string{"", "copilot"} {
		t.Run("entrypoint="+entrypoint, func(t *testing.T) {
			testHostRearmsSignalHandlers(t, entrypoint)
		})
	}
}

func testHostRearmsSignalHandlers(t *testing.T, entrypoint string) {
	t.Helper()
	signals := make(chan os.Signal, 1)
	signal.Notify(signals, syscall.SIGUSR1)
	defer signal.Stop(signals)

	var original linuxSigaction
	if !linuxGetSigaction(int(syscall.SIGUSR1), &original) {
		t.Fatal("failed to read SIGUSR1 action")
	}
	defer linuxSetSigaction(int(syscall.SIGUSR1), &original)

	host := &Host{
		cliEntrypoint: entrypoint,
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
			connectionWrite: func(uint32, unsafe.Pointer, uintptr) bool {
				assertSignalHandlerOnStack(t, "connection write")
				return true
			},
			connectionClose: func(uint32) bool {
				assertSignalHandlerOnStack(t, "connection close")
				return true
			},
			hostShutdown: func(uint32) bool {
				assertSignalHandlerOnStack(t, "host shutdown")
				return true
			},
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

	withoutOnStack := original
	withoutOnStack.flags &^= linuxSaOnStack
	if !linuxSetSigaction(int(syscall.SIGUSR1), &withoutOnStack) {
		t.Fatal("failed to clear SA_ONSTACK before connection write")
	}
	if _, err := host.writeFrame([]byte("request")); err != nil {
		t.Fatal(err)
	}

	if !linuxSetSigaction(int(syscall.SIGUSR1), &withoutOnStack) {
		t.Fatal("failed to clear SA_ONSTACK before disposal")
	}
	host.Dispose()
}

func assertSignalHandlerOnStack(t *testing.T, operation string) {
	t.Helper()
	var action linuxSigaction
	if !linuxGetSigaction(int(syscall.SIGUSR1), &action) {
		t.Fatalf("failed to read SIGUSR1 action before %s", operation)
	}
	if action.flags&linuxSaOnStack == 0 {
		t.Fatalf("SA_ONSTACK was not restored before %s", operation)
	}
}
