package e2e

import (
	"testing"
	"time"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
)

// TestInProcessFfiE2E is a smoke test for the in-process (FFI) transport. It
// starts a client that loads the native runtime cdylib directly, lets the native
// host construct the server, performs a purely local
// "ping" round-trip through the runtime, and stops cleanly. No auth or replay
// proxy is involved, so it needs no snapshot.
//
// Mirrors python/e2e/test_inprocess_ffi_e2e.py and
// nodejs/test/e2e/inprocess_ffi.e2e.test.ts.
func TestInProcessFfiE2E(t *testing.T) {
	// Loading the native runtime cdylib (libnode) into this test process installs
	// foreign signal handlers. On macOS the Go runtime then aborts when it reaps
	// its own os/exec children (see ffihost signal re-arming). The in-process
	// matrix cell already loads libnode for the whole suite and re-arms those
	// handlers; the default (child-process) cell must never load it, so restrict
	// this dedicated FFI smoke test to the in-process cell.
	if !testharness.IsInProcessTransport() {
		t.Skip("in-process FFI smoke test runs only under the inprocess transport cell")
	}

	cliPath := testharness.CLIPath()
	if cliPath == "" {
		t.Fatal("CLI not found. Run 'npm install' in the nodejs directory first.")
	}
	t.Setenv("COPILOT_CLI_PATH", cliPath)

	t.Run("should start and connect over in-process FFI", func(t *testing.T) {
		client := copilot.NewClient(&copilot.ClientOptions{
			Connection: copilot.InProcessConnection{},
		})
		t.Cleanup(func() { client.ForceStop() })

		if err := client.Start(t.Context()); err != nil {
			t.Fatalf("Failed to start client over in-process FFI: %v", err)
		}

		pong, err := client.Ping(t.Context(), "ffi message")
		if err != nil {
			t.Fatalf("Failed to ping: %v", err)
		}

		if pong.Message != "pong: ffi message" {
			t.Errorf("Expected pong.message to be 'pong: ffi message', got %q", pong.Message)
		}

		if pong.Timestamp.IsZero() {
			t.Errorf("Expected non-zero pong.timestamp, got %s", pong.Timestamp)
		}

		if err := client.Stop(); err != nil {
			t.Errorf("Expected no errors on stop, got %v", err)
		}
	})

	t.Run("should force stop over in-process FFI within a bounded time", func(t *testing.T) {
		// Regression test for github/copilot-sdk#2525: the in-process FFI
		// host's Dispose used to call the native host_shutdown export
		// in-line with no timeout. A slow or stuck native shutdown (observed
		// on Windows, closing the runtime's SQLite session store) would hang
		// ForceStop indefinitely, even though ForceStop is documented as the
		// bounded recovery path for exactly a hung/slow Stop. Asserts that
		// ForceStop returns within a generous bound instead of hanging.
		client := copilot.NewClient(&copilot.ClientOptions{
			Connection: copilot.InProcessConnection{},
		})

		if err := client.Start(t.Context()); err != nil {
			t.Fatalf("Failed to start client over in-process FFI: %v", err)
		}
		if _, err := client.Ping(t.Context(), "hello before force stop"); err != nil {
			t.Fatalf("Failed to ping: %v", err)
		}

		done := make(chan struct{})
		go func() {
			client.ForceStop()
			close(done)
		}()

		select {
		case <-done:
		case <-time.After(20 * time.Second):
			t.Fatal("ForceStop did not complete within a bounded time")
		}
	})
}
