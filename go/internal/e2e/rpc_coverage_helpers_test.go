package e2e

import (
	"os"
	"path/filepath"
	"runtime"
	"strings"
	"testing"
	"time"

	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
)

func rpcPtr[T any](value T) *T {
	return &value
}

func createUniqueRPCWorkDirectory(t *testing.T, ctx *testharness.TestContext, prefix string) string {
	t.Helper()
	dir := filepath.Join(ctx.WorkDir, prefix+"-"+randomHex(t))
	if err := os.MkdirAll(dir, 0755); err != nil {
		t.Fatalf("Failed to create %q: %v", dir, err)
	}
	return dir
}

func rpcPathsEqual(expected, actual string) bool {
	expected = filepath.Clean(expected)
	actual = filepath.Clean(actual)
	if runtime.GOOS == "windows" {
		return strings.EqualFold(expected, actual)
	}
	return expected == actual
}

func assertRPCPathEqual(t *testing.T, expected, actual string) {
	t.Helper()
	if !rpcPathsEqual(expected, actual) {
		t.Fatalf("Expected path %q to equal %q", actual, expected)
	}
}

func assertRPCContainsPath(t *testing.T, paths []string, expected string) {
	t.Helper()
	for _, path := range paths {
		if rpcPathsEqual(expected, path) {
			return
		}
	}
	t.Fatalf("Expected paths to contain %q, got %v", expected, paths)
}

func waitForRPCCondition(t *testing.T, timeout time.Duration, description string, condition func() (bool, error)) {
	t.Helper()
	deadline := time.Now().Add(timeout)
	var lastErr error
	for time.Now().Before(deadline) {
		ok, err := condition()
		if err == nil && ok {
			return
		}
		if err != nil {
			lastErr = err
		}
		time.Sleep(100 * time.Millisecond)
	}
	if lastErr != nil {
		t.Fatalf("Timed out waiting for %s: %v", description, lastErr)
	}
	t.Fatalf("Timed out waiting for %s", description)
}
