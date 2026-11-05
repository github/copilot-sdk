package e2e

import (
	"fmt"
	"os"
	"strings"
	"testing"

	"github.com/github/copilot-sdk/go/internal/e2e/testharness"
)

func TestMain(m *testing.M) {
	if strings.EqualFold(os.Getenv("COPILOT_SDK_DEFAULT_CONNECTION"), "inprocess") && os.Getenv("COPILOT_CLI_PATH") == "" {
		path := testharness.CLIPath()
		if path == "" {
			fmt.Fprintln(os.Stderr, "Could not locate the in-process E2E runtime")
			os.Exit(1)
		}
		// Configure discovery once, before parallel fixtures capture their environments.
		if err := os.Setenv("COPILOT_CLI_PATH", path); err != nil {
			fmt.Fprintln(os.Stderr, "Could not configure the in-process E2E runtime:", err)
			os.Exit(1)
		}
	}
	os.Exit(m.Run())
}
