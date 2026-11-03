//go:build copilot_inprocess && (darwin || linux || windows)

package testharness

import (
	"testing"
	"time"
)

func TestDeferredCleanupObservationRetainsPendingCompletion(t *testing.T) {
	for _, pending := range []int{0, 2} {
		name := "already complete"
		if pending != 0 {
			name = "completion after diagnostic intervals"
		}
		t.Run(name, func(t *testing.T) {
			observations, reports := 0, 0
			err := waitForDeferredCleanup(func(interval time.Duration) bool {
				if interval != 10*time.Second {
					t.Fatalf("cleanup observation interval = %s; want the existing 10s interval", interval)
				}
				observations++
				return observations > pending
			}, func() {
				reports++
			})
			if err != nil {
				t.Fatalf("healthy deferred cleanup failed: %v", err)
			}
			if observations != pending+1 || reports != pending {
				t.Fatalf("observations/reports = %d/%d; want %d/%d", observations, reports, pending+1, pending)
			}
		})
	}
}

func TestDeferredCleanupObservationUsesActualIdleSignal(t *testing.T) {
	if err := waitForInProcessCleanup(); err != nil {
		t.Fatalf("cleanup without outstanding native callbacks failed: %v", err)
	}
}
