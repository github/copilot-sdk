//go:build copilot_inprocess && (darwin || linux || windows)

package copilot

import (
	"path/filepath"
	"strconv"
	"strings"
	"testing"
)

func TestClient_InProcessRuntimePathCapturedAtConstruction(t *testing.T) {
	t.Setenv(defaultConnectionEnvVar, "inprocess")
	for _, tc := range []struct {
		name    string
		options *ClientOptions
	}{
		{"nil options", nil},
		{"empty options", &ClientOptions{}},
		{"explicit connection", &ClientOptions{Connection: InProcessConnection{}}},
	} {
		t.Run(tc.name, func(t *testing.T) {
			for _, change := range []string{"replace", "clear"} {
				t.Run(change, func(t *testing.T) {
					initialPath := filepath.Join(t.TempDir(), "original", "copilot-runtime")
					t.Setenv("COPILOT_CLI_PATH", initialPath)
					client := NewClient(tc.options)

					laterPath := ""
					if change == "replace" {
						laterPath = filepath.Join(t.TempDir(), "replacement", "copilot-runtime")
					}
					t.Setenv("COPILOT_CLI_PATH", laterPath)
					t.Cleanup(func() { client.ForceStop() })

					// Missing libraries expose the path selected by real startup
					// without loading native threads into this unit-test process.
					err := client.Start(t.Context())
					if err == nil || !strings.Contains(err.Error(), strconv.Quote(initialPath)) {
						t.Fatalf("Expected startup to resolve the original runtime %q, got %v", initialPath, err)
					}
				})
			}
		})
	}
}
