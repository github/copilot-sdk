package testcli_test

import (
	"os"
	"path/filepath"
	"runtime"
	"strings"
	"testing"

	"github.com/github/copilot-sdk/go/testcli"
)

func TestSetupHonorsConfiguredPath(t *testing.T) {
	cliPath := filepath.Join(t.TempDir(), "copilot")
	if err := os.WriteFile(cliPath, []byte("test"), 0755); err != nil {
		t.Fatal(err)
	}
	t.Setenv("COPILOT_CLI_PATH", cliPath)

	// A valid override keeps setup local; success is a nil error and an unchanged path.
	if err := testcli.Setup(); err != nil {
		t.Fatal(err)
	}
	if got := os.Getenv("COPILOT_CLI_PATH"); got != cliPath {
		t.Fatalf("COPILOT_CLI_PATH = %q, want %q", got, cliPath)
	}
}

func TestSetupHonorsJavaScriptConfiguredPath(t *testing.T) {
	cliPath := filepath.Join(t.TempDir(), "copilot.js")
	if err := os.WriteFile(cliPath, []byte("test"), 0644); err != nil {
		t.Fatal(err)
	}
	t.Setenv("COPILOT_CLI_PATH", cliPath)

	if err := testcli.Setup(); err != nil {
		t.Fatal(err)
	}
}

func TestSetupRejectsNonExecutableNativePath(t *testing.T) {
	if runtime.GOOS == "windows" {
		t.Skip("Windows does not use Unix execute permission bits")
	}
	cliPath := filepath.Join(t.TempDir(), "copilot")
	if err := os.WriteFile(cliPath, []byte("test"), 0644); err != nil {
		t.Fatal(err)
	}
	t.Setenv("COPILOT_CLI_PATH", cliPath)

	requireErrorContaining(t, testcli.Setup(), "COPILOT_CLI_PATH", "is not executable")
}

func TestSetupRejectsInvalidConfiguredPath(t *testing.T) {
	tests := map[string]string{
		"missing file": filepath.Join(t.TempDir(), "missing-copilot"),
		"directory":    t.TempDir(),
	}
	for name, cliPath := range tests {
		t.Run(name, func(t *testing.T) {
			t.Setenv("COPILOT_CLI_PATH", cliPath)

			requireErrorContaining(t, testcli.Setup(), "COPILOT_CLI_PATH", "is not a file")
		})
	}
}

func requireErrorContaining(t *testing.T, err error, fragments ...string) {
	t.Helper()
	if err == nil {
		t.Fatal("testcli.Setup() returned nil")
	}
	for _, fragment := range fragments {
		if !strings.Contains(err.Error(), fragment) {
			t.Fatalf("error = %q, want it to contain %q", err, fragment)
		}
	}
}
