package e2e

import (
	"os"
	"testing"

	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
)

func TestMain(m *testing.M) {
	os.Exit(testharness.RunWithInProcessIsolation(m))
}
