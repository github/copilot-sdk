package e2e

import (
	"bytes"
	"context"
	"runtime/pprof"
	"strconv"
	"strings"
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
			opts.Env = append(opts.Env, "COPILOT_DEBUG_GITHUB_API_URL="+ctx.ProxyURL)
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

// assertNoGoroutineLeak observes only goroutines spawned by these failed calls.
func assertNoGoroutineLeak(t *testing.T, n int, fn func()) {
	t.Helper()
	label := "failed-session-" + randomHex(t)
	releaseControl := make(chan struct{})
	controlStarted := make(chan struct{})
	controlStopped := make(chan struct{})
	pprof.Do(t.Context(), pprof.Labels(label, "control"), func(context.Context) {
		go func() {
			defer close(controlStopped)
			close(controlStarted)
			<-releaseControl
		}()
	})
	<-controlStarted
	defer func() {
		close(releaseControl)
		<-controlStopped
	}()
	for i := 0; i < n; i++ {
		pprof.Do(t.Context(), pprof.Labels(label, "owned"), func(context.Context) { fn() })
	}

	deadline := time.Now().Add(2 * time.Second)
	for {
		var profile bytes.Buffer
		if err := pprof.Lookup("goroutine").WriteTo(&profile, 1); err != nil {
			t.Fatalf("Read owned goroutine profile: %v", err)
		}
		remaining := 0
		controlObserved := false
		for _, stack := range strings.Split(profile.String(), "\n\n") {
			if strings.Contains(stack, strconv.Quote(label)+":\"control\"") {
				controlObserved = true
				continue
			}
			if !strings.Contains(stack, strconv.Quote(label)+":\"owned\"") {
				continue
			}
			for _, line := range strings.Split(stack, "\n") {
				if strings.Contains(line, " @ ") {
					count, err := strconv.Atoi(strings.Fields(line)[0])
					if err != nil {
						t.Fatalf("Parse owned goroutine count in %q: %v", line, err)
					}
					remaining += count
					break
				}
			}
		}
		if !controlObserved {
			t.Fatalf("Owned control goroutine is missing from the labelled profile\n%s", profile.String())
		}
		if remaining == 0 {
			return
		}
		if !time.Now().Before(deadline) {
			t.Fatalf("%d owned goroutines remain after %d failed calls\n%s", remaining, n, profile.String())
		}
		time.Sleep(50 * time.Millisecond)
	}
}
