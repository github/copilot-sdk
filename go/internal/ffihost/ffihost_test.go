//go:build copilot_inprocess && (darwin || linux || windows)

package ffihost

import (
	"encoding/json"
	"sync/atomic"
	"testing"
	"time"
	"unsafe"
)

func TestDisposeUnregistersOutboundTarget(t *testing.T) {
	token := uintptr(nextOutboundToken.Add(1))
	host := &Host{
		recv:          newReceiveBuffer(),
		callbackToken: token,
	}
	outboundTargets.Store(token, host)

	host.Dispose()

	if _, ok := outboundTargets.Load(token); ok {
		t.Fatal("Expected disposed host to be removed from outbound callback registry")
	}
}

func TestBuildArgvAppendsManagedOptions(t *testing.T) {
	host := &Host{
		cliEntrypoint: "copilot",
		args:          []string{"--log-level", "debug", "--remote"},
	}

	var argv []string
	if err := json.Unmarshal(host.buildArgv(), &argv); err != nil {
		t.Fatal(err)
	}

	expected := []string{"copilot", "--embedded-host", "--no-auto-update", "--log-level", "debug", "--remote"}
	if len(argv) != len(expected) {
		t.Fatalf("Expected %d arguments, got %d: %v", len(expected), len(argv), argv)
	}
	for i := range expected {
		if argv[i] != expected[i] {
			t.Fatalf("Expected argument %d to be %q, got %q", i, expected[i], argv[i])
		}
	}
}

func TestDisposeWaitsForStartBeforeShuttingDown(t *testing.T) {
	started := make(chan struct{})
	releaseStart := make(chan struct{})
	startDone := make(chan error, 1)
	disposeDone := make(chan struct{})
	var shutdownID atomic.Uint32

	host := &Host{
		lib: &ffiLibrary{
			hostStart: func(_ unsafe.Pointer, _ uintptr, _ unsafe.Pointer, _ uintptr) uint32 {
				close(started)
				<-releaseStart
				return 41
			},
			hostShutdown: func(serverID uint32) bool {
				shutdownID.Store(serverID)
				return true
			},
			connectionOpen: func(_ uint32, _ uintptr, _ uintptr, _ unsafe.Pointer, _ uintptr, _ unsafe.Pointer, _ uintptr, _ unsafe.Pointer, _ uintptr) uint32 {
				return 42
			},
			connectionClose: func(_ uint32) bool { return true },
		},
		recv: newReceiveBuffer(),
	}

	go func() { startDone <- host.Start() }()
	<-started
	go func() {
		host.Dispose()
		close(disposeDone)
	}()

	select {
	case <-disposeDone:
		t.Fatal("Dispose returned before native startup completed")
	case <-time.After(20 * time.Millisecond):
	}

	close(releaseStart)
	if err := <-startDone; err != nil {
		t.Fatal(err)
	}
	<-disposeDone

	if got := shutdownID.Load(); got != 41 {
		t.Fatalf("Expected shutdown of server 41, got %d", got)
	}
}
