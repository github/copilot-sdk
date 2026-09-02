package testcli

import (
	"os"
	"path/filepath"
	"strings"
	"testing"
)

func TestParseTestRuntimeMetadata(t *testing.T) {
	metadata, err := parseTestRuntimeMetadata(strings.NewReader(`{"packages":{"node_modules/@github/copilot-linux-x64":{"version":"1.2.3","integrity":"sha512-test"}}}`), "linux-x64")
	if err != nil {
		t.Fatal(err)
	}
	if metadata.version != "1.2.3" || metadata.integrity != "sha512-test" {
		t.Fatalf("parseTestRuntimeMetadata() = %#v", metadata)
	}
}

func TestInstalledTestRuntimePathRejectsStaleRuntime(t *testing.T) {
	packageDir := t.TempDir()
	platform := "linux-x64"
	if err := os.MkdirAll(filepath.Join(packageDir, "prebuilds", platform), 0755); err != nil {
		t.Fatal(err)
	}
	for path, content := range map[string]string{
		filepath.Join(packageDir, "package.json"):                        `{"version":"1.2.3"}`,
		filepath.Join(packageDir, testCLIBinaryName()):                   "cli",
		filepath.Join(packageDir, "prebuilds", platform, "runtime.node"): "runtime",
		filepath.Join(packageDir, ".integrity"):                          "sha512-current\n",
	} {
		if err := os.WriteFile(path, []byte(content), 0755); err != nil {
			t.Fatal(err)
		}
	}

	if _, ok := installedTestRuntimePath(packageDir, platform, testRuntimeMetadata{version: "1.2.3", integrity: "sha512-current"}); !ok {
		t.Fatal("installedTestRuntimePath() rejected a matching runtime")
	}
	if _, ok := installedTestRuntimePath(packageDir, platform, testRuntimeMetadata{version: "2.0.0", integrity: "sha512-current"}); ok {
		t.Fatal("installedTestRuntimePath() accepted a stale version")
	}
	if _, ok := installedTestRuntimePath(packageDir, platform, testRuntimeMetadata{version: "1.2.3", integrity: "sha512-new"}); ok {
		t.Fatal("installedTestRuntimePath() accepted stale integrity")
	}
}
