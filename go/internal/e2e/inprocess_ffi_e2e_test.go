package e2e

import (
	"os"
	"testing"

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

	t.Run("keeps test environment isolated from host process", func(t *testing.T) {
		ambientHome, hadAmbientHome := os.LookupEnv("COPILOT_HOME")
		assertEnvironmentUnchanged := func() {
			t.Helper()
			if home, present := os.LookupEnv("COPILOT_HOME"); home != ambientHome || present != hadAmbientHome {
				t.Fatal("In-process test client changed the host process environment")
			}
		}

		ctx := testharness.NewTestContext(t)
		client := ctx.NewClient()
		t.Cleanup(func() { client.ForceStop() })
		assertEnvironmentUnchanged()

		if err := client.Start(t.Context()); err != nil {
			t.Fatalf("Failed to start isolated in-process client: %v", err)
		}
		assertEnvironmentUnchanged()

		if _, err := client.Ping(t.Context(), "isolated environment"); err != nil {
			t.Fatalf("Failed to ping isolated in-process client: %v", err)
		}
		if err := client.Stop(); err != nil {
			t.Fatalf("Failed to stop isolated in-process client: %v", err)
		}
		assertEnvironmentUnchanged()
	})
}
