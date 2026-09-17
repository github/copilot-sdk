//go:build !copilot_inprocess || (!darwin && !linux && !windows)

package testharness

func waitForInProcessCleanup() error {
	return nil
}

// PrepareForProcessWait is a no-op when the in-process runtime is unavailable.
func PrepareForProcessWait() {}
