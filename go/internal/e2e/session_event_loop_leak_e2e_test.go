package e2e

import (
	"runtime"
	"testing"
	"time"

	copilot "github.com/github/copilot-sdk/go"
	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
)

// TestSessionEventLoopLeakE2E is a regression test for the goroutine leak fixed
// alongside PR #2360: CreateSession/ResumeSession construct a *Session and start
// its event-dispatch goroutine eagerly, before the server confirms the session,
// so the CLI can route session-scoped requests to it while session.create (or
// session.resume) is still being processed. Every failure path must stop that
// goroutine — otherwise each failed call leaks one goroutine forever, since no
// caller ever receives the failed session to Disconnect() it. This test fails
// without the fix and passes with it.
func TestSessionEventLoopLeakE2E(t *testing.T) {
	ctx := testharness.NewTestContext(t)

	t.Run("CreateSession failure does not leak the event loop goroutine", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		// Redirect the CLI's GitHub API calls at the replaying proxy so an
		// invalid per-session token makes the real CLI reject session.create
		// with a genuine RPC error (401 Unauthorized), exercising the same
		// failure path a real user would hit — not a mocked transport.
		client := ctx.NewClient(func(opts *copilot.ClientOptions) {
			conn := opts.Connection.(copilot.StdioConnection)
			conn.Env = append(conn.Env, "COPILOT_DEBUG_GITHUB_API_URL="+ctx.ProxyURL)
			opts.Connection = conn
		})
		t.Cleanup(func() { client.ForceStop() })

		createFailing := func() {
			if _, err := client.CreateSession(t.Context(), &copilot.SessionConfig{
				OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
				GitHubToken:         "invalid-token",
			}); err == nil {
				t.Fatal("expected CreateSession to fail with an invalid token")
			}
		}

		// Warm up: the first call spawns the CLI subprocess and its steady-state
		// goroutines (read loop, etc.), which must not be counted as leaks.
		createFailing()

		assertNoGoroutineLeak(t, 20, createFailing)
	})

	t.Run("ResumeSession failure does not leak the event loop goroutine", func(t *testing.T) {
		ctx.ConfigureForTest(t)

		client := ctx.NewClient()
		t.Cleanup(func() { client.ForceStop() })

		resumeNonExistent := func() {
			if _, err := client.ResumeSession(t.Context(), "non-existent-leak-check-session", &copilot.ResumeSessionConfig{
				OnPermissionRequest: copilot.PermissionHandler.ApproveAll,
			}); err == nil {
				t.Fatal("expected ResumeSession for a non-existent session to fail")
			}
		}

		// Warm up: the first call spawns the CLI subprocess and its steady-state
		// goroutines (read loop, etc.), which must not be counted as leaks.
		resumeNonExistent()

		assertNoGoroutineLeak(t, 20, resumeNonExistent)
	})
}

// assertNoGoroutineLeak runs fn n times and fails if the goroutine count grows
// roughly in proportion to n afterward, which would indicate one goroutine
// leaked per call rather than transient goroutines that already exited.
func assertNoGoroutineLeak(t *testing.T, n int, fn func()) {
	t.Helper()
	runtime.GC()
	before := runtime.NumGoroutine()

	for i := 0; i < n; i++ {
		fn()
	}

	// Give any goroutines that exit promptly (but not synchronously with the
	// call returning) a brief window to actually terminate before measuring.
	deadline := time.Now().Add(2 * time.Second)
	var after int
	for {
		runtime.GC()
		after = runtime.NumGoroutine()
		if after <= before+3 || time.Now().After(deadline) {
			break
		}
		time.Sleep(50 * time.Millisecond)
	}

	t.Logf("goroutines before=%d after=%d (n=%d)", before, after, n)
	if after > before+3 {
		t.Fatalf("goroutine count grew from %d to %d after %d failed calls; suspected event-loop leak", before, after, n)
	}
}
