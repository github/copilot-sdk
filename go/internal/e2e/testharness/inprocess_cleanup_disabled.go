//go:build !copilot_inprocess || (!darwin && !linux && !windows)

package testharness

func waitForInProcessCleanup() error {
	return nil
}
