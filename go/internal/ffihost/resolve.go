package ffihost

import (
	"fmt"
	"os"
	"os/exec"
	"path/filepath"
	"runtime"
	"strings"
	"sync"
)

// NaturalLibraryName is the natural platform shared-library file name for the
// runtime cdylib — the `.node` file renamed to what a Rust cdylib would be
// called on this OS. The library is loaded by absolute path, so the on-disk name
// is ours to choose; this matches the flat name the bundler installs next to the
// CLI binary and the name the other SDKs use.
func NaturalLibraryName() string {
	switch runtime.GOOS {
	case "windows":
		return "copilot_runtime.dll"
	case "darwin":
		return "libcopilot_runtime.dylib"
	default:
		return "libcopilot_runtime.so"
	}
}

// PrebuildsFolder returns the napi-rs `<node-platform>-<arch>` folder name the
// runtime package ships under prebuilds/ (e.g. linux-x64, darwin-arm64,
// win32-x64, including the musl variant on Alpine). Returns "" for unsupported
// platforms.
func PrebuildsFolder() string {
	var platform string
	switch runtime.GOOS {
	case "linux":
		if isMusl() {
			platform = "linuxmusl"
		} else {
			platform = "linux"
		}
	case "darwin":
		platform = "darwin"
	case "windows":
		platform = "win32"
	default:
		return ""
	}

	var arch string
	switch runtime.GOARCH {
	case "amd64":
		arch = "x64"
	case "arm64":
		arch = "arm64"
	default:
		return ""
	}
	return platform + "-" + arch
}

// ResolveLibraryPath resolves the native runtime library next to the given CLI
// entrypoint. It checks, in order:
//
//  1. The natural platform library name next to the CLI (bundled/flat layout).
//  2. prebuilds/<platform>/runtime.node next to the CLI (dev/package layout).
//
// It returns an error when neither exists.
func ResolveLibraryPath(cliEntrypoint string) (string, error) {
	abs, err := filepath.Abs(cliEntrypoint)
	if err != nil {
		abs = cliEntrypoint
	}
	dir := filepath.Dir(abs)

	flat := filepath.Join(dir, NaturalLibraryName())
	if fileExists(flat) {
		return flat, nil
	}

	if folder := PrebuildsFolder(); folder != "" {
		prebuilt := filepath.Join(dir, "prebuilds", folder, "runtime.node")
		if fileExists(prebuilt) {
			return prebuilt, nil
		}
	}

	return "", fmt.Errorf(
		"in-process FFI runtime library not found next to %q (looked for %q and prebuilds/%s/runtime.node); "+
			"use a runtime package that ships the native library",
		abs, NaturalLibraryName(), PrebuildsFolder())
}

func fileExists(path string) bool {
	info, err := os.Stat(path)
	return err == nil && !info.IsDir()
}

var (
	muslOnce   sync.Once
	muslResult bool
)

// isMusl reports whether the current Linux system uses musl libc (e.g. Alpine),
// which ships the runtime under the linuxmusl-<arch> prebuilds folder.
func isMusl() bool {
	muslOnce.Do(func() {
		if runtime.GOOS != "linux" {
			return
		}
		// `ldd --version` prints "musl libc" on musl systems and errors/glibc text
		// elsewhere; a best-effort check is enough to pick the prebuilds folder.
		out, _ := exec.Command("ldd", "--version").CombinedOutput()
		muslResult = strings.Contains(strings.ToLower(string(out)), "musl")
	})
	return muslResult
}
