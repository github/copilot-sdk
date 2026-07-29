package ffihost

import (
	"os"
	"path/filepath"
	"testing"
)

func TestResolveLibraryPathUsesNaturalLibraryNextToCLI(t *testing.T) {
	dir := t.TempDir()
	cliPath := filepath.Join(dir, "copilot")
	libraryPath := filepath.Join(dir, NaturalLibraryName())

	for _, path := range []string{cliPath, libraryPath} {
		if err := os.WriteFile(path, []byte("test"), 0600); err != nil {
			t.Fatalf("WriteFile(%q): %v", path, err)
		}
	}

	got, err := ResolveLibraryPath(cliPath)
	if err != nil {
		t.Fatalf("ResolveLibraryPath() error: %v", err)
	}
	if got != libraryPath {
		t.Fatalf("ResolveLibraryPath() = %q, want %q", got, libraryPath)
	}
}

func TestResolveLibraryPathFallsBackToPrebuilds(t *testing.T) {
	folder := PrebuildsFolder()
	if folder == "" {
		t.Skip("unsupported platform")
	}

	dir := t.TempDir()
	cliPath := filepath.Join(dir, "copilot")
	libraryPath := filepath.Join(dir, "prebuilds", folder, "runtime.node")
	if err := os.MkdirAll(filepath.Dir(libraryPath), 0755); err != nil {
		t.Fatalf("MkdirAll(): %v", err)
	}
	for _, path := range []string{cliPath, libraryPath} {
		if err := os.WriteFile(path, []byte("test"), 0600); err != nil {
			t.Fatalf("WriteFile(%q): %v", path, err)
		}
	}

	got, err := ResolveLibraryPath(cliPath)
	if err != nil {
		t.Fatalf("ResolveLibraryPath() error: %v", err)
	}
	if got != libraryPath {
		t.Fatalf("ResolveLibraryPath() = %q, want %q", got, libraryPath)
	}
}
