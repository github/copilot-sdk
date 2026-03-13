package jsonrpc2

import (
	"io"
	"sync"
	"testing"
	"time"
)

func TestOnCloseCalledOnUnexpectedExit(t *testing.T) {
	stdinR, stdinW := io.Pipe()
	stdoutR, stdoutW := io.Pipe()
	defer stdinR.Close()

	client := NewClient(stdinW, stdoutR)

	var called bool
	var mu sync.Mutex
	client.SetOnClose(func() {
		mu.Lock()
		called = true
		mu.Unlock()
	})

	client.Start()

	// Simulate unexpected process death by closing the stdout writer
	stdoutW.Close()

	// Wait for readLoop to detect the close and invoke the callback
	time.Sleep(200 * time.Millisecond)

	mu.Lock()
	defer mu.Unlock()
	if !called {
		t.Error("expected onClose to be called when read loop exits unexpectedly")
	}
}

func TestOnCloseNotCalledOnIntentionalStop(t *testing.T) {
	stdinR, stdinW := io.Pipe()
	stdoutR, stdoutW := io.Pipe()
	defer stdinR.Close()
	defer stdoutW.Close()

	client := NewClient(stdinW, stdoutR)

	var called bool
	var mu sync.Mutex
	client.SetOnClose(func() {
		mu.Lock()
		called = true
		mu.Unlock()
	})

	client.Start()

	// Intentional stop — should set running=false before closing stdout,
	// so the readLoop should NOT invoke onClose.
	client.Stop()

	time.Sleep(200 * time.Millisecond)

	mu.Lock()
	defer mu.Unlock()
	if called {
		t.Error("onClose should not be called on intentional Stop()")
	}
}
