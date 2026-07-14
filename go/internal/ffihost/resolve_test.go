package ffihost

import (
	"os"
	"path/filepath"
	"strings"
	"testing"
)

func TestVersionedLibraryPathForEntrypoint(t *testing.T) {
	dir := t.TempDir()
	cliName := "copilot_1.2.3"
	if filepath.Ext(NaturalLibraryName()) == ".dll" {
		cliName += ".exe"
	}

	got := versionedLibraryPathForEntrypoint(filepath.Join(dir, cliName))
	libraryName := NaturalLibraryName()
	want := filepath.Join(
		dir,
		strings.TrimSuffix(libraryName, filepath.Ext(libraryName))+"_1.2.3"+filepath.Ext(libraryName),
	)
	if got != want {
		t.Fatalf("versionedLibraryPathForEntrypoint() = %q, want %q", got, want)
	}
}

func TestResolveLibraryPathPrefersMatchingVersion(t *testing.T) {
	dir := t.TempDir()
	cliName := "copilot_1.2.3"
	if filepath.Ext(NaturalLibraryName()) == ".dll" {
		cliName += ".exe"
	}
	cliPath := filepath.Join(dir, cliName)
	versionedPath := versionedLibraryPathForEntrypoint(cliPath)
	flatPath := filepath.Join(dir, NaturalLibraryName())

	for _, path := range []string{cliPath, versionedPath, flatPath} {
		if err := os.WriteFile(path, []byte("test"), 0600); err != nil {
			t.Fatalf("WriteFile(%q): %v", path, err)
		}
	}

	got, err := ResolveLibraryPath(cliPath)
	if err != nil {
		t.Fatalf("ResolveLibraryPath() error: %v", err)
	}
	if got != versionedPath {
		t.Fatalf("ResolveLibraryPath() = %q, want %q", got, versionedPath)
	}
}

func TestVersionedLibraryPathForUnversionedEntrypoint(t *testing.T) {
	if got := versionedLibraryPathForEntrypoint(filepath.Join(t.TempDir(), "copilot")); got != "" {
		t.Fatalf("Expected no versioned library path, got %q", got)
	}
}
