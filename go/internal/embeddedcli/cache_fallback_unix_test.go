//go:build unix

package embeddedcli

import (
	"bytes"
	"crypto/sha256"
	"fmt"
	"os"
	"os/exec"
	"path/filepath"
	"strings"
	"testing"
)

func TestPathRetriesCacheSelectionFailure(t *testing.T) {
	const child = "COPILOT_TEST_CACHE_SELECTION_RETRY"
	if os.Getenv(child) != "1" {
		cmd := exec.CommandContext(t.Context(), os.Args[0], "-test.run=^TestPathRetriesCacheSelectionFailure$")
		cmd.Env = append(os.Environ(), child+"=1")
		if output, err := cmd.CombinedOutput(); err != nil {
			t.Fatalf("cache selection retry: %v\n%s", err, output)
		}
		return
	}

	root := withoutUserCache(t)
	if err := os.WriteFile(root, []byte("not a directory"), 0600); err != nil {
		t.Fatal(err)
	}
	content := []byte("cli")
	hash := sha256.Sum256(content)
	Setup(Config{Cli: bytes.NewReader(content), CliHash: hash[:]})
	if path := Path(); path != "" {
		t.Fatalf("unsafe cache root accepted: %q", path)
	}
	if err := os.Remove(root); err != nil {
		t.Fatal(err)
	}
	want := filepath.Join(root, "copilot-sdk", binaryNameForOS())
	if path := Path(); path != want {
		t.Fatalf("cache selection failure was cached: path=%q want=%q", path, want)
	}
	if got, err := os.ReadFile(want); err != nil || !bytes.Equal(got, content) {
		t.Fatalf("installed CLI: content=%q err=%v", got, err)
	}
	if err := os.Chmod(root, 0755); err != nil {
		t.Fatal(err)
	}
	if path := Path(); path != want {
		t.Fatalf("warm path repeated cache selection: path=%q want=%q", path, want)
	}
}

func TestPrivateTemporaryCacheCreatesAndReusesInTrustedBase(t *testing.T) {
	base := filepath.Join(t.TempDir(), "scratch", "tmp")
	root, err := privateTemporaryCacheDir(base)
	if err != nil {
		t.Fatal(err)
	}
	parent, err := filepath.EvalSymlinks(base)
	if err != nil {
		t.Fatal(err)
	}
	want := filepath.Join(parent, fmt.Sprintf("copilot-sdk-cache-%d", os.Geteuid()))
	if root != want {
		t.Fatalf("private cache root = %q, want %q", root, want)
	}
	info, err := os.Lstat(root)
	if err != nil || !info.IsDir() || info.Mode().Perm() != 0700 {
		t.Fatalf("owner-only cache creation: info=%v err=%v", info, err)
	}
	marker := filepath.Join(root, "retained")
	if err := os.WriteFile(marker, []byte("warm cache"), 0600); err != nil {
		t.Fatal(err)
	}
	// This base remains protected by the fixture's owner-only ancestor.
	if err := os.Chmod(base, 0777); err != nil {
		t.Fatal(err)
	}
	if again, err := privateTemporaryCacheDir(base); err != nil || again != root {
		t.Fatalf("warm cache root=%q err=%v", again, err)
	}
	if content, err := os.ReadFile(marker); err != nil || string(content) != "warm cache" {
		t.Fatalf("warm cache changed: content=%q err=%v", content, err)
	}
}

func TestPrivateTemporaryCacheRejectsUnsafeExistingRoot(t *testing.T) {
	for _, kind := range []string{"readable", "symlink", "file"} {
		t.Run(kind, func(t *testing.T) {
			base := t.TempDir()
			root := filepath.Join(base, fmt.Sprintf("copilot-sdk-cache-%d", os.Geteuid()))
			switch kind {
			case "readable":
				if err := os.Mkdir(root, 0755); err != nil {
					t.Fatal(err)
				}
				if err := os.Chmod(root, 0755); err != nil {
					t.Fatal(err)
				}
			case "symlink":
				if err := os.Symlink(t.TempDir(), root); err != nil {
					t.Fatal(err)
				}
			case "file":
				if err := os.WriteFile(root, []byte("not a cache"), 0600); err != nil {
					t.Fatal(err)
				}
			}
			before, err := os.Lstat(root)
			if err != nil {
				t.Fatal(err)
			}
			if _, err := privateTemporaryCacheDir(base); err == nil || !strings.Contains(err.Error(), "unsafe temporary cache") {
				t.Fatalf("unsafe existing %s must be rejected: %v", kind, err)
			}
			after, err := os.Lstat(root)
			if err != nil || after.Mode() != before.Mode() {
				t.Fatalf("unsafe root was repaired: before=%v after=%v err=%v", before, after, err)
			}
		})
	}
}
