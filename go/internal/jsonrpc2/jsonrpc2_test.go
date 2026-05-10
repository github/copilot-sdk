package jsonrpc2

import (
	"errors"
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

// TestSetProcessDone_ErrorAvailableImmediately validates that getProcessError()
// returns the correct error immediately after processDone is closed.
// The current implementation stores a pointer to the process error
// synchronously when the processDone channel is closed, so callers should
// never observe a nil error after the channel has been closed.
func TestSetProcessDone_ErrorAvailableImmediately(t *testing.T) {
	misses := 0
	const iterations = 1000

	for i := 0; i < iterations; i++ {
		stdinR, stdinW := io.Pipe()
		stdoutR, stdoutW := io.Pipe()

		client := NewClient(stdinW, stdoutR)

		done := make(chan struct{})
		processErr := errors.New("CLI process exited: exit status 1")

		client.SetProcessDone(done, &processErr)

		// Simulate process exit: error is already set, close the channel.
		close(done)

		// Do NOT yield to the scheduler — check immediately.
		// In the current code the goroutine inside SetProcessDone may not
		// have copied the error to client.processError yet.
		if err := client.getProcessError(); err == nil {
			misses++
		}

		stdinR.Close()
		stdinW.Close()
		stdoutR.Close()
		stdoutW.Close()
	}

	if misses > 0 {
		t.Errorf("SetProcessDone regression: getProcessError() returned nil %d/%d times "+
			"immediately after processDone was closed, even though the error pointer "+
			"should be stored synchronously.", misses, iterations)
	}
}

// TestSetProcessDone_RequestMissesProcessError validates that the Request()
// method returns the specific process error instead of the generic
// "process exited unexpectedly" message once processDone has been closed.
func TestSetProcessDone_RequestMissesProcessError(t *testing.T) {
	misses := 0
	const iterations = 100

	for i := 0; i < iterations; i++ {
		stdinR, stdinW := io.Pipe()
		stdoutR, stdoutW := io.Pipe()

		client := NewClient(stdinW, stdoutR)
		client.Start()

		done := make(chan struct{})
		processErr := errors.New("CLI process exited: authentication failed")

		client.SetProcessDone(done, &processErr)

		// Simulate process exit.
		close(done)
		// Close the writer so the readLoop can exit.
		stdoutW.Close()

		// Make a request — should get the specific process error.
		_, err := client.Request("test.method", nil)
		if err != nil && err.Error() == "process exited unexpectedly" {
			misses++
		}

		client.Stop()
		stdinR.Close()
		stdinW.Close()
		stdoutR.Close()
	}

	if misses > 0 {
		t.Errorf("Request() bug: returned generic 'process exited unexpectedly' %d/%d times "+
			"instead of the actual process error after process exit; the process "+
			"error was not correctly propagated from SetProcessDone.", misses, iterations)
	}
}

// TestSetProcessDone_ErrorAvailableImmediately verifies that the process error
// is available as soon as the done channel is closed, matching the
// pointer-based implementation where no asynchronous copy is required.
func TestSetProcessDone_ErrorCopiedEventually(t *testing.T) {
	stdinR, stdinW := io.Pipe()
	stdoutR, stdoutW := io.Pipe()
	defer stdinR.Close()
	defer stdinW.Close()
	defer stdoutR.Close()
	defer stdoutW.Close()

	client := NewClient(stdinW, stdoutR)

	done := make(chan struct{})
	processErr := errors.New("CLI process exited: version mismatch")

	client.SetProcessDone(done, &processErr)

	// Close the channel: the process error should now be observable immediately,
	// without needing to yield to another goroutine.
	close(done)

	err := client.getProcessError()
	if err == nil {
		t.Fatal("expected process error to be available immediately after done is closed, got nil")
	}
	if err.Error() != processErr.Error() {
		t.Errorf("expected %q, got %q", processErr.Error(), err.Error())
	}
}
