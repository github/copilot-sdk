//go:build unix

package embeddedcli

import (
	"fmt"
	"os"
	"path/filepath"
	"syscall"
)

func fallbackCacheDir() (string, error) {
	return privateTemporaryCacheDir(os.TempDir())
}

func privateTemporaryCacheDir(tempDir string) (string, error) {
	base, err := filepath.Abs(tempDir)
	if err != nil {
		return "", fmt.Errorf("resolving temporary cache parent: %w", err)
	}
	if err := os.MkdirAll(base, 0755); err != nil {
		return "", fmt.Errorf("creating temporary cache parent: %w", err)
	}
	base, err = filepath.EvalSymlinks(base)
	if err != nil {
		return "", fmt.Errorf("resolving temporary cache parent: %w", err)
	}
	// Like explicit cache paths, the OS/application-selected temp base is trusted.
	uid := uint32(os.Geteuid())
	root := filepath.Join(base, fmt.Sprintf("copilot-sdk-cache-%d", uid))
	if err := os.Mkdir(root, 0700); err != nil && !os.IsExist(err) {
		return "", fmt.Errorf("creating private temporary cache: %w", err)
	}
	info, err := os.Lstat(root)
	if err != nil {
		return "", fmt.Errorf("inspecting temporary cache: %w", err)
	}
	stat, ok := info.Sys().(*syscall.Stat_t)
	if !ok {
		return "", fmt.Errorf("cannot inspect ownership of %q", root)
	}
	if !privateTemporaryCache(stat.Uid, info.Mode(), uid) {
		return "", fmt.Errorf("unsafe temporary cache %q (owner UID %d, mode %v); expected an owner-only directory for UID %d", root, stat.Uid, info.Mode(), uid)
	}
	return root, nil
}
