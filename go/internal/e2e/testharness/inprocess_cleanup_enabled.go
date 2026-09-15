//go:build copilot_inprocess && (darwin || linux || windows)

package testharness

import (
	"fmt"
	"time"

	"github.com/github/copilot-sdk/go/internal/ffihost"
)

func waitForInProcessCleanup() error {
	const timeout = 10 * time.Second
	if !ffihost.WaitForCleanup(timeout) {
		return fmt.Errorf("timed out after %s waiting for deferred in-process cleanup", timeout)
	}
	return nil
}
