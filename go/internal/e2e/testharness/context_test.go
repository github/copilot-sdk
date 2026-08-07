package testharness

import (
	"os"
	"path/filepath"
	"reflect"
	"testing"
)

func TestCLIPlatformPackageNames(t *testing.T) {
	tests := []struct {
		name     string
		platform string
		want     []string
	}{
		{
			name:     "macOS",
			platform: "darwin-arm64",
			want:     []string{"copilot-darwin-arm64"},
		},
		{
			name:     "Windows",
			platform: "win32-x64",
			want:     []string{"copilot-win32-x64"},
		},
		{
			name:     "Linux glibc",
			platform: "linux-x64",
			want:     []string{"copilot-linux-x64", "copilot-linuxmusl-x64"},
		},
		{
			name:     "Linux musl",
			platform: "linuxmusl-arm64",
			want:     []string{"copilot-linuxmusl-arm64", "copilot-linux-arm64"},
		},
		{
			name:     "unsupported platform",
			platform: "",
			want:     nil,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := cliPlatformPackageNames(tt.platform); !reflect.DeepEqual(got, tt.want) {
				t.Fatalf("cliPlatformPackageNames(%q) = %v, want %v", tt.platform, got, tt.want)
			}
		})
	}
}

func TestFindCLIInNodeModulesSelectsCurrentPlatform(t *testing.T) {
	githubModules := t.TempDir()
	makeCLIEntrypoint(t, githubModules, "copilot-darwin-arm64")
	makeCLIEntrypoint(t, githubModules, "copilot-language-server")
	want := makeCLIEntrypoint(t, githubModules, "copilot-linux-x64")

	got := findCLIInNodeModules(githubModules, []string{"copilot-linux-x64", "copilot-linuxmusl-x64"})
	if got != want {
		t.Fatalf("findCLIInNodeModules() = %q, want %q", got, want)
	}
}

func TestFindCLIInNodeModulesUsesCandidateOrder(t *testing.T) {
	githubModules := t.TempDir()
	makeCLIEntrypoint(t, githubModules, "copilot-linux-x64")
	want := makeCLIEntrypoint(t, githubModules, "copilot-linuxmusl-x64")

	got := findCLIInNodeModules(githubModules, []string{"copilot-linuxmusl-x64", "copilot-linux-x64"})
	if got != want {
		t.Fatalf("findCLIInNodeModules() = %q, want %q", got, want)
	}
}

func TestFindCLIInNodeModulesReturnsEmptyWithoutCandidate(t *testing.T) {
	githubModules := t.TempDir()
	makeCLIEntrypoint(t, githubModules, "copilot-win32-x64")
	if err := os.Mkdir(filepath.Join(githubModules, "copilot-linux-x64"), 0o755); err != nil {
		t.Fatalf("Mkdir(): %v", err)
	}

	if got := findCLIInNodeModules(githubModules, []string{"copilot-linux-x64"}); got != "" {
		t.Fatalf("findCLIInNodeModules() = %q, want empty path", got)
	}
}

func TestInstalledCLIPackageNames(t *testing.T) {
	githubModules := t.TempDir()
	makeCLIEntrypoint(t, githubModules, "copilot-win32-x64")
	makeCLIEntrypoint(t, githubModules, "copilot-darwin-arm64")
	if err := os.Mkdir(filepath.Join(githubModules, "not-copilot"), 0o755); err != nil {
		t.Fatalf("Mkdir(): %v", err)
	}

	want := []string{"copilot-darwin-arm64", "copilot-win32-x64"}
	if got := installedCLIPackageNames(githubModules); !reflect.DeepEqual(got, want) {
		t.Fatalf("installedCLIPackageNames() = %v, want %v", got, want)
	}
}

func makeCLIEntrypoint(t *testing.T, githubModules, packageName string) string {
	t.Helper()
	packageDir := filepath.Join(githubModules, packageName)
	if err := os.MkdirAll(packageDir, 0o755); err != nil {
		t.Fatalf("MkdirAll(): %v", err)
	}
	entrypoint := filepath.Join(packageDir, "index.js")
	if err := os.WriteFile(entrypoint, []byte("// test CLI entrypoint\n"), 0o600); err != nil {
		t.Fatalf("WriteFile(): %v", err)
	}
	return entrypoint
}
