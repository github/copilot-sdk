package embeddedcli

import (
	"bytes"
	"crypto/sha256"
	"fmt"
	"io"
	"os"
	"os/exec"
	"path/filepath"
	"runtime"
	"strings"
	"sync"
	"time"

	"github.com/github/copilot-sdk/go/internal/flock"
)

// Config defines the inputs used to install and locate the embedded Copilot CLI.
//
// Cli and CliHash are required. If Dir is empty, the CLI is installed into the
// system cache directory. When Version is set, the CLI is installed into a
// version-specific child directory so multiple versions can coexist. License,
// when provided, is written next to the installed binary.
//
// RuntimeLib and RuntimeLibHash are optional: when set, the native in-process
// runtime library (cdylib) is installed next to the CLI binary so the in-process
// (FFI) transport can load it. They are omitted for CLI packages that do not
// ship the native runtime.
type Config struct {
	Cli     io.Reader
	CliHash []byte

	License []byte

	RuntimeLib     io.Reader
	RuntimeLibHash []byte

	// LinuxMuslCli and LinuxMuslRuntimeLib are optional alternatives selected
	// automatically when the application runs on a musl-based Linux system.
	LinuxMuslCli            io.Reader
	LinuxMuslCliHash        []byte
	LinuxMuslRuntimeLib     io.Reader
	LinuxMuslRuntimeLibHash []byte

	Dir     string
	Version string
}

func Setup(cfg Config) {
	if cfg.Cli == nil {
		panic("Cli reader is required")
	}
	if len(cfg.CliHash) != sha256.Size {
		panic(fmt.Sprintf("CliHash must be a SHA-256 hash (%d bytes), got %d bytes", sha256.Size, len(cfg.CliHash)))
	}
	if cfg.LinuxMuslCli != nil && len(cfg.LinuxMuslCliHash) != sha256.Size {
		panic(fmt.Sprintf("LinuxMuslCliHash must be a SHA-256 hash (%d bytes), got %d bytes", sha256.Size, len(cfg.LinuxMuslCliHash)))
	}
	if cfg.LinuxMuslRuntimeLib != nil && len(cfg.LinuxMuslRuntimeLibHash) != sha256.Size {
		panic(fmt.Sprintf("LinuxMuslRuntimeLibHash must be a SHA-256 hash (%d bytes), got %d bytes", sha256.Size, len(cfg.LinuxMuslRuntimeLibHash)))
	}
	setupMu.Lock()
	defer setupMu.Unlock()
	if setupDone {
		panic("Setup must only be called once")
	}
	if pathInitialized {
		panic("Setup must be called before Path is accessed")
	}
	config = cfg
	setupDone = true
}

var Path = sync.OnceValue(func() string {
	setupMu.Lock()
	defer setupMu.Unlock()
	if !setupDone {
		return ""
	}
	pathInitialized = true
	path := install()
	return path
})

// RuntimeLibPath returns the on-disk path to the installed native in-process
// runtime library (cdylib), or "" when no runtime library was bundled or the
// CLI could not be installed. It ensures the embedded CLI is installed first.
func RuntimeLibPath() string {
	Path()
	setupMu.Lock()
	defer setupMu.Unlock()
	return runtimeLibPath
}

var (
	config          Config
	setupMu         sync.Mutex
	setupDone       bool
	pathInitialized bool
	runtimeLibPath  string
	linuxMuslBundle bool
)

func install() (path string) {
	selectLinuxMuslBundle()

	verbose := os.Getenv("COPILOT_CLI_INSTALL_VERBOSE") == "1"
	logError := func(msg string, err error) {
		if verbose {
			fmt.Printf("embedded CLI installation error: %s: %v\n", msg, err)
		}
	}
	if verbose {
		start := time.Now()
		defer func() {
			duration := time.Since(start)
			fmt.Printf("installing embedded CLI at %s installation took %s\n", path, duration)
		}()
	}
	installDir := config.Dir
	if installDir == "" {
		if copilotHome := os.Getenv("COPILOT_HOME"); copilotHome != "" {
			installDir = filepath.Join(copilotHome, "cache", "copilot-sdk")
		} else {
			var err error
			if installDir, err = os.UserCacheDir(); err != nil {
				// Fall back to temp dir if UserCacheDir is unavailable
				installDir = os.TempDir()
			}
			installDir = filepath.Join(installDir, "copilot-sdk")
		}
	}
	path, err := installAt(installDir)
	if err != nil {
		logError("installing in configured directory", err)
		return ""
	}
	return path
}

func selectLinuxMuslBundle() {
	if runtime.GOOS != "linux" || config.LinuxMuslCli == nil || !isMusl() {
		return
	}
	config = linuxMuslConfig(config)
	linuxMuslBundle = true
}

func linuxMuslConfig(cfg Config) Config {
	cfg.Cli = cfg.LinuxMuslCli
	cfg.CliHash = cfg.LinuxMuslCliHash
	cfg.RuntimeLib = cfg.LinuxMuslRuntimeLib
	cfg.RuntimeLibHash = cfg.LinuxMuslRuntimeLibHash
	return cfg
}

func isMusl() bool {
	out, _ := exec.Command("ldd", "--version").CombinedOutput()
	return strings.Contains(strings.ToLower(string(out)), "musl")
}

func installAt(installDir string) (string, error) {
	version := sanitizeVersion(config.Version)
	if version != "" {
		installDir = filepath.Join(installDir, version)
	}
	if linuxMuslBundle {
		installDir = filepath.Join(installDir, "linuxmusl")
	}
	if err := os.MkdirAll(installDir, 0755); err != nil {
		return "", fmt.Errorf("creating install directory: %w", err)
	}

	// Best effort to prevent concurrent installs.
	if release, _ := flock.Acquire(filepath.Join(installDir, ".copilot-cli.lock")); release != nil {
		defer release()
	}

	binaryName := "copilot"
	if runtime.GOOS == "windows" {
		binaryName += ".exe"
	}
	finalPath := filepath.Join(installDir, binaryName)

	if _, err := os.Stat(finalPath); err == nil {
		existingHash, err := hashFile(finalPath)
		if err != nil {
			return "", fmt.Errorf("hashing existing binary: %w", err)
		}
		if !bytes.Equal(existingHash, config.CliHash) {
			return "", fmt.Errorf("existing binary hash mismatch")
		}
		if config.RuntimeLib != nil {
			libPath, err := installRuntimeLib(installDir)
			if err != nil {
				return "", err
			}
			runtimeLibPath = libPath
		}
		return finalPath, nil
	}

	f, err := os.OpenFile(finalPath, os.O_WRONLY|os.O_CREATE|os.O_TRUNC, 0755)
	if err != nil {
		return "", fmt.Errorf("creating binary file: %w", err)
	}
	_, err = io.Copy(f, config.Cli)
	if err1 := f.Close(); err1 != nil && err == nil {
		err = err1
	}
	if closer, ok := config.Cli.(io.Closer); ok {
		closer.Close()
	}
	if err != nil {
		return "", fmt.Errorf("writing binary file: %w", err)
	}
	if len(config.License) > 0 {
		licensePath := finalPath + ".license"
		if err := os.WriteFile(licensePath, config.License, 0644); err != nil {
			return "", fmt.Errorf("writing license file: %w", err)
		}
	}

	// Install the native in-process runtime library (if bundled) next to the CLI.
	// Fail closed on any hash mismatch; never place unverified native code.
	if config.RuntimeLib != nil {
		libPath, err := installRuntimeLib(installDir)
		if err != nil {
			return "", err
		}
		runtimeLibPath = libPath
	}

	return finalPath, nil
}

// installRuntimeLib writes the embedded runtime cdylib into installDir under its
// natural platform file name, verifying its SHA-256. It is idempotent: an
// existing file with a matching hash is reused; a mismatch is a hard error.
func installRuntimeLib(installDir string) (string, error) {
	if len(config.RuntimeLibHash) != sha256.Size {
		return "", fmt.Errorf("RuntimeLibHash must be a SHA-256 hash (%d bytes), got %d bytes", sha256.Size, len(config.RuntimeLibHash))
	}
	libPath := filepath.Join(installDir, naturalRuntimeLibName())

	if _, err := os.Stat(libPath); err == nil {
		existingHash, err := hashFile(libPath)
		if err != nil {
			return "", fmt.Errorf("hashing existing runtime library: %w", err)
		}
		if !bytes.Equal(existingHash, config.RuntimeLibHash) {
			return "", fmt.Errorf("existing runtime library hash mismatch")
		}
		return libPath, nil
	}

	// Write to a temp file in the same directory, verify, then atomically rename.
	tmp, err := os.CreateTemp(installDir, ".copilot-runtime-*.tmp")
	if err != nil {
		return "", fmt.Errorf("creating temp runtime library: %w", err)
	}
	tmpPath := tmp.Name()
	h := sha256.New()
	_, err = io.Copy(io.MultiWriter(tmp, h), config.RuntimeLib)
	if err1 := tmp.Close(); err1 != nil && err == nil {
		err = err1
	}
	if closer, ok := config.RuntimeLib.(io.Closer); ok {
		closer.Close()
	}
	if err != nil {
		os.Remove(tmpPath)
		return "", fmt.Errorf("writing runtime library: %w", err)
	}
	if !bytes.Equal(h.Sum(nil), config.RuntimeLibHash) {
		os.Remove(tmpPath)
		return "", fmt.Errorf("runtime library hash mismatch")
	}
	if err := os.Rename(tmpPath, libPath); err != nil {
		os.Remove(tmpPath)
		return "", fmt.Errorf("installing runtime library: %w", err)
	}
	return libPath, nil
}

// naturalRuntimeLibName is the flat platform file name for the runtime cdylib,
// matching ffihost.NaturalLibraryName (kept in sync; embeddedcli stays
// dependency-free for use by generated embed files).
func naturalRuntimeLibName() string {
	switch runtime.GOOS {
	case "windows":
		return "copilot_runtime.dll"
	case "darwin":
		return "libcopilot_runtime.dylib"
	default:
		return "libcopilot_runtime.so"
	}
}

// sanitizeVersion makes a version string safe for filenames.
func sanitizeVersion(version string) string {
	if version == "" {
		return ""
	}
	var b strings.Builder
	for _, r := range version {
		switch {
		case r >= 'a' && r <= 'z':
			b.WriteRune(r)
		case r >= 'A' && r <= 'Z':
			b.WriteRune(r)
		case r >= '0' && r <= '9':
			b.WriteRune(r)
		case r == '.' || r == '-' || r == '_':
			b.WriteRune(r)
		default:
			b.WriteRune('_')
		}
	}
	sanitized := b.String()
	if sanitized == "." || sanitized == ".." {
		return strings.Repeat("_", len(sanitized))
	}
	return sanitized
}

// hashFile returns the SHA-256 hash of a file on disk.
func hashFile(path string) ([]byte, error) {
	file, err := os.Open(path)
	if err != nil {
		return nil, err
	}
	defer file.Close()
	h := sha256.New()
	if _, err := io.Copy(h, file); err != nil {
		return nil, err
	}
	return h.Sum(nil), nil
}
