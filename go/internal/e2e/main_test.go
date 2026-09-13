package e2e

import (
	"os"
	"testing"

	"github.com/github/copilot-sdk/go/internal/testdiagnostics"
)

func TestMain(m *testing.M) {
	os.Exit(testdiagnostics.Run(m.Run))
}
