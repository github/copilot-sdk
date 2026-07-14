package embeddedcli

import (
	"bytes"
	"crypto/sha256"
	"os"
	"path/filepath"
	"runtime"
	"strings"
	"testing"
)

func resetGlobals() {
	setupMu.Lock()
	defer setupMu.Unlock()
	config = Config{}
	setupDone = false
	pathInitialized = false
	runtimeLibPath = ""
}

func mustPanic(t *testing.T, fn func()) {
	t.Helper()
	defer func() {
		if r := recover(); r == nil {
			t.Fatalf("expected panic")
		}
	}()
	fn()
}

func binaryNameForOS() string {
	name := "copilot"
	if runtime.GOOS == "windows" {
		name += ".exe"
	}
	return name
}

func TestSetupPanicsOnNilCli(t *testing.T) {
	resetGlobals()
	mustPanic(t, func() { Setup(Config{}) })
}

func TestSetupPanicsOnSecondCall(t *testing.T) {
	resetGlobals()
	hash := sha256.Sum256([]byte("ok"))
	Setup(Config{Cli: bytes.NewReader([]byte("ok")), CliHash: hash[:]})
	hash2 := sha256.Sum256([]byte("ok"))
	mustPanic(t, func() { Setup(Config{Cli: bytes.NewReader([]byte("ok")), CliHash: hash2[:]}) })
	resetGlobals()
}

func TestInstallAtWritesBinaryAndLicense(t *testing.T) {
	resetGlobals()
	tempDir := t.TempDir()
	content := []byte("hello")
	hash := sha256.Sum256(content)
	Setup(Config{
		Cli:     bytes.NewReader(content),
		CliHash: hash[:],
		License: []byte("license"),
		Version: "1.2.3",
		Dir:     tempDir,
	})

	path := Path()

	expectedPath := versionedBinaryPath(tempDir, binaryNameForOS(), "1.2.3")
	if path != expectedPath {
		t.Fatalf("unexpected path: got %q want %q", path, expectedPath)
	}

	got, err := os.ReadFile(path)
	if err != nil {
		t.Fatalf("read binary: %v", err)
	}
	if !bytes.Equal(got, content) {
		t.Fatalf("binary content mismatch")
	}

	licensePath := path + ".license"
	license, err := os.ReadFile(licensePath)
	if err != nil {
		t.Fatalf("read license: %v", err)
	}
	if string(license) != "license" {
		t.Fatalf("license content mismatch")
	}

	gotHash, err := hashFile(path)
	if err != nil {
		t.Fatalf("hash file: %v", err)
	}
	if !bytes.Equal(gotHash, hash[:]) {
		t.Fatalf("hash mismatch")
	}
}

func TestInstallAtExistingBinaryHashMismatch(t *testing.T) {
	resetGlobals()
	tempDir := t.TempDir()
	binaryPath := versionedBinaryPath(tempDir, binaryNameForOS(), "")
	if err := os.MkdirAll(filepath.Dir(binaryPath), 0755); err != nil {
		t.Fatalf("mkdir: %v", err)
	}
	if err := os.WriteFile(binaryPath, []byte("bad"), 0755); err != nil {
		t.Fatalf("write binary: %v", err)
	}

	goodHash := sha256.Sum256([]byte("good"))
	config = Config{
		Cli:     bytes.NewReader([]byte("good")),
		CliHash: goodHash[:],
	}

	_, err := installAt(tempDir)
	if err == nil || !strings.Contains(err.Error(), "hash mismatch") {
		t.Fatalf("expected hash mismatch error, got %v", err)
	}
}

func TestSanitizeVersion(t *testing.T) {
	got := sanitizeVersion("v1.2.3+build/abc")
	want := "v1.2.3_build_abc"
	if got != want {
		t.Fatalf("sanitizeVersion() = %q want %q", got, want)
	}
}

func TestVersionedBinaryPath(t *testing.T) {
	got := versionedBinaryPath("/tmp", "copilot.exe", "1.0.0")
	want := filepath.Join("/tmp", "copilot_1.0.0.exe")
	if got != want {
		t.Fatalf("versionedBinaryPath() = %q want %q", got, want)
	}
}

func TestInstallAtAllowsMultipleRuntimeVersions(t *testing.T) {
	resetGlobals()
	tempDir := t.TempDir()

	installVersion := func(version string, cliContent, runtimeContent []byte) (string, string) {
		t.Helper()
		cliHash := sha256.Sum256(cliContent)
		runtimeHash := sha256.Sum256(runtimeContent)
		config = Config{
			Cli:            bytes.NewReader(cliContent),
			CliHash:        cliHash[:],
			RuntimeLib:     bytes.NewReader(runtimeContent),
			RuntimeLibHash: runtimeHash[:],
			Version:        version,
		}

		cliPath, err := installAt(tempDir)
		if err != nil {
			t.Fatalf("install version %s: %v", version, err)
		}
		return cliPath, runtimeLibPath
	}

	cli1, runtime1 := installVersion("1.0.0", []byte("cli-one"), []byte("runtime-one"))
	cli2, runtime2 := installVersion("2.0.0", []byte("cli-two"), []byte("runtime-two"))

	if cli1 == cli2 {
		t.Fatalf("Expected versioned CLI paths to differ, got %q", cli1)
	}
	if runtime1 == runtime2 {
		t.Fatalf("Expected versioned runtime paths to differ, got %q", runtime1)
	}
	if got, err := os.ReadFile(runtime1); err != nil || string(got) != "runtime-one" {
		t.Fatalf("Unexpected first runtime: content=%q err=%v", got, err)
	}
	if got, err := os.ReadFile(runtime2); err != nil || string(got) != "runtime-two" {
		t.Fatalf("Unexpected second runtime: content=%q err=%v", got, err)
	}
}
