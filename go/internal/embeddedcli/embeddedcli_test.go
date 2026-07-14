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
	linuxMuslBundle = false
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

func TestLinuxMuslConfigSelectsAlternativeArtifacts(t *testing.T) {
	glibcCLI := strings.NewReader("glibc-cli")
	glibcRuntime := strings.NewReader("glibc-runtime")
	muslCLI := strings.NewReader("musl-cli")
	muslRuntime := strings.NewReader("musl-runtime")
	muslCLIHash := bytes.Repeat([]byte{1}, sha256.Size)
	muslRuntimeHash := bytes.Repeat([]byte{2}, sha256.Size)

	selected := linuxMuslConfig(Config{
		Cli:                     glibcCLI,
		RuntimeLib:              glibcRuntime,
		LinuxMuslCli:            muslCLI,
		LinuxMuslCliHash:        muslCLIHash,
		LinuxMuslRuntimeLib:     muslRuntime,
		LinuxMuslRuntimeLibHash: muslRuntimeHash,
	})

	if selected.Cli != muslCLI || selected.RuntimeLib != muslRuntime {
		t.Fatal("Expected Linux musl artifacts to replace the glibc artifacts")
	}
	if !bytes.Equal(selected.CliHash, muslCLIHash) || !bytes.Equal(selected.RuntimeLibHash, muslRuntimeHash) {
		t.Fatal("Expected Linux musl hashes to replace the glibc hashes")
	}
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

	expectedPath := filepath.Join(tempDir, "1.2.3", binaryNameForOS())
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
	binaryPath := filepath.Join(tempDir, binaryNameForOS())
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
	tests := map[string]string{
		"v1.2.3+build/abc": "v1.2.3_build_abc",
		".":                "_",
		"..":               "__",
	}
	for input, want := range tests {
		if got := sanitizeVersion(input); got != want {
			t.Errorf("sanitizeVersion(%q) = %q want %q", input, got, want)
		}
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
	if got, want := filepath.Base(cli1), binaryNameForOS(); got != want {
		t.Fatalf("First CLI filename = %q, want %q", got, want)
	}
	if got, want := filepath.Base(runtime1), naturalRuntimeLibName(); got != want {
		t.Fatalf("First runtime filename = %q, want %q", got, want)
	}
	if got, want := filepath.Base(filepath.Dir(cli1)), "1.0.0"; got != want {
		t.Fatalf("First CLI version directory = %q, want %q", got, want)
	}
	if filepath.Dir(cli1) != filepath.Dir(runtime1) {
		t.Fatalf("CLI and runtime were installed in different directories: %q and %q", cli1, runtime1)
	}
	if got, err := os.ReadFile(runtime1); err != nil || string(got) != "runtime-one" {
		t.Fatalf("Unexpected first runtime: content=%q err=%v", got, err)
	}
	if got, err := os.ReadFile(runtime2); err != nil || string(got) != "runtime-two" {
		t.Fatalf("Unexpected second runtime: content=%q err=%v", got, err)
	}
}

func TestInstallAtExistingBinaryInstallsMissingRuntime(t *testing.T) {
	resetGlobals()
	tempDir := t.TempDir()
	versionDir := filepath.Join(tempDir, "1.2.3")
	if err := os.MkdirAll(versionDir, 0755); err != nil {
		t.Fatalf("mkdir: %v", err)
	}

	cliContent := []byte("cli")
	cliPath := filepath.Join(versionDir, binaryNameForOS())
	if err := os.WriteFile(cliPath, cliContent, 0755); err != nil {
		t.Fatalf("write CLI: %v", err)
	}
	cliHash := sha256.Sum256(cliContent)
	runtimeContent := []byte("runtime")
	runtimeHash := sha256.Sum256(runtimeContent)
	config = Config{
		Cli:            bytes.NewReader(cliContent),
		CliHash:        cliHash[:],
		RuntimeLib:     bytes.NewReader(runtimeContent),
		RuntimeLibHash: runtimeHash[:],
		Version:        "1.2.3",
	}

	gotCLIPath, err := installAt(tempDir)
	if err != nil {
		t.Fatalf("installAt(): %v", err)
	}
	if gotCLIPath != cliPath {
		t.Fatalf("installAt() = %q, want %q", gotCLIPath, cliPath)
	}
	if got, err := os.ReadFile(filepath.Join(versionDir, naturalRuntimeLibName())); err != nil || string(got) != "runtime" {
		t.Fatalf("Unexpected runtime: content=%q err=%v", got, err)
	}
}
