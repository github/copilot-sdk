//go:build copilot_inprocess && (darwin || linux || windows)

package testharness

import (
	"log"
	"time"

	"github.com/github/copilot-sdk/go/internal/ffihost"
)

func waitForInProcessCleanup() error {
	return waitForDeferredCleanup(ffihost.WaitForCleanup, func() {
		log.Print("deferred in-process cleanup still pending; the Go test watchdog owns a stuck cleanup")
	})
}

func waitForDeferredCleanup(wait func(time.Duration) bool, reportPending func()) error {
	const interval = 10 * time.Second
	// Observe completion without retrying disposal or moving on to tear down
	// the proxy and directories while a deferred native callback still uses them.
	for !wait(interval) {
		reportPending()
	}
	return nil
}

// PrepareForProcessWait repairs signal handlers before the harness stops or
// waits for a child process while an in-process runtime is active.
func PrepareForProcessWait() {
	ffihost.PrepareForChildProcessWait()
}

// ProtectProcessWait keeps signal handlers compatible while a child exits.
func ProtectProcessWait() func() {
	return ffihost.ProtectChildProcessWait()
}
